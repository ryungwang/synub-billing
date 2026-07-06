package io.synub.billing.mail;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Inquiry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 문의 접수 알림 메일(비동기). 발송 실패가 접수 저장을 막지 않는다. */
@Component
public class InquiryMailer {

    private static final Logger log = LoggerFactory.getLogger(InquiryMailer.class);

    private final MailSender mail;
    private final AppProperties props;

    public InquiryMailer(MailSender mail, AppProperties props) {
        this.mail = mail;
        this.props = props;
    }

    /** 고객센터 수신함으로 접수 알림 발송. reply-to 대신 본문에 회신 이메일을 명시. */
    @Async
    public void notifyReceived(Inquiry inq) {
        String to = props.mail().inquiryTo();
        if (to == null || to.isBlank()) {
            log.warn("문의 알림 수신함(app.mail.inquiry-to) 미설정 — 발송 생략 id={}", inq.getId());
            return;
        }
        try {
            String subject = "[Synub 문의:" + inq.getType() + "] "
                    + (inq.getName() == null || inq.getName().isBlank() ? inq.getEmail() : inq.getName());
            mail.send(to, subject, html(inq));
            log.info("문의 접수 알림 발송 id={} to={}", inq.getId(), to);
        } catch (Exception e) {
            log.warn("문의 접수 알림 발송 실패 id={}: {}", inq.getId(), e.getMessage());
        }
    }

    private String html(Inquiry inq) {
        String product = inq.getProductLabel() == null || inq.getProductLabel().isBlank()
                ? "—" : esc(inq.getProductLabel());
        String attach = inq.getAttachmentFilename() == null ? "없음"
                : esc(inq.getAttachmentFilename()) + " (관리자 콘솔에서 다운로드)";
        return """
            <div style="max-width:520px;margin:0 auto;font-family:-apple-system,'Pretendard',sans-serif;color:#191F28">
              <div style="padding:24px 0;text-align:center">
                <div style="font-size:20px;font-weight:800">Synub Billing 문의 접수</div>
              </div>
              <div style="border:1px solid #E5E8EB;border-radius:16px;padding:24px">
                <table style="width:100%%;font-size:14px;line-height:1.7;color:#4E5968;border-collapse:collapse">
                  <tr><td style="width:88px;color:#8B95A1">유형</td><td style="color:#191F28;font-weight:600">%s</td></tr>
                  <tr><td style="color:#8B95A1">제품</td><td>%s</td></tr>
                  <tr><td style="color:#8B95A1">이름</td><td>%s</td></tr>
                  <tr><td style="color:#8B95A1">회신 이메일</td><td><a href="mailto:%s">%s</a></td></tr>
                  <tr><td style="color:#8B95A1">첨부</td><td>%s</td></tr>
                </table>
                <div style="margin-top:16px;padding-top:16px;border-top:1px solid #F2F4F6;white-space:pre-wrap;font-size:14px;line-height:1.7;color:#191F28">%s</div>
              </div>
            </div>
            """.formatted(esc(inq.getType()), product,
                inq.getName() == null ? "—" : esc(inq.getName()),
                esc(inq.getEmail()), esc(inq.getEmail()), attach, esc(inq.getMessage()));
    }

    /** HTML 인젝션 방지 — 사용자 입력을 본문에 넣기 전 최소 이스케이프. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
