package com.example.smd.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

//
    /**
     * Tạo HTML template cho welcome email
     */
    public String buildWelcomeEmail(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome to SMD</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to SMD!</h1>
                    </div>
                    <div class="content">
                        <h2>Welcome aboard, %s!</h2>
                        <p>Thank you for joining SMD System. We're excited to have you on board!</p>
                        <p>You can now start using our platform to manage your syllabus and learning materials.</p>
                        <p>If you have any questions, feel free to contact our support team.</p>
                        <p>Best regards,<br>The SMD Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 SMD. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }

    /**
     * Tạo HTML template cho test email
     */
    public String buildTestEmail(String recipientName, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Test Email</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #17a2b8; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SMD Test Email</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>This is a test email from SMD System.</p>
                        <p><strong>Message:</strong></p>
                        <p>%s</p>
                        <p>If you received this email, the email service is working correctly!</p>
                        <p>Best regards,<br>The SMD Team</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 SMD. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(recipientName, message);
    }
}
