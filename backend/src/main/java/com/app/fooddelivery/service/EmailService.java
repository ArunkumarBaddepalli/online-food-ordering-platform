package com.app.fooddelivery.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends the app's emails.
 *
 * Two rules hold everywhere here:
 *
 * Sending is asynchronous and never propagates a failure. An order must be
 * placed even if the mail server is unreachable; telling a customer their
 * payment failed because a receipt could not be sent would be absurd.
 *
 * With no mail server configured the app runs normally and logs what it would
 * have sent, so a fresh clone works without credentials.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:no-reply@fooddelivery.local}")
    private String from;

    @Value("${app.mail.from-name:Food Delivery}")
    private String fromName;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Says on startup whether mail will actually be delivered.
     *
     * Without this the difference between "sending" and "quietly logging" is
     * invisible until somebody notices a receipt never arrived.
     */
    @jakarta.annotation.PostConstruct
    void reportConfiguration() {
        if (isConfigured()) {
            log.info("Email is live: sending through {} as {}", mailHost, from);
        } else {
            log.warn("Email is NOT being sent. Messages will be logged instead. "
                    + "Set MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD in backend/.env.local to send for real. "
                    + "(host={}, username={}, password={})",
                    notBlank(mailHost) ? "set" : "missing",
                    notBlank(mailUsername) ? "set" : "missing",
                    notBlank(mailPassword) ? "set" : "missing");
        }
    }

    /**
     * Configured means genuinely able to send, not merely pointed at a host.
     * A host with no credentials looks set up and then fails on every message,
     * which is worse than plainly saying it is off.
     */
    public boolean isConfigured() {
        return notBlank(mailHost) && notBlank(mailUsername) && notBlank(mailPassword);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public String baseUrl() {
        return baseUrl;
    }

    @Async
    public void send(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            return;
        }

        if (!isConfigured()) {
            log.info("No mail server configured. Would have sent \"{}\" to {}", subject, to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Sent \"{}\" to {}", subject, to);

        } catch (Exception e) {
            // Deliberately swallowed: see the class comment.
            log.error("Could not send \"{}\" to {}", subject, to, e);
        }
    }

    /** One frame for every message, so they look like they come from one place. */
    public String layout(String heading, String body) {
        return """
                <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                            max-width:560px;margin:0 auto;padding:24px;color:#212529">
                  <div style="font-size:20px;font-weight:600;margin-bottom:4px">🍕 Food Delivery</div>
                  <hr style="border:none;border-top:2px solid #198754;margin:12px 0 24px">
                  <h2 style="font-size:18px;margin:0 0 12px">%s</h2>
                  %s
                  <hr style="border:none;border-top:1px solid #e9ecef;margin:28px 0 12px">
                  <p style="font-size:12px;color:#6c757d;margin:0">
                    This message was sent automatically. Please do not reply to it.
                  </p>
                </div>
                """.formatted(heading, body);
    }

    public String button(String url, String label) {
        return """
                <p style="margin:24px 0">
                  <a href="%s" style="background:#198754;color:#fff;text-decoration:none;
                     padding:12px 20px;border-radius:6px;display:inline-block;font-weight:600">%s</a>
                </p>
                <p style="font-size:12px;color:#6c757d">
                  If the button does not work, paste this into your browser:<br>
                  <span style="word-break:break-all">%s</span>
                </p>
                """.formatted(url, label, url);
    }
}
