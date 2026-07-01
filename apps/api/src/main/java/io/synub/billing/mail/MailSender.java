package io.synub.billing.mail;

/** 이메일 발신 추상화. 로컬은 로그(LogMailSender), 운영은 SMTP(SmtpMailSender, SES). */
public interface MailSender {
    void send(String to, String subject, String htmlBody);
}
