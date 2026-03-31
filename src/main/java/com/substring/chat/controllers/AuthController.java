package com.substring.chat.controllers;

import com.substring.chat.entities.User;
import com.substring.chat.repositories.UserRepository;
import com.substring.chat.services.OtpService;
import com.substring.chat.services.EmailService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    public AuthController(OtpService otpService,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // ================= SEND OTP =================
//    @PostMapping("/send-otp")
//    public ResponseEntity<?> sendOtp(@RequestParam String email,
//                                     @RequestParam String role)
//
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String role = body.get("role");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setRole(role);
            user.setVerified(false);
            user.setBlocked(false);
            userRepository.save(user);
        }

        String otp = otpService.generateOtp(email);

       boolean sent = emailService.sendOtp(email, otp);

        if(!sent){
           return ResponseEntity.badRequest().body("Failed to send OTP");
      }

        return ResponseEntity.ok("OTP Sent Successfully");
    }

    // ================= VERIFY OTP =================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email,
                                       @RequestParam String otp) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }

        user.setVerified(true);
        userRepository.save(user);

        return ResponseEntity.ok("Verified Successfully");
    }

    // ================= CHECK VERIFIED =================
    @GetMapping("/check-verified")
    public ResponseEntity<Boolean> checkVerified(@RequestParam String email) {

        User user = userRepository.findByEmail(email).orElse(null);

        return ResponseEntity.ok(user != null && user.isVerified());
    }
}