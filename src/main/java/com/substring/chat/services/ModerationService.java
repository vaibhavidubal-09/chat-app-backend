package com.substring.chat.services;

import com.substring.chat.entities.ModerationAlert;
import com.substring.chat.entities.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ModerationService {

    private static final Set<String> MEDIUM_WORDS = Set.of(
            "idiot", "stupid", "hate", "moron", "damn", "hell",
            "abuse", "loser", "dumb", "trash", "pathetic", "worthless"
    );

    private static final Set<String> HIGH_WORDS = Set.of(
            "cheat", "fraud", "scam", "threat", "harass", "blackmail"
    );

    private static final List<PatternRule> PATTERN_RULES = List.of(
            new PatternRule(Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE),
                    "External link detected", "MEDIUM"),
            new PatternRule(Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
                    "Script injection pattern detected", "HIGH"),
            new PatternRule(Pattern.compile("(drop\\s+table|delete\\s+from|truncate\\s+table)", Pattern.CASE_INSENSITIVE),
                    "Database attack pattern detected", "HIGH"),
            new PatternRule(Pattern.compile("(kill yourself|self harm|suicide|kill you|kill him|kill her|i('|\\s)?ll kill|murder|stab you|shoot you)", Pattern.CASE_INSENSITIVE),
                    "Self-harm or violent language detected", "HIGH"),
            new PatternRule(Pattern.compile("(otp\\s*share|share\\s+password|bank\\s+account|upi\\s+pin)", Pattern.CASE_INSENSITIVE),
                    "Sensitive credential or payment phrase detected", "HIGH")
    );

    public Optional<ModerationAlert> analyze(String content, User sender) {
        if (content == null || content.isBlank() || sender == null) {
            return Optional.empty();
        }

        String normalized = content.toLowerCase();

        for (String word : HIGH_WORDS) {
            if (containsWholeWord(normalized, word)) {
                return Optional.of(buildAlert(sender, content,
                        "High-risk abusive or coercive language detected",
                        "HIGH"));
            }
        }

        for (String word : MEDIUM_WORDS) {
            if (containsWholeWord(normalized, word)) {
                return Optional.of(buildAlert(sender, content,
                        "Improper classroom language detected",
                        "MEDIUM"));
            }
        }

        for (PatternRule rule : PATTERN_RULES) {
            if (rule.pattern.matcher(content).find()) {
                return Optional.of(buildAlert(sender, content, rule.reason, rule.severity));
            }
        }

        if (looksLikeShouting(content)) {
            return Optional.of(buildAlert(sender, content,
                    "Aggressive all-caps or repeated punctuation detected",
                    "MEDIUM"));
        }

        return Optional.empty();
    }

    private boolean containsWholeWord(String input, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(input).find();
    }

    private boolean looksLikeShouting(String content) {
        String lettersOnly = content.replaceAll("[^A-Za-z]", "");
        if (lettersOnly.length() < 8) {
            return false;
        }

        long uppercaseCount = lettersOnly.chars().filter(Character::isUpperCase).count();
        boolean repeatedPunctuation = content.contains("!!!") || content.contains("???");

        return uppercaseCount >= Math.round(lettersOnly.length() * 0.7) || repeatedPunctuation;
    }

    private ModerationAlert buildAlert(User sender, String content, String reason, String severity) {
        return ModerationAlert.builder()
                .id(UUID.randomUUID().toString())
                .senderEmail(sender.getEmail())
                .senderName(sender.getUserName())
                .messageContent(content)
                .reason(reason)
                .severity(severity)
                .detectedAt(LocalDateTime.now())
                .resolved(false)
                .build();
    }

    private static class PatternRule {
        private final Pattern pattern;
        private final String reason;
        private final String severity;

        private PatternRule(Pattern pattern, String reason, String severity) {
            this.pattern = pattern;
            this.reason = reason;
            this.severity = severity;
        }
    }
}
