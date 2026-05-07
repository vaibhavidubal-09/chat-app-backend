package com.substring.chat.services;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, String> tokenToEmail = new ConcurrentHashMap<>();

    public String createSession(String email) {
        String token = UUID.randomUUID().toString();
        tokenToEmail.put(token, normalize(email));
        return token;
    }

    public String getEmail(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }

        return tokenToEmail.getOrDefault(token.trim(), "");
    }

    public boolean isValid(String token, String email) {
        return normalize(email).equals(getEmail(token));
    }

    public void invalidate(String token) {
        if (token != null) {
            tokenToEmail.remove(token.trim());
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
