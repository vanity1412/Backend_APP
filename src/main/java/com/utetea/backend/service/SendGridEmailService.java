package com.utetea.backend.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class SendGridEmailService {

    @Value("${sendgrid.api-key:}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:watershoputetea@gmail.com}")
    private String fromEmail;

    @Value("${sendgrid.enabled:false}")
    private boolean sendGridEnabled;

    public boolean isEnabled() {
        return sendGridEnabled && sendGridApiKey != null && !sendGridApiKey.isEmpty();
    }

    public boolean sendEmail(String toEmail, String subject, String textContent) {
        if (!isEnabled()) {
            log.warn("SendGrid is not enabled or API key is missing");
            return false;
        }

        Email from = new Email(fromEmail, "UTE Tea");
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", textContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("SendGrid email sent successfully to: {}", toEmail);
                return true;
            } else {
                log.error("SendGrid failed with status {}: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("SendGrid error sending email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    public boolean sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (!isEnabled()) {
            log.warn("SendGrid is not enabled or API key is missing");
            return false;
        }

        Email from = new Email(fromEmail, "UTE Tea");
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("SendGrid HTML email sent successfully to: {}", toEmail);
                return true;
            } else {
                log.error("SendGrid failed with status {}: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("SendGrid error sending HTML email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
