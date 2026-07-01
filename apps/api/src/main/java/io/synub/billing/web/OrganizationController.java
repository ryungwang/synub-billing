package io.synub.billing.web;

import io.synub.billing.dto.Dtos.CreateOrgRequest;
import io.synub.billing.dto.Dtos.OrgDto;
import io.synub.billing.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 조직 생성/조회. 내가 속한 조직과 역할을 관리한다. */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService organizations;

    public OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
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
}
