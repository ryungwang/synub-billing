package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.dto.Dtos.MemberDto;
import io.synub.billing.dto.Dtos.OrgDto;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.storage.StorageService;
import io.synub.billing.tenant.CurrentTenant;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.ForbiddenException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 조직 생성·조회 + 멤버십·역할 + 사업자 인증. 조직/역할/인증은 빌링이 주관(결제 권한과 직결). */
@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final CustomerRepository customers;
    private final BusinessVerifier businessVerifier;
    private final IdentityVerifier identityVerifier;
    private final StorageService storage;
    private final CurrentUser currentUser;
    private final CurrentTenant tenant;

    public OrganizationService(OrganizationRepository organizations, MembershipRepository memberships,
                               CustomerRepository customers, BusinessVerifier businessVerifier,
                               IdentityVerifier identityVerifier, StorageService storage,
                               CurrentUser currentUser, CurrentTenant tenant) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.customers = customers;
        this.businessVerifier = businessVerifier;
        this.identityVerifier = identityVerifier;
        this.storage = storage;
        this.currentUser = currentUser;
        this.tenant = tenant;
    }

    /**
     * 조직 생성 + 사업자 인증 제출. 생성자는 owner.
     * 3중 검증: 사업자번호 형식·체크섬·국세청 상태(실존) + 중복 금지 + 사업자등록증 서류 제출.
     * 소유권은 관리자 심사(approve)로 확정 — 생성 시점엔 pending, 인증 전 결제 불가.
     */
    @Transactional
    public OrgDto create(String name, String businessNo, String repName, String openDate,
                         String identityVerificationId, byte[] docBytes, String docFilename) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new BadRequestException("회사 이름을 입력하세요.");
        if (trimmed.length() > 80) throw new BadRequestException("회사 이름은 80자 이하여야 합니다.");

        String bizNo = businessVerifier.normalize(businessNo);
        if (!businessVerifier.isValidFormat(bizNo)) {
            throw new BadRequestException("유효하지 않은 사업자등록번호입니다.");
        }
        if (organizations.existsByBusinessNo(bizNo)) {
            throw new BadRequestException("이미 등록된 사업자등록번호입니다.");
        }
        String rep = repName == null ? "" : repName.trim();
        if (rep.isEmpty()) throw new BadRequestException("대표자명을 입력하세요.");
        String openDt = openDate == null ? "" : openDate.replaceAll("[^0-9]", "");

        // 국세청 진위확인: 번호+대표자+개업일 일치 (apiKey 있을 때). 실패 시 등록 거부.
        if (!businessVerifier.verifyAuthenticity(bizNo, openDt, rep)) {
            throw new BadRequestException("국세청 진위확인에 실패했습니다. 사업자번호·대표자명·개업일자를 확인하세요.");
        }
        if (!businessVerifier.isActiveBusiness(bizNo)) {
            throw new BadRequestException("휴업·폐업 상태이거나 확인되지 않는 사업자입니다.");
        }

        // 대표자 본인인증(있으면): 인증 실명이 대표자명과 일치해야 함. 불일치 시 거부(도용 방지).
        Optional<String> verifiedName = identityVerifier.verifiedName(identityVerificationId);
        if (verifiedName.isPresent()
                && !squash(verifiedName.get()).equals(squash(rep))) {
            throw new BadRequestException("본인인증 이름과 대표자명이 일치하지 않습니다.");
        }

        if (docBytes == null || docBytes.length == 0) {
            throw new BadRequestException("사업자등록증을 첨부하세요.");
        }

        Customer me = currentUser.resolve();
        String docKey = storage.store(docBytes, docFilename);
        Organization org = new Organization(tenant.companyId(), trimmed);
        org.submitBusiness(bizNo, rep, openDt, docKey);
        // 대표자 본인인증 통과 → 소유권 자동 확정(즉시 인증완료). 없으면 pending(관리자 서류 심사).
        if (verifiedName.isPresent()) {
            org.confirmByRepresentative(java.time.Instant.now());
        }
        organizations.save(org);
        memberships.save(new Membership(org.getId(), me.getId(), Membership.OWNER));
        return toOrgDto(org, Membership.OWNER);
    }

    private String squash(String s) {
        return s == null ? "" : s.replaceAll("\\s", "");
    }

    /** 내가 속한 조직 목록(+내 역할·인증상태). */
    @Transactional(readOnly = true)
    public List<OrgDto> myOrganizations() {
        Customer me = currentUser.resolve();
        return memberships.findByCustomerId(me.getId()).stream()
                .map(m -> toOrgDto(org(m.getOrganizationId()), m.getRole()))
                .toList();
    }

    private OrgDto toOrgDto(Organization org, String role) {
        return new OrgDto(org.getId(), org.getName(), role, org.getVerifyStatus());
    }

    /** 특정 고객의 조직 내 멤버십(없으면 empty). */
    @Transactional(readOnly = true)
    public Optional<Membership> membership(Long organizationId, Long customerId) {
        return memberships.findByOrganizationIdAndCustomerId(organizationId, customerId);
    }

    /** 조직 멤버 목록. 멤버만 조회 가능. */
    @Transactional(readOnly = true)
    public List<MemberDto> members(Long organizationId) {
        requireMember(organizationId);
        return memberships.findByOrganizationId(organizationId).stream()
                .map(m -> {
                    Customer c = customers.findById(m.getCustomerId())
                            .orElseThrow(() -> new NotFoundException("고객을 찾을 수 없습니다."));
                    return new MemberDto(c.getId(), c.getExternalId(), c.getEmail(), m.getRole());
                })
                .toList();
    }

    /** 멤버 역할 변경(owner 만). 마지막 owner 를 강등할 수 없다. */
    @Transactional
    public void changeRole(Long organizationId, Long customerId, String role) {
        requireOwner(organizationId);
        validateAssignableRole(role);
        Membership target = memberships.findByOrganizationIdAndCustomerId(organizationId, customerId)
                .orElseThrow(() -> new NotFoundException("멤버를 찾을 수 없습니다."));
        if (Membership.OWNER.equals(target.getRole()) && !Membership.OWNER.equals(role)
                && ownerCount(organizationId) <= 1) {
            throw new BadRequestException("마지막 소유자의 역할은 변경할 수 없습니다.");
        }
        target.setRole(role);
    }

    /** 멤버 제거(owner 만). 마지막 owner 는 제거할 수 없다. */
    @Transactional
    public void removeMember(Long organizationId, Long customerId) {
        requireOwner(organizationId);
        Membership target = memberships.findByOrganizationIdAndCustomerId(organizationId, customerId)
                .orElseThrow(() -> new NotFoundException("멤버를 찾을 수 없습니다."));
        if (Membership.OWNER.equals(target.getRole()) && ownerCount(organizationId) <= 1) {
            throw new BadRequestException("마지막 소유자는 제거할 수 없습니다.");
        }
        memberships.delete(target);
    }

    // ---- 역할 검증 헬퍼 (다른 서비스에서도 사용) ----

    public Membership requireMember(Long organizationId) {
        Customer me = currentUser.resolve();
        return memberships.findByOrganizationIdAndCustomerId(organizationId, me.getId())
                .orElseThrow(() -> new ForbiddenException("해당 조직의 멤버가 아닙니다."));
    }

    public Membership requireManager(Long organizationId) {
        Membership m = requireMember(organizationId);
        if (!m.canManageBilling()) {
            throw new ForbiddenException("조직의 소유자·결제 관리자만 할 수 있습니다.");
        }
        return m;
    }

    public Membership requireOwner(Long organizationId) {
        Membership m = requireMember(organizationId);
        if (!Membership.OWNER.equals(m.getRole())) {
            throw new ForbiddenException("조직의 소유자만 할 수 있습니다.");
        }
        return m;
    }

    /** 초대 수락 시 멤버십 생성(이미 있으면 무시). */
    @Transactional
    public void addMember(Long organizationId, Long customerId, String role) {
        if (memberships.findByOrganizationIdAndCustomerId(organizationId, customerId).isEmpty()) {
            memberships.save(new Membership(organizationId, customerId, role));
        }
    }

    public Organization org(Long id) {
        return organizations.findById(id)
                .orElseThrow(() -> new NotFoundException("조직을 찾을 수 없습니다."));
    }

    /** 초대·역할에 지정 가능한 역할(owner 는 초대·지정 대상 아님 — 생성자만 owner). */
    public void validateAssignableRole(String role) {
        if (!Membership.MEMBER.equals(role) && !Membership.BILLING_MANAGER.equals(role)
                && !Membership.OWNER.equals(role)) {
            throw new BadRequestException("알 수 없는 역할입니다: " + role);
        }
    }

    private long ownerCount(Long organizationId) {
        return memberships.findByOrganizationId(organizationId).stream()
                .filter(m -> Membership.OWNER.equals(m.getRole())).count();
    }
}
