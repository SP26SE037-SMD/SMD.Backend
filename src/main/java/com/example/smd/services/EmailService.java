package com.example.smd.services;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final Gmail gmail;
    private final EmailTemplateService emailTemplateService;

    @Value("${spring.gmail.sender}")
    private String sender_Email;
    private static final String SENDER_NAME = "SMD System";
    private static final String LOGO_CID = "smd-logo";
    private static final String LOGO_PATH = "static/smd-logo.png";

    public record AccountCreatedEmail(String email, String fullName) {}

    /**
     * DTO gửi email nhắc nhở task quá hạn.
     * Dùng record để truyền tham số rõ ràng, immutable.
     */
    public record TaskOverdueEmail(
            String email,
            String recipientName,
            String taskName,
            String dueDate,
            long overdueDays
    ) {}

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
     * Gửi email thông báo tạo account hàng loạt theo cơ chế chạy song song.
     */
    public void sendAccountCreatedEmailsBatch(List<AccountCreatedEmail> emailList) {
        if (emailList == null || emailList.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (AccountCreatedEmail item : emailList) {
            tasks.add(CompletableFuture.runAsync(() -> {
                try {
                    String displayName = (item.fullName() == null || item.fullName().isBlank())
                            ? item.email()
                            : item.fullName();

                    String loginHint = "Your account has been created successfully. " +
                            "Please use your registered email to log in and update your profile information.";
                    String htmlBody = emailTemplateService.buildAccountCreatedEmail(
                            displayName,
                            item.email(),
                            loginHint
                    );

                    MimeMessage mimeMessage = buildMimeMessageWithAttachment(
                            item.email(),
                            "SMD Account Created",
                            htmlBody
                    );

                    sendMessage(mimeMessage);
                    log.info("Account created email sent to: {}", item.email());
                } catch (Exception ex) {
                    log.error("Failed to send account created email to {}: {}", item.email(), ex.getMessage());
                }
            }));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Gửi email nhắc nhở task quá hạn theo cơ chế song song (parallel batch).
     * Tuyệt đối KHÔNG dùng for-loop tuần tự để gửi từng email một.
     * Mỗi email được gửi trong một CompletableFuture riêng -> chạy đồng thời.
     * allOf().join() đảm bảo tất cả hoàn thành trước khi Job tiếp tục.
     *
     * @param emailList danh sách task quá hạn cần gửi email (đã được lọc sẵn từ DB)
     */
    public void sendTaskOverdueEmailsBatch(List<TaskOverdueEmail> emailList) {
        if (emailList == null || emailList.isEmpty()) {
            log.info("[TaskReminderJob] No overdue emails to send.");
            return;
        }

        log.info("[TaskReminderJob] Sending {} overdue task email(s) in parallel...", emailList.size());

        // Tạo một CompletableFuture cho mỗi email, chạy song song
        List<CompletableFuture<Void>> futures = emailList.stream()
                .map(item -> CompletableFuture.runAsync(() -> {
                    try {
                        String recipientName = (item.recipientName() == null || item.recipientName().isBlank())
                                ? item.email()
                                : item.recipientName();

                        // Render template HTML với thông tin task
                        String htmlBody = emailTemplateService.buildTaskOverdueEmail(
                                recipientName,
                                item.taskName(),
                                item.dueDate(),
                                item.overdueDays()
                        );

                        MimeMessage mimeMessage = buildMimeMessage(
                                item.email(),
                                "[SMD] ⚠️ Task Quá Hạn: " + item.taskName(),
                                htmlBody
                        );

                        sendMessage(mimeMessage);
                        log.info("[TaskReminderJob] Overdue email sent to: {} for task: {}",
                                item.email(), item.taskName());
                    } catch (Exception ex) {
                        // Log lỗi nhưng KHÔNG throw để không ảnh hưởng các email khác
                        log.error("[TaskReminderJob] Failed to send overdue email to {} for task '{}': {}",
                                item.email(), item.taskName(), ex.getMessage());
                    }
                }))
                .toList();

        // Chờ tất cả email gửi xong mới tiếp tục
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("[TaskReminderJob] Finished sending overdue emails.");
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
     * Tạo MimeMessage HTML với logo attachment
     */
    private MimeMessage buildMimeMessageWithAttachment(String to,
                                                       String subject,
                                                       String htmlBody)
            throws MessagingException, UnsupportedEncodingException, IOException {

        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(sender_Email, SENDER_NAME));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(to));
        email.setSubject(subject, "UTF-8");

        // Create multipart message with related type for inline images
        MimeMultipart multipart = new MimeMultipart("related");

        // HTML body part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        // Logo attachment part
        MimeBodyPart logoPart = new MimeBodyPart();
        try {
            ClassPathResource logoResource = new ClassPathResource(LOGO_PATH);
            InputStream logoStream = logoResource.getInputStream();
            byte[] logoBytes = logoStream.readAllBytes();
            logoStream.close();

            logoPart.setDataHandler(new DataHandler(
                    new ByteArrayDataSource(logoBytes, "image/png")
            ));
            logoPart.setHeader("Content-ID", "<" + LOGO_CID + ">");
            logoPart.setHeader("Content-Disposition", "inline; filename=\"logo.png\"");
            multipart.addBodyPart(logoPart);
        } catch (IOException ex) {
            log.warn("Failed to load logo image, email will be sent without logo: {}", ex.getMessage());
        }

        email.setContent(multipart);
        return email;
    }

    /**
     * Custom DataSource để embed binary data
     */
    private static class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final String contentType;

        public ByteArrayDataSource(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(data);
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "logo.png";
        }
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
