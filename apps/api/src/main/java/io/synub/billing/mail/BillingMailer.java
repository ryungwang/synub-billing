package io.synub.billing.mail;

import io.synub.billing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 결제 상태 알림 이메일(실패·정지·정상화). 비동기 — 발송 실패가 청구 로직을 막지 않는다. */
@Component
public class BillingMailer {

    private static final Logger log = LoggerFactory.getLogger(BillingMailer.class);

    private final MailSender mail;
    private final AppProperties props;

    public BillingMailer(MailSender mail, AppProperties props) {
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void paymentFailed(String to, String productName, int amount, LocalDate nextRetry) {
        String body = card(productName, "warning",
                "결제에 실패했어요",
                String.format("%s 정기결제(%s)에 실패했습니다. %s에 자동으로 다시 시도합니다.<br>"
                                + "결제수단을 확인·갱신하시면 즉시 정상화됩니다.",
                        productName, won(amount), nextRetry),
                "결제수단 확인", props.mail().appBaseUrl() + "/payment-methods");
        send(to, "[Synub] " + productName + " 결제 실패 안내", body);
    }

    @Async
    public void suspended(String to, String productName) {
        String body = card(productName, "danger",
                "구독이 정지되었어요",
                String.format("%s 구독이 재시도 후에도 결제되지 않아 정지되었습니다.<br>"
                        + "결제수단을 갱신하고 다시 구독하시면 이용을 재개할 수 있어요.", productName),
                "결제수단 갱신", props.mail().appBaseUrl() + "/payment-methods");
        send(to, "[Synub] " + productName + " 구독 정지 안내", body);
    }

    @Async
    public void recovered(String to, String productName) {
        String body = card(productName, "success",
                "결제가 정상화되었어요",
                String.format("%s 정기결제가 정상적으로 처리되어 구독이 다시 활성화되었습니다.", productName),
                "구독 확인", props.mail().appBaseUrl() + "/subscriptions");
        send(to, "[Synub] " + productName + " 결제 정상화 안내", body);
    }

    private void send(String to, String subject, String body) {
        try {
            mail.send(to, subject, body);
            log.info("결제 알림 이메일 발송 to={} subject={}", to, subject);
        } catch (Exception e) {
            log.warn("결제 알림 이메일 발송 실패 to={}: {}", to, e.getMessage());
        }
    }

    private String won(int amount) {
        return String.format("%,d원", amount);
    }

    private String card(String product, String tone, String title, String message,
                        String ctaLabel, String ctaUrl) {
        String accent = switch (tone) {
            case "danger" -> "#F04452";
            case "success" -> "#15B86B";
            default -> "#FF9500"; // warning
        };
        return """
            <div style="max-width:480px;margin:0 auto;font-family:-apple-system,'Pretendard',sans-serif;color:#191F28">
              <div style="padding:32px 0;text-align:center">
                <div style="font-size:20px;font-weight:800">Synub Billing</div>
              </div>
              <div style="border:1px solid #E5E8EB;border-radius:16px;padding:32px">
                <div style="width:44px;height:4px;border-radius:2px;background:%s;margin-bottom:20px"></div>
                <h1 style="font-size:20px;font-weight:800;margin:0 0 8px">%s</h1>
                <p style="font-size:14px;color:#4E5968;line-height:1.6;margin:0 0 20px">%s</p>
                <a href="%s" style="display:inline-block;background:#3182F6;color:#fff;text-decoration:none;font-weight:700;font-size:15px;padding:12px 20px;border-radius:12px">%s</a>
                <p style="font-size:12px;color:#8B95A1;margin:20px 0 0">Synub 통합 결제 · %s</p>
              </div>
            </div>
            """.formatted(accent, title, message, ctaUrl, ctaLabel, product);
    }
}
