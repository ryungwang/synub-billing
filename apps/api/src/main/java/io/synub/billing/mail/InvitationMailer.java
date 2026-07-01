package io.synub.billing.mail;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Membership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 조직 초대 이메일 작성·발송(비동기). 발송 실패가 초대 생성을 막지 않는다. */
@Component
public class InvitationMailer {

    private static final Logger log = LoggerFactory.getLogger(InvitationMailer.class);

    private final MailSender mail;
    private final AppProperties props;

    public InvitationMailer(MailSender mail, AppProperties props) {
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void sendInvitation(String toEmail, String orgName, String role, String inviterEmail) {
        try {
            String subject = "[Synub] " + orgName + " 팀에 초대되었습니다";
            String body = html(orgName, roleLabel(role), inviterEmail, props.mail().appBaseUrl());
            mail.send(toEmail, subject, body);
            log.info("초대 이메일 발송 to={} org={}", toEmail, orgName);
        } catch (Exception e) {
            log.warn("초대 이메일 발송 실패 to={}: {}", toEmail, e.getMessage());
        }
    }

    private String roleLabel(String role) {
        if (Membership.BILLING_MANAGER.equals(role)) return "결제 관리자";
        if (Membership.OWNER.equals(role)) return "소유자";
        return "멤버";
    }

    private String html(String orgName, String roleLabel, String inviterEmail, String appUrl) {
        return """
            <div style="max-width:480px;margin:0 auto;font-family:-apple-system,'Pretendard',sans-serif;color:#191F28">
              <div style="padding:32px 0;text-align:center">
                <div style="font-size:20px;font-weight:800">Synub Billing</div>
              </div>
              <div style="border:1px solid #E5E8EB;border-radius:16px;padding:32px">
                <h1 style="font-size:20px;font-weight:800;margin:0 0 8px">%s 팀 초대</h1>
                <p style="font-size:14px;color:#4E5968;line-height:1.6;margin:0 0 20px">
                  <b>%s</b>님이 <b>%s</b> 팀에 <b>%s</b> 역할로 초대했습니다.<br>
                  통합계정으로 로그인하면 초대를 수락하고 팀에 합류할 수 있어요.
                </p>
                <a href="%s" style="display:inline-block;background:#3182F6;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:12px 20px;border-radius:12px">
                  로그인하고 초대 수락
                </a>
                <p style="font-size:12px;color:#8B95A1;margin:20px 0 0">
                  본 메일에 짚이는 바가 없다면 무시하셔도 됩니다.
                </p>
              </div>
            </div>
            """.formatted(orgName, inviterEmail, orgName, roleLabel, appUrl);
    }
}
