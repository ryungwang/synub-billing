package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Invitation;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.dto.Dtos.InvitationDto;
import io.synub.billing.repo.InvitationRepository;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 조직 멤버 초대 흐름. 이메일로 pending 초대 생성(관리 권한 필요) →
 * 초대받은 사람이 로그인(토큰 email 매칭) 후 수락하면 멤버십 생성.
 */
@Service
public class InvitationService {

    private final InvitationRepository invitations;
    private final OrganizationService organizations;
    private final CurrentUser currentUser;

    public InvitationService(InvitationRepository invitations, OrganizationService organizations,
                             CurrentUser currentUser) {
        this.invitations = invitations;
        this.organizations = organizations;
        this.currentUser = currentUser;
    }

    /** 초대 생성. owner/billing_manager 만. 역할은 member 또는 billing_manager. */
    @Transactional
    public InvitationDto invite(Long organizationId, String email, String role) {
        Membership manager = organizations.requireManager(organizationId);
        if (Membership.OWNER.equals(role)) {
            throw new BadRequestException("owner 역할로는 초대할 수 없습니다.");
        }
        organizations.validateAssignableRole(role);
        String normalized = email.trim().toLowerCase();
        invitations.findByOrganizationIdAndEmailAndStatus(organizationId, normalized, Invitation.PENDING)
                .ifPresent(x -> { throw new BadRequestException("이미 초대한 이메일입니다."); });

        Invitation inv = invitations.save(
                new Invitation(organizationId, normalized, role, currentUser.resolve().getId()));
        return toDto(inv, null);
    }

    /** 조직의 pending 초대 목록. 관리자만. */
    @Transactional(readOnly = true)
    public List<InvitationDto> organizationInvitations(Long organizationId) {
        organizations.requireManager(organizationId);
        return invitations.findByOrganizationIdAndStatus(organizationId, Invitation.PENDING).stream()
                .map(i -> toDto(i, null))
                .toList();
    }

    /** 초대 취소. 관리자만. */
    @Transactional
    public void cancel(Long organizationId, Long invitationId) {
        organizations.requireManager(organizationId);
        Invitation inv = load(invitationId);
        if (!inv.getOrganizationId().equals(organizationId)) {
            throw new NotFoundException("초대를 찾을 수 없습니다.");
        }
        if (!inv.isPending()) throw new BadRequestException("이미 처리된 초대입니다.");
        inv.setStatus(Invitation.CANCELED);
    }

    /** 현재 사용자가 받은 pending 초대(토큰 email 매칭). 조직명 포함. */
    @Transactional(readOnly = true)
    public List<InvitationDto> myPending() {
        String email = currentUser.email();
        if (email == null || email.isBlank()) return List.of();
        return invitations.findByEmailAndStatus(email.trim().toLowerCase(), Invitation.PENDING).stream()
                .map(i -> toDto(i, organizations.org(i.getOrganizationId())))
                .toList();
    }

    /** 초대 수락 — 초대 이메일과 현재 로그인 이메일이 일치해야 한다. 멤버십 생성. */
    @Transactional
    public void accept(Long invitationId) {
        Invitation inv = requireMineAndPending(invitationId);
        Customer me = currentUser.resolve();
        organizations.addMember(inv.getOrganizationId(), me.getId(), inv.getRole());
        inv.setStatus(Invitation.ACCEPTED);
    }

    /** 초대 거절. */
    @Transactional
    public void decline(Long invitationId) {
        Invitation inv = requireMineAndPending(invitationId);
        inv.setStatus(Invitation.DECLINED);
    }

    private Invitation requireMineAndPending(Long invitationId) {
        Invitation inv = load(invitationId);
        if (!inv.isPending()) throw new BadRequestException("이미 처리된 초대입니다.");
        String myEmail = currentUser.email();
        if (myEmail == null || !inv.getEmail().equalsIgnoreCase(myEmail.trim())) {
            throw new ForbiddenException("본인에게 온 초대가 아닙니다.");
        }
        return inv;
    }

    private Invitation load(Long id) {
        return invitations.findById(id)
                .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));
    }

    private InvitationDto toDto(Invitation i, Organization org) {
        return new InvitationDto(i.getId(), i.getOrganizationId(),
                org != null ? org.getName() : null, i.getEmail(), i.getRole(), i.getStatus());
    }
}
