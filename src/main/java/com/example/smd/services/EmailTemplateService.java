package com.example.smd.services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailTemplateService {

    private static final String TEMPLATE_FOLDER = "templeteEmail/";

    public String buildWelcomeEmail(String userName) {
        Map<String, String> variables = defaultVariables();
        variables.put("userName", safe(userName));
        return renderTemplate("welcome.html", variables);
    }

    public String buildTestEmail(String recipientName, String message) {
        Map<String, String> variables = defaultVariables();
        variables.put("recipientName", safe(recipientName));
        variables.put("message", safe(message));
        return renderTemplate("test.html", variables);
    }

    public String buildAccountCreatedEmail(String recipientName, String email, String message) {
        Map<String, String> variables = defaultVariables();
        variables.put("recipientName", safe(recipientName));
        variables.put("email", safe(email));
        variables.put("message", safe(message));
        return renderTemplate("account-created.html", variables);
    }

    private Map<String, String> defaultVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("year", String.valueOf(Year.now().getValue()));
        return vars;
    }

    private String renderTemplate(String templateName, Map<String, String> variables) {
        String html = loadTemplate(templateName);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", safe(entry.getValue()));
        }
        return html;
    }

    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_FOLDER + templateName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load email template: " + templateName, e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
