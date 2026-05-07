package com.substring.chat.controllers;

import com.substring.chat.entities.User;
import com.substring.chat.playload.AuthResponse;
import com.substring.chat.repositories.UserRepository;
import com.substring.chat.services.EmailService;
import com.substring.chat.services.OtpService;
import com.substring.chat.services.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://front-chat-vert.vercel.app"
})
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SessionService sessionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(OtpService otpService,
                          UserRepository userRepository,
                          EmailService emailService,
                          SessionService sessionService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.sessionService = sessionService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email,
                                     @RequestParam(required = false) String role) {

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setRole("STUDENT");
            user.setVerified(false);
            user.setBlocked(false);
            userRepository.save(user);
        }

        String otp = otpService.generateOtp(normalizedEmail);

        boolean sent = emailService.sendOtp(normalizedEmail, otp);
        if (!sent) {
            return ResponseEntity.badRequest().body("Failed to send OTP");
        }

        return ResponseEntity.ok("OTP Sent Successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email,
                                       @RequestParam String otp,
                                       @RequestParam String password) {

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Password is required");
        }

        if (!otpService.verifyOtp(normalizedEmail, otp)) {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        } else if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }

        user.setVerified(true);
        userRepository.save(user);

        String token = sessionService.createSession(normalizedEmail);
        return ResponseEntity.ok(buildResponse(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password) {

        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        if (!user.isVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email not verified");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password not set");
        }

        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }

        String token = sessionService.createSession(normalizedEmail);
        return ResponseEntity.ok(buildResponse(user, token));
    }

    @GetMapping("/check-verified")
    public ResponseEntity<Boolean> checkVerified(@RequestParam String email) {

        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        return ResponseEntity.ok(user != null && user.isVerified());
    }

    private AuthResponse buildResponse(User user, String token) {
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getUserName(),
                user.getRole(),
                user.isVerified()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
