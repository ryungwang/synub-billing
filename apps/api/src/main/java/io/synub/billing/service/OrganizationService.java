package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.dto.Dtos.OrgDto;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.tenant.CurrentTenant;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 조직 생성·조회 + 멤버십 확인. 조직/역할은 빌링이 주관(결제 권한과 직결). */
@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final CurrentUser currentUser;
    private final CurrentTenant tenant;

    public OrganizationService(OrganizationRepository organizations, MembershipRepository memberships,
                               CurrentUser currentUser, CurrentTenant tenant) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.currentUser = currentUser;
        this.tenant = tenant;
    }

    /** 조직 생성. 생성자는 owner 로 등록된다. */
    @Transactional
    public OrgDto create(String name) {
        Customer me = currentUser.resolve();
        Organization org = organizations.save(new Organization(tenant.companyId(), name.trim()));
        memberships.save(new Membership(org.getId(), me.getId(), Membership.OWNER));
        return new OrgDto(org.getId(), org.getName(), Membership.OWNER);
    }

    /** 내가 속한 조직 목록(+내 역할). */
    @Transactional(readOnly = true)
    public List<OrgDto> myOrganizations() {
        Customer me = currentUser.resolve();
        return memberships.findByCustomerId(me.getId()).stream()
                .map(m -> {
                    Organization o = organizations.findById(m.getOrganizationId())
                            .orElseThrow(() -> new NotFoundException("조직을 찾을 수 없습니다."));
                    return new OrgDto(o.getId(), o.getName(), m.getRole());
                })
                .toList();
    }

    /** 특정 고객의 조직 내 멤버십(없으면 empty). */
    @Transactional(readOnly = true)
    public Optional<Membership> membership(Long organizationId, Long customerId) {
        return memberships.findByOrganizationIdAndCustomerId(organizationId, customerId);
    }
}
