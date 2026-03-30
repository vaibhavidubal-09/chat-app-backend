package com.substring.chat.entities;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationAlert {

    private String id;
    private String senderEmail;
    private String senderName;
    private String messageContent;
    private String reason;
    private String severity;
    private LocalDateTime detectedAt;
    private boolean resolved;
}
