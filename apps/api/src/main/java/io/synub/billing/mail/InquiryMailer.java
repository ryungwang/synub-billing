package io.synub.billing.mail;

import io.synub.billing.config.AppProperties;
import io.synub.billing.domain.Inquiry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private String html(Inquiry inq) {
        String appUrl = appUrl();
        String logo = appUrl + "/brand/synub-logo-light.png";
        String product = inq.getProductLabel() == null || inq.getProductLabel().isBlank()
                ? "<span style=\"color:#B0B8C1\">지정 안 함</span>" : esc(inq.getProductLabel());
        String person = inq.getName() == null || inq.getName().isBlank()
                ? esc(inq.getEmail()) : esc(inq.getName());
        String attachRow = inq.getAttachmentFilename() == null ? "" : ("""
                  <tr>
                    <td style="padding:11px 0;color:#8B95A1;vertical-align:top;white-space:nowrap">첨부파일</td>
                    <td style="padding:11px 0 11px 16px;color:#191F28">
                      <span style="display:inline-block;background:#F2F4F6;border-radius:8px;padding:4px 10px;font-size:13px">📎 %s <span style="color:#8B95A1">· %s</span></span>
                    </td>
                  </tr>""".formatted(esc(inq.getAttachmentFilename()), humanSize(inq.getAttachmentSize())));
        String replyUrl = "mailto:" + inq.getEmail() + "?subject="
                + enc("Re: [Synub 문의] " + (inq.getName() == null ? "" : inq.getName() + "님 ") + "문의 답변");
        String stamp = ZonedDateTime.now(KST).format(STAMP);

        return """
            <div style="margin:0;padding:0;background:#F1F3F5;">
              <div style="max-width:560px;margin:0 auto;padding:32px 16px;font-family:-apple-system,BlinkMacSystemFont,'Pretendard','Segoe UI',Roboto,sans-serif;">
                <div style="text-align:left;padding:6px 0 24px 4px;">
                  <img src="%s" alt="Synub" width="168" style="width:168px;height:auto;display:inline-block;border:0;" />
                </div>
                <div style="background:#ffffff;border:1px solid #E8EBED;border-radius:22px;overflow:hidden;box-shadow:0 8px 28px rgba(17,24,39,0.06);">
                  <div style="height:5px;background:#3182F6;"></div>
                  <div style="padding:30px 30px 6px;">
                    <div style="display:inline-block;background:#EAF2FE;color:#3182F6;font-size:12px;font-weight:800;letter-spacing:-0.01em;padding:6px 13px;border-radius:999px;">%s</div>
                    <h1 style="font-size:22px;font-weight:800;color:#191F28;margin:16px 0 6px;letter-spacing:-0.02em;">새 문의가 접수되었어요</h1>
                    <p style="font-size:13px;color:#8B95A1;margin:0;">%s · Synub Billing 문의 폼</p>
                  </div>
                  <div style="padding:10px 30px 2px;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;font-size:14px;line-height:1.5;">
                      <tr>
                        <td style="padding:11px 0;color:#8B95A1;width:84px;vertical-align:top;white-space:nowrap;border-top:1px solid #F2F4F6;">제품</td>
                        <td style="padding:11px 0 11px 16px;color:#191F28;font-weight:600;border-top:1px solid #F2F4F6;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:11px 0;color:#8B95A1;vertical-align:top;white-space:nowrap;border-top:1px solid #F2F4F6;">보낸 분</td>
                        <td style="padding:11px 0 11px 16px;color:#191F28;font-weight:600;border-top:1px solid #F2F4F6;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:11px 0;color:#8B95A1;vertical-align:top;white-space:nowrap;border-top:1px solid #F2F4F6;">회신 이메일</td>
                        <td style="padding:11px 0 11px 16px;border-top:1px solid #F2F4F6;"><a href="mailto:%s" style="color:#3182F6;text-decoration:none;font-weight:600;">%s</a></td>
                      </tr>
                      %s
                    </table>
                  </div>
                  <div style="padding:14px 30px 6px;">
                    <div style="background:#F9FAFB;border:1px solid #F1F3F5;border-radius:16px;padding:18px 20px;font-size:14px;line-height:1.75;color:#333D4B;white-space:pre-wrap;">%s</div>
                  </div>
                  <div style="padding:18px 30px 30px;">
                    <a href="%s" style="display:block;background:#3182F6;color:#ffffff;text-decoration:none;text-align:center;font-weight:700;font-size:15px;padding:15px;border-radius:14px;letter-spacing:-0.01em;">✉️ 답장하기</a>
                    <a href="%s/admin/inquiries" style="display:block;margin-top:10px;text-align:center;color:#8B95A1;text-decoration:none;font-weight:600;font-size:13px;padding:8px;">관리자 콘솔에서 보기 →</a>
                  </div>
                </div>
                <p style="text-align:center;font-size:12px;color:#B0B8C1;margin:22px 0 0;line-height:1.7;">
                  이 메일은 문의 폼(/contact) 접수 시 자동 발송됩니다.<br/>© Synub Inc.
                </p>
              </div>
            </div>
            """.formatted(logo, esc(inq.getType()), stamp, product, person,
                esc(inq.getEmail()), esc(inq.getEmail()), attachRow, esc(inq.getMessage()),
                replyUrl, appUrl);
    }

    /** 이메일 내 링크·로고 기준 URL. 뒤 슬래시 제거. */
    private String appUrl() {
        String base = props.mail().appBaseUrl();
        if (base == null || base.isBlank()) return "https://app.synub.io";
        return base.replaceAll("/+$", "");
    }

    private static String humanSize(Integer bytes) {
        if (bytes == null) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return Math.round(bytes / 1024.0) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** HTML 인젝션 방지 — 사용자 입력을 본문에 넣기 전 최소 이스케이프. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
