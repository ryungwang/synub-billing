package io.synub.billing.web;

import io.synub.billing.dto.Dtos.*;
import io.synub.billing.service.InvitationService;
import io.synub.billing.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 조직 생성/조회 + 멤버·초대 관리. */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService organizations;
    private final InvitationService invitations;

    public OrganizationController(OrganizationService organizations, InvitationService invitations) {
        this.organizations = organizations;
        this.invitations = invitations;
    }

    @GetMapping
    public List<OrgDto> myOrganizations() {
        return organizations.myOrganizations();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgDto create(@Valid @RequestBody CreateOrgRequest req) {
        return organizations.create(req.name());
    }

    // ---- 멤버 ----

    @GetMapping("/{orgId}/members")
    public List<MemberDto> members(@PathVariable Long orgId) {
        return organizations.members(orgId);
    }

    @PatchMapping("/{orgId}/members/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(@PathVariable Long orgId, @PathVariable Long customerId,
                           @Valid @RequestBody ChangeRoleRequest req) {
        organizations.changeRole(orgId, customerId, req.role());
    }

    @DeleteMapping("/{orgId}/members/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long orgId, @PathVariable Long customerId) {
        organizations.removeMember(orgId, customerId);
    }

    // ---- 초대 ----

    @GetMapping("/{orgId}/invitations")
    public List<InvitationDto> invitations(@PathVariable Long orgId) {
        return invitations.organizationInvitations(orgId);
    }

    @PostMapping("/{orgId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationDto invite(@PathVariable Long orgId, @Valid @RequestBody CreateInvitationRequest req) {
        return invitations.invite(orgId, req.email(), req.role());
    }

    @DeleteMapping("/{orgId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvitation(@PathVariable Long orgId, @PathVariable Long invitationId) {
        invitations.cancel(orgId, invitationId);
    }
}
