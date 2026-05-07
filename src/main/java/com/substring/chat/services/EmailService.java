package com.substring.chat.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String senderEmail,
            @Value("${brevo.sender.name:Classroom Chat}") String senderName
    ) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public boolean sendOtp(String email, String otp) {
        return sendPlainTextEmail(
                email,
                "Classroom Chat OTP Verification",
                "Hello,\n\n" +
                        "Your OTP is: " + otp + "\n\n" +
                        "Valid for 5 minutes.\n\n" +
                        "Do NOT share this code.\n\n" +
                        "Regards,\n" + senderName
        );
    }

    public boolean sendMeetingInvite(String email, String roomName, String meetingLink) {
        return sendPlainTextEmail(
                email,
                "Live Meeting Started: " + roomName,
                "Hello,\n\n" +
                        "Your class meeting has started for " + roomName + ".\n\n" +
                        "Join here:\n" + meetingLink + "\n\n" +
                        "Regards,\n" + senderName
        );
    }

    private boolean sendPlainTextEmail(String email, String subject, String textContent) {
        if (senderEmail == null || senderEmail.isBlank()) {
            System.out.println("Failed to send email: Gmail sender email is not configured");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(textContent);
            mailSender.send(message);

            System.out.println("Email sent successfully to: " + email);
            return true;
        } catch (Exception e) {
            System.out.println("Failed to send email through Gmail SMTP: " + e.getMessage());
            return false;
        }
    }
}