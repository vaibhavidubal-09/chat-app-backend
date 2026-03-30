package com.substring.chat.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    // Display name
    private String userName;

    // Email used for login
    private String email;

    // Email verified via OTP
    @Builder.Default
    private boolean verified = false;

    // TEACHER or STUDENT
    private String role;

    // Teacher can block student
    @Builder.Default
    private boolean blocked = false;

    // Account creation time
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}