package com.substring.chat.playload;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.ModerationAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocketEventPayload {

    private String type;
    private String roomId;
    private String roomName;
    private String senderEmail;
    private String recipientEmail;
    private String studentEmail;
    private String peerEmail;
    private String title;
    private String message;
    private String severity;
    private String messageId;
    private Boolean blocked;
    private Boolean typing;
    private Message chatMessage;
    private ModerationAlert moderationAlert;
}
