package com.substring.chat.playload;

public record AuthResponse(
        String token,
        String email,
        String userName,
        String role,
        boolean verified
) {
}
