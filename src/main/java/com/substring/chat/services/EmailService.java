package com.substring.chat.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOtp(String email, String otp) {

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            // ✅ MUST match your Gmail
            message.setFrom("vaibhavidubal09@gmail.com");

            message.setTo(email);
            message.setSubject("Classroom Chat OTP Verification");

            message.setText(
                    "Hello,\n\n" +
                            "Your OTP is: " + otp + "\n\n" +
                            "Valid for 5 minutes.\n\n" +
                            "Do NOT share this code.\n\n" +
                            "Regards,\nClassroom Chat"
            );

            mailSender.send(message);

            System.out.println("✅ OTP email sent to: " + email);

            return true;

        } catch (MailException e) {

            System.out.println("❌ Failed to send OTP email: " + e.getMessage());

            return false;
        }
    }

    public boolean sendMeetingInvite(String email, String roomName, String meetingLink) {

        try {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("vaibhavidubal09@gmail.com");
            message.setTo(email);
            message.setSubject("Live Meeting Started: " + roomName);
            message.setText(
                    "Hello,\n\n" +
                            "Your class meeting has started for " + roomName + ".\n\n" +
                            "Join here:\n" + meetingLink + "\n\n" +
                            "Regards,\nClassroom Chat"
            );

            mailSender.send(message);
            System.out.println("Meeting invite sent to: " + email);
            return true;

        } catch (MailException e) {

            System.out.println("Failed to send meeting email: " + e.getMessage());
            return false;
        }
    }
}
