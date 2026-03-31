package com.substring.chat.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final String BREVO_SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String brevoApiKey;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.sender.email:}") String senderEmail,
            @Value("${brevo.sender.name:Classroom Chat}") String senderName
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.brevoApiKey = brevoApiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public boolean sendOtp(String email, String otp) {
        return sendTransactionalEmail(
                email,
                "Classroom Chat OTP Verification",
                "Hello,\n\n" +
                        "Your OTP is: " + otp + "\n\n" +
                        "Valid for 5 minutes.\n\n" +
                        "Do NOT share this code.\n\n" +
                        "Regards,\nClassroom Chat"
        );
    }

    public boolean sendMeetingInvite(String email, String roomName, String meetingLink) {
        return sendTransactionalEmail(
                email,
                "Live Meeting Started: " + roomName,
                "Hello,\n\n" +
                        "Your class meeting has started for " + roomName + ".\n\n" +
                        "Join here:\n" + meetingLink + "\n\n" +
                        "Regards,\nClassroom Chat"
        );
    }

    private boolean sendTransactionalEmail(String email, String subject, String textContent) {
        if (brevoApiKey.isBlank() || senderEmail.isBlank()) {
            System.out.println("Failed to send email: Brevo API key or sender email is not configured");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "name", senderName,
                            "email", senderEmail
                    ),
                    "to", List.of(Map.of("email", email)),
                    "subject", subject,
                    "textContent", textContent
            );

            HttpEntity<String> requestEntity = new HttpEntity<>(
                    objectMapper.writeValueAsString(payload),
                    headers
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_SEND_EMAIL_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully to: " + email);
                return true;
            }

            System.out.println("Failed to send email. Brevo response: " + response.getStatusCode());
            return false;
        } catch (Exception e) {
            System.out.println("Failed to send email through Brevo API: " + e.getMessage());
            return false;
        }
    }
}
