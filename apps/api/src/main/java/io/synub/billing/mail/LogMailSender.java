package io.synub.billing.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 개발용 메일 발신 — 실제 SMTP 없이 로그로 출력. app.mail.smtp-enabled=false(기본)일 때. */
@Component
@ConditionalOnProperty(name = "app.mail.smtp-enabled", havingValue = "false", matchIfMissing = true)
public class LogMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LogMailSender.class);

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("[MAIL:dev] to={} | subject={}\n{}", to, subject, htmlBody);
    }
}
