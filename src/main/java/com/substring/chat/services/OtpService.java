package com.substring.chat.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    // Store OTP with timestamp
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    // ============================================================
    // GENERATE OTP
    // ============================================================
    public String generateOtp(String email) {

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        otpStorage.put(email, new OtpData(otp, LocalDateTime.now()));

        return otp;
    }

    // ============================================================
    // VERIFY OTP
    // ============================================================
    public boolean verifyOtp(String email, String otp) {

        OtpData otpData = otpStorage.get(email);

        if (otpData == null) {
            return false;
        }

        // Check expiration
        if (otpData.getCreatedAt().plusMinutes(OTP_EXPIRY_MINUTES)
                .isBefore(LocalDateTime.now())) {

            otpStorage.remove(email);
            return false;
        }

        // Check OTP match
        if (otpData.getOtp().equals(otp)) {
            otpStorage.remove(email);
            return true;
        }

        return false;
    }

    // ============================================================
    // OTP DATA CLASS
    // ============================================================
    private static class OtpData {

        private final String otp;
        private final LocalDateTime createdAt;

        public OtpData(String otp, LocalDateTime createdAt) {
            this.otp = otp;
            this.createdAt = createdAt;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}