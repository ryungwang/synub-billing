package io.synub.billing.service;

import io.synub.billing.domain.Customer;
import io.synub.billing.domain.Membership;
import io.synub.billing.domain.Organization;
import io.synub.billing.dto.Dtos.OrgMemberDto;
import io.synub.billing.dto.Dtos.ProvisionMemberRequest;
import io.synub.billing.repo.CustomerRepository;
import io.synub.billing.repo.MembershipRepository;
import io.synub.billing.repo.OrganizationRepository;
import io.synub.billing.web.ApiExceptions.BadRequestException;
import io.synub.billing.web.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제품(그룹웨어 등)이 조직 직원을 빌링 조직 멤버로 등록 — org_code로 조직을 찾아
 * 통합계정(external_id)을 그 조직의 멤버로 연결. 그래야 그 직원이 빌링에서 회사 컨텍스트로 보이고
 * 조직 구독의 이용 권한(entitlement)이 성립한다. 서비스 키 인증(사용자 토큰 아님).
 */
@Service
public class OrgMemberProvisioningService {

    private final OrganizationRepository organizations;
    private final CustomerRepository customers;
    private final MembershipRepository memberships;

    public OrgMemberProvisioningService(OrganizationRepository organizations,
                                        CustomerRepository customers, MembershipRepository memberships) {
        this.organizations = organizations;
        this.customers = customers;
        this.memberships = memberships;
    }

    @Transactional
    public OrgMemberDto addMember(ProvisionMemberRequest req) {
        Organization org = organizations.findByOrgCode(req.orgCode().trim())
                .orElseThrow(() -> new NotFoundException("조직코드를 찾을 수 없습니다: " + req.orgCode()));
        if (!org.isVerified()) {
            throw new BadRequestException("인증 완료된 조직만 멤버를 추가할 수 있습니다.");
        }
        String role = normalizeRole(req.role());

        // 통합계정 → 빌링 고객 JIT 매핑(테넌트=조직의 회사)
        Customer customer = customers
                .findByCompanyIdAndExternalId(org.getCompanyId(), req.externalId())
                .orElseGet(() -> customers.save(
                        new Customer(org.getCompanyId(), req.externalId(), req.email())));

        // 멱등: 이미 멤버면 유지(다운그레이드 방지), 아니면 추가
        var existing = memberships.findByOrganizationIdAndCustomerId(org.getId(), customer.getId());
        if (existing.isPresent()) {
            return new OrgMemberDto(org.getOrgCode(), req.externalId(), existing.get().getRole(), false);
        }
        memberships.save(new Membership(org.getId(), customer.getId(), role));
        return new OrgMemberDto(org.getOrgCode(), req.externalId(), role, true);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return Membership.MEMBER;
        String r = role.trim();
        if (Membership.MEMBER.equals(r) || Membership.BILLING_MANAGER.equals(r)) return r;
        // owner 등은 이 경로로 부여 금지(직원 프로비저닝은 member/billing_manager만)
        throw new BadRequestException("허용되지 않는 역할입니다: " + role);
    }
}
