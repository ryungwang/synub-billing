package io.synub.billing.web;

import io.synub.billing.dto.Dtos.InvitationDto;
import io.synub.billing.service.InvitationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 내가 받은 초대 조회 + 수락/거절. (조직 관리자의 초대 발송은 OrganizationController) */
@RestController
@RequestMapping("/invitations")
public class InvitationController {

    private final InvitationService invitations;

    public InvitationController(InvitationService invitations) {
        this.invitations = invitations;
    }

    @GetMapping
    public List<InvitationDto> myPending() {
        return invitations.myPending();
    }

    @PostMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable Long id) {
        invitations.accept(id);
    }

    @PostMapping("/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable Long id) {
        invitations.decline(id);
    }
}
