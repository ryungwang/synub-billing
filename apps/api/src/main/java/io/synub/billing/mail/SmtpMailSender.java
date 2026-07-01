package io.synub.billing.mail;

import io.synub.billing.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 운영용 메일 발신 — SMTP(SES 등). app.mail.smtp-enabled=true 일 때. spring.mail.* 설정 필요. */
@Component
@ConditionalOnProperty(name = "app.mail.smtp-enabled", havingValue = "true")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public SmtpMailSender(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(props.mail().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("메일 발송 실패: " + e.getMessage(), e);
        }
    }
}
