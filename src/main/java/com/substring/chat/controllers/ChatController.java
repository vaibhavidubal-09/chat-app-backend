package com.substring.chat.controllers;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.ModerationAlert;
import com.substring.chat.entities.Room;
import com.substring.chat.entities.User;
import com.substring.chat.playload.EditRequest;
import com.substring.chat.playload.MessageRequest;
import com.substring.chat.playload.SocketEventPayload;
import com.substring.chat.repositories.RoomRepository;
import com.substring.chat.repositories.UserRepository;
import com.substring.chat.services.EmailService;
import com.substring.chat.services.ModerationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Controller
public class ChatController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate template;
    private final EmailService emailService;
    private final ModerationService moderationService;

    private static final Map<String, Set<String>> onlineUsers = new HashMap<>();

    public ChatController(RoomRepository roomRepository,
                          UserRepository userRepository,
                          SimpMessagingTemplate template,
                          EmailService emailService,
                          ModerationService moderationService) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.template = template;
        this.emailService = emailService;
        this.moderationService = moderationService;
    }

    @MessageMapping("/sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId,
                            MessageRequest request) {

        Room room = getRoom(roomId);
        User sender = getUser(request.getSender());

        initializeRoomCollections(room);
        assertCanSend(room, sender);

        boolean isPrivate = request.isPrivateMessage() || hasText(request.getRecipient());

        if (isPrivate) {
            handlePrivateMessage(room, sender, request);
            return;
        }

        Message message = buildMessage(request, null, false);
        room.getMessages().add(message);
        maybePublishModerationAlert(room, sender, message);

        roomRepository.save(room);
        template.convertAndSend("/topic/room/" + roomId, message);
    }

    @MessageMapping("/blockStudent/{roomId}")
    public void blockStudent(@DestinationVariable String roomId,
                             String email) {

        Room room = getRoom(roomId);
        initializeRoomCollections(room);

        room.getBlockedStudents().add(email);
        room.getStudents().remove(email);

        roomRepository.save(room);
    }

    @MessageMapping("/startMeeting/{roomId}")
    public void startMeeting(@DestinationVariable String roomId,
                             String teacherEmail) {

        Room room = getRoom(roomId);

        if (!normalizeEmail(room.getTeacherEmail()).equals(normalizeEmail(teacherEmail))) {
            throw new RuntimeException("Only teacher can start meeting");
        }

        String link = "https://meet.jit.si/" + roomId + "-" + System.currentTimeMillis();

        room.setMeetingActive(true);
        roomRepository.save(room);

        if (room.getStudents() != null) {
            for (String studentEmail : room.getStudents()) {
                emailService.sendMeetingInvite(studentEmail, room.getRoomName(), link);
            }
        }

        template.convertAndSend("/topic/meeting/" + roomId, link);
    }

    @MessageMapping("/stopMeeting/{roomId}")
    public void stopMeeting(@DestinationVariable String roomId,
                            String teacherEmail) {

        Room room = getRoom(roomId);

        if (!normalizeEmail(room.getTeacherEmail()).equals(normalizeEmail(teacherEmail))) {
            throw new RuntimeException("Only teacher can stop meeting");
        }

        room.setMeetingActive(false);
        template.convertAndSend("/topic/meeting/" + roomId, "ENDED");
        roomRepository.save(room);
    }

    @MessageMapping("/edit/{roomId}")
    public void edit(@DestinationVariable String roomId,
                     EditRequest req) {

        Room room = getRoom(roomId);
        initializeRoomCollections(room);

        Message roomMessage = findMessage(room.getMessages(), req.getMessageId());
        if (roomMessage != null) {
            roomMessage.setContent(req.getContent());
            roomMessage.setEdited(true);
            roomRepository.save(room);
            template.convertAndSend("/topic/edit/" + roomId, roomMessage);
            return;
        }

        Message privateMessage = findMessage(room.getPrivateMessages(), req.getMessageId());
        if (privateMessage != null) {
            privateMessage.setContent(req.getContent());
            privateMessage.setEdited(true);
            roomRepository.save(room);
            notifyPrivateParticipants(room, privateMessage, "PRIVATE_MESSAGE_UPDATED",
                    "Private message updated", "A private message was edited.");
            return;
        }

        throw new RuntimeException("Message not found");
    }

    @MessageMapping("/delete/{roomId}")
    public void delete(@DestinationVariable String roomId,
                       String messageId) {

        Room room = getRoom(roomId);
        initializeRoomCollections(room);

        Message roomMessage = findMessage(room.getMessages(), messageId);
        if (roomMessage != null) {
            room.getMessages().removeIf(m -> Objects.equals(m.getId(), messageId));
            roomRepository.save(room);
            template.convertAndSend("/topic/delete/" + roomId, messageId);
            return;
        }

        Message privateMessage = findMessage(room.getPrivateMessages(), messageId);
        if (privateMessage != null) {
            room.getPrivateMessages().removeIf(m -> Objects.equals(m.getId(), messageId));
            roomRepository.save(room);
            notifyPrivateParticipants(room, privateMessage, "PRIVATE_MESSAGE_DELETED",
                    "Private message deleted", "A private message was removed.");
            return;
        }

        throw new RuntimeException("Message not found");
    }

    @MessageMapping("/typing/{roomId}")
    @SendTo("/topic/typing/{roomId}")
    public Map<String, Object> typing(@DestinationVariable String roomId,
                                      Map<String, Object> payload) {
        return payload;
    }

    @MessageMapping("/privateTyping/{roomId}")
    public void privateTyping(@DestinationVariable String roomId,
                              Map<String, Object> payload) {

        Room room = getRoom(roomId);

        String senderEmail = normalizeEmail(asString(payload.get("sender")));
        String recipientEmail = normalizeEmail(asString(payload.get("recipient")));
        boolean typing = Boolean.TRUE.equals(payload.get("typing"));

        validatePrivateParticipants(room, senderEmail, recipientEmail);

        SocketEventPayload event = SocketEventPayload.builder()
                .type("PRIVATE_TYPING")
                .roomId(roomId)
                .senderEmail(senderEmail)
                .recipientEmail(recipientEmail)
                .peerEmail(senderEmail)
                .typing(typing)
                .build();

        template.convertAndSend("/topic/notifications/" + topicKey(recipientEmail), event);
    }

    @MessageMapping("/join/{roomId}")
    @SendTo("/topic/online/{roomId}")
    public Set<String> join(@DestinationVariable String roomId,
                            String email) {

        Room room = getRoom(roomId);

        if (isBlockedInRoom(room, email)) {
            throw new RuntimeException("Blocked from this class");
        }

        onlineUsers.putIfAbsent(roomId, new HashSet<>());
        onlineUsers.get(roomId).add(normalizeEmail(email));

        return onlineUsers.get(roomId);
    }

    @MessageMapping("/seen/{roomId}")
    public void seen(@DestinationVariable String roomId,
                     Map<String, Object> payload) {

        Room room = getRoom(roomId);
        initializeRoomCollections(room);

        String messageId = asString(payload.get("messageId"));
        String user = normalizeEmail(asString(payload.get("user")));

        Message roomMessage = findMessage(room.getMessages(), messageId);
        if (roomMessage != null) {
            roomMessage.setSeen(true);
            roomRepository.save(room);
            template.convertAndSend("/topic/seen/" + roomId, Map.of(
                    "messageId", messageId,
                    "user", user
            ));
            return;
        }

        Message privateMessage = findMessage(room.getPrivateMessages(), messageId);
        if (privateMessage != null) {
            privateMessage.setSeen(true);
            roomRepository.save(room);

            SocketEventPayload event = SocketEventPayload.builder()
                    .type("PRIVATE_MESSAGE_SEEN")
                    .roomId(roomId)
                    .senderEmail(user)
                    .recipientEmail(otherParticipant(privateMessage, user))
                    .peerEmail(user)
                    .messageId(messageId)
                    .chatMessage(privateMessage)
                    .build();

            template.convertAndSend("/topic/notifications/" + topicKey(privateMessage.getSender()), event);
            template.convertAndSend("/topic/notifications/" + topicKey(privateMessage.getRecipient()), event);
        }
    }

    private void handlePrivateMessage(Room room, User sender, MessageRequest request) {
        initializeRoomCollections(room);

        String recipientEmail = normalizeEmail(request.getRecipient());
        User recipient = getUser(recipientEmail);

        validatePrivateParticipants(room, sender.getEmail(), recipient.getEmail());

        Message message = buildMessage(request, recipient.getEmail(), true);
        room.getPrivateMessages().add(message);
        maybePublishModerationAlert(room, sender, message);

        roomRepository.save(room);
        notifyPrivateParticipants(room, message, "PRIVATE_MESSAGE",
                "New private message",
                sender.getEmail() + " sent a private message.");
    }

    private void maybePublishModerationAlert(Room room, User sender, Message message) {
        if (!"TEXT".equalsIgnoreCase(message.getType())) {
            return;
        }

        moderationService.analyze(message.getContent(), sender).ifPresent(alert -> {
            room.getModerationAlerts().add(0, alert);

            template.convertAndSend("/topic/moderation/" + room.getRoomId(), alert);

            SocketEventPayload teacherNotification = SocketEventPayload.builder()
                    .type("MODERATION_ALERT")
                    .roomId(room.getRoomId())
                    .roomName(room.getRoomName())
                    .senderEmail(sender.getEmail())
                    .title("Moderation alert")
                    .message(alert.getReason())
                    .severity(alert.getSeverity())
                    .moderationAlert(alert)
                    .chatMessage(message)
                    .build();

            template.convertAndSend("/topic/notifications/" + topicKey(room.getTeacherEmail()), teacherNotification);

            if (!normalizeEmail(sender.getEmail()).equals(normalizeEmail(room.getTeacherEmail()))) {
                SocketEventPayload senderNotification = SocketEventPayload.builder()
                        .type("MODERATION_WARNING")
                        .roomId(room.getRoomId())
                        .roomName(room.getRoomName())
                        .senderEmail(sender.getEmail())
                        .title("Message flagged")
                        .message(alert.getReason())
                        .severity(alert.getSeverity())
                        .moderationAlert(alert)
                        .chatMessage(message)
                        .build();

                template.convertAndSend("/topic/notifications/" + topicKey(sender.getEmail()), senderNotification);
            }
        });
    }

    private void notifyPrivateParticipants(Room room, Message message, String type, String title, String body) {
        SocketEventPayload senderEvent = SocketEventPayload.builder()
                .type(type)
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .senderEmail(message.getSender())
                .recipientEmail(message.getRecipient())
                .peerEmail(message.getRecipient())
                .title(title)
                .message(body)
                .messageId(message.getId())
                .chatMessage(message)
                .build();

        SocketEventPayload recipientEvent = SocketEventPayload.builder()
                .type(type)
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .senderEmail(message.getSender())
                .recipientEmail(message.getRecipient())
                .peerEmail(message.getSender())
                .title(title)
                .message(body)
                .messageId(message.getId())
                .chatMessage(message)
                .build();

        template.convertAndSend("/topic/notifications/" + topicKey(message.getSender()), senderEvent);
        template.convertAndSend("/topic/notifications/" + topicKey(message.getRecipient()), recipientEvent);
    }

    private void validatePrivateParticipants(Room room, String senderEmail, String recipientEmail) {
        String normalizedTeacher = normalizeEmail(room.getTeacherEmail());
        String normalizedSender = normalizeEmail(senderEmail);
        String normalizedRecipient = normalizeEmail(recipientEmail);

        boolean teacherToStudent = normalizedTeacher.equals(normalizedSender)
                && isMemberOrBlocked(room, normalizedRecipient);
        boolean studentToTeacher = normalizedTeacher.equals(normalizedRecipient)
                && isMemberOrBlocked(room, normalizedSender);

        if (!(teacherToStudent || studentToTeacher)) {
            throw new RuntimeException("Private messaging is only allowed between the teacher and class students");
        }

        if (!normalizedTeacher.equals(normalizedSender) && isBlockedInRoom(room, normalizedSender)) {
            throw new RuntimeException("Blocked from this class");
        }
    }

    private void assertCanSend(Room room, User sender) {
        if (!sender.isVerified()) {
            throw new RuntimeException("Verify your email first");
        }

        if (isBlockedInRoom(room, sender.getEmail())) {
            throw new RuntimeException("Blocked from this class");
        }
    }

    private Message buildMessage(MessageRequest request, String recipient, boolean isPrivate) {
        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setSender(normalizeEmail(request.getSender()));
        message.setRecipient(hasText(recipient) ? normalizeEmail(recipient) : null);
        message.setPrivateMessage(isPrivate);
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setFileUrl(request.getFileUrl());
        message.setReplyTo(request.getReplyTo());
        message.setTimeStamp(LocalDateTime.now());
        return message;
    }

    private void initializeRoomCollections(Room room) {
        if (room.getBlockedStudents() == null) {
            room.setBlockedStudents(new ArrayList<>());
        }

        if (room.getMessages() == null) {
            room.setMessages(new ArrayList<>());
        }

        if (room.getPrivateMessages() == null) {
            room.setPrivateMessages(new ArrayList<>());
        }

        if (room.getModerationAlerts() == null) {
            room.setModerationAlerts(new ArrayList<>());
        }
    }

    private Room getRoom(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Message findMessage(List<Message> messages, String messageId) {
        if (messages == null || messageId == null) {
            return null;
        }

        return messages.stream()
                .filter(message -> Objects.equals(message.getId(), messageId))
                .findFirst()
                .orElse(null);
    }

    private String otherParticipant(Message message, String userEmail) {
        if (message == null) {
            return null;
        }

        String normalizedUser = normalizeEmail(userEmail);
        return normalizedUser.equals(normalizeEmail(message.getSender()))
                ? message.getRecipient()
                : message.getSender();
    }

    private boolean isMemberOrBlocked(Room room, String email) {
        return containsEmail(room.getStudents(), email)
                || containsEmail(room.getBlockedStudents(), email);
    }

    private boolean isBlockedInRoom(Room room, String email) {
        return containsEmail(room.getBlockedStudents(), email);
    }

    private boolean containsEmail(List<String> emails, String targetEmail) {
        if (emails == null || targetEmail == null) {
            return false;
        }

        String normalizedTarget = normalizeEmail(targetEmail);
        return emails.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeEmail)
                .anyMatch(normalizedTarget::equals);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String topicKey(String email) {
        return normalizeEmail(email).replaceAll("[^a-z0-9]", "_");
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
