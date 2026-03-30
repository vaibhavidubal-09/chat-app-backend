package com.substring.chat.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    private String id; // MongoDB unique id

    // Unique classroom code used by students to join
    private String roomId;

    // Classroom name (ex: Data Structures - A)
    private String roomName;

    // Email of teacher who created the class
    private String teacherEmail;

    // ✅ Students who successfully joined (after OTP)
    @Builder.Default
    private List<String> students = new ArrayList<>();

    // ✅ NEW: Allowed students (teacher controls this)
    @Builder.Default
    private List<String> allowedStudents = new ArrayList<>();

    // ✅ NEW: Blocked students
    @Builder.Default
    private List<String> blockedStudents = new ArrayList<>();

    // Chat messages in this classroom
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    // Private teacher-student conversations scoped to this classroom
    @Builder.Default
    private List<Message> privateMessages = new ArrayList<>();

    // Teacher moderation suggestions for suspicious messages
    @Builder.Default
    private List<ModerationAlert> moderationAlerts = new ArrayList<>();

    // Teacher can deactivate class
    private boolean active = true;

    // ✅ NEW: Meeting status
    private boolean meetingActive = false;

    // Class creation time
    private LocalDateTime createdAt = LocalDateTime.now();
}
