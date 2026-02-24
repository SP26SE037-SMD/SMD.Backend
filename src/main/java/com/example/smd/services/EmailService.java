
package com.example.smd.services;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final Gmail gmail;
    private final EmailTemplateService emailTemplateService;

    @Value("${spring.gmail.sender}")
    private  String sender_Email;
    private static final String SENDER_NAME = "SMD System";

    /**
     * Gửi email chào mừng user mới
     */
    public void sendWelcomeEmail(String toEmail, String userName)
            throws MessagingException, IOException {

        String htmlBody = emailTemplateService.buildWelcomeEmail(userName);

        MimeMessage mimeMessage = buildMimeMessage(
                toEmail,
                "Welcome to SMD!",
                htmlBody
        );

        sendMessage(mimeMessage);
        log.info("Welcome email sent to: {}", toEmail);
    }

    /**
     * Gửi test email
     */
    public void sendTestEmail(String toEmail,
                              String recipientName,
                              String message)
            throws MessagingException, IOException {

        String htmlBody =
                emailTemplateService.buildTestEmail(recipientName, message);

        MimeMessage mimeMessage = buildMimeMessage(
                toEmail,
                "Test Email from SMD System",
                htmlBody
        );

        sendMessage(mimeMessage);
        log.info("Test email sent to: {}", toEmail);
    }

    /**
     * Tạo MimeMessage HTML
     */
    private MimeMessage buildMimeMessage(String to,
                                         String subject,
                                         String htmlBody)
            throws MessagingException, UnsupportedEncodingException {

        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(sender_Email, SENDER_NAME));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject, "UTF-8");
        email.setContent(htmlBody, "text/html; charset=UTF-8");

        return email;
    }

    /**
     * Gửi qua Gmail API
     */
    private void sendMessage(MimeMessage email)
            throws MessagingException, IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);

        String encodedEmail =
                Base64.encodeBase64URLSafeString(buffer.toByteArray());

        Message message = new Message();
        message.setRaw(encodedEmail);

        gmail.users().messages().send("me", message).execute();
    }
}