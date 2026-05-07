package com.substring.chat.controllers;

import com.substring.chat.entities.Message;
import com.substring.chat.entities.Room;
import com.substring.chat.entities.User;
import com.substring.chat.playload.SocketEventPayload;
import com.substring.chat.repositories.RoomRepository;
import com.substring.chat.repositories.UserRepository;
import com.substring.chat.services.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SimpMessagingTemplate template;

    public RoomController(RoomRepository roomRepository,
                          UserRepository userRepository,
                          SessionService sessionService,
                          SimpMessagingTemplate template) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.template = template;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                        @RequestBody Map<String, String> data) {

        String teacherEmail = normalizeEmail(data.get("email"));
        String roomName = data.get("roomName");

        User teacher = authenticatedUser(token, teacherEmail);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only teachers can create class");
        }

        Room existingRoom = roomRepository.findByRoomId(roomName.toUpperCase()).orElse(null);
        if (existingRoom != null) {
            return ResponseEntity.badRequest().body("Class already exists");
        }

        Room room = new Room();
        room.setRoomId(roomName.toUpperCase());
        room.setRoomName(roomName);
        room.setTeacherEmail(teacherEmail);
        room.setStudents(new ArrayList<>());
        room.setMessages(new ArrayList<>());
        room.setPrivateMessages(new ArrayList<>());
        room.setBlockedStudents(new ArrayList<>());
        room.setModerationAlerts(new ArrayList<>());
        room.setActive(true);

        roomRepository.save(room);

        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                     @PathVariable String roomId) {

        if (!isAuthenticated(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Room not found");
        }

        return ResponseEntity.ok(room);
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                      @RequestBody Map<String, String> data) {

        String roomId = data.get("roomId");
        String email = normalizeEmail(data.get("email"));

        User user = authenticatedUser(token, email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        initializeRoomCollections(room);

        if (containsEmail(room.getBlockedStudents(), email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are blocked from this class");
        }

        if (!containsEmail(room.getStudents(), email)) {
            room.getStudents().add(email);
            roomRepository.save(room);
        }

        return ResponseEntity.ok(room);
    }

    @PostMapping("/addStudent")
    public ResponseEntity<?> addStudent(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                        @RequestBody Map<String, String> data) {

        String roomId = data.get("roomId");
        String teacherEmail = normalizeEmail(data.get("teacherEmail"));
        String studentEmail = normalizeEmail(data.get("studentEmail"));

        User teacher = authenticatedUser(token, teacherEmail);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only class teacher can add students");
        }

        Room room = roomRepository.findByRoomId(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        if (!normalizeEmail(room.getTeacherEmail()).equals(teacherEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only class teacher can add students");
        }

        initializeRoomCollections(room);

        room.getBlockedStudents().removeIf(email -> Objects.equals(normalizeEmail(email), studentEmail));

        if (!containsEmail(room.getStudents(), studentEmail)) {
            room.getStudents().add(studentEmail);
        }

        roomRepository.save(room);

        return ResponseEntity.ok("Student added successfully");
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateStudent(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @RequestBody Map<String, String> data) {

        String roomId = data.get("roomId");
        String teacherEmail = normalizeEmail(data.get("teacherEmail"));
        String studentEmail = normalizeEmail(data.get("studentEmail"));

        User teacher = authenticatedUser(token, teacherEmail);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        User student = userRepository.findByEmail(studentEmail).orElse(null);
        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only teacher can activate");
        }

        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        if (!normalizeEmail(room.getTeacherEmail()).equals(teacherEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only class teacher can activate students");
        }

        if (student == null) {
            return ResponseEntity.badRequest().body("Student not found");
        }

        initializeRoomCollections(room);

        room.getBlockedStudents().removeIf(email -> Objects.equals(normalizeEmail(email), studentEmail));

        if (!containsEmail(room.getStudents(), studentEmail)) {
            room.getStudents().add(studentEmail);
        }

        student.setBlocked(false);

        userRepository.save(student);
        roomRepository.save(room);
        publishRoomBlockEvent(room, teacherEmail, studentEmail, false);

        return ResponseEntity.ok("Student activated successfully in this class");
    }

    @PostMapping("/block")
    public ResponseEntity<?> blockStudent(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                          @RequestBody Map<String, String> data) {

        String roomId = data.get("roomId");
        String teacherEmail = normalizeEmail(data.get("teacherEmail"));
        String studentEmail = normalizeEmail(data.get("studentEmail"));

        User teacher = authenticatedUser(token, teacherEmail);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        User student = userRepository.findByEmail(studentEmail).orElse(null);
        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only teacher can block");
        }

        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        if (!normalizeEmail(room.getTeacherEmail()).equals(teacherEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only class teacher can block students");
        }

        if (student == null) {
            return ResponseEntity.badRequest().body("Student not found");
        }

        initializeRoomCollections(room);

        if (!containsEmail(room.getBlockedStudents(), studentEmail)) {
            room.getBlockedStudents().add(studentEmail);
        }

        if (room.getModerationAlerts() != null) {
            room.getModerationAlerts().stream()
                    .filter(alert -> studentEmail.equalsIgnoreCase(alert.getSenderEmail()))
                    .forEach(alert -> alert.setResolved(true));
        }

        room.getStudents().removeIf(email -> Objects.equals(normalizeEmail(email), studentEmail));
        roomRepository.save(room);
        publishRoomBlockEvent(room, teacherEmail, studentEmail, true);

        return ResponseEntity.ok("Student blocked successfully in this class");
    }

    @GetMapping("/teacher/{email}")
    public ResponseEntity<?> getTeacherClasses(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                               @PathVariable String email) {

        User teacher = authenticatedUser(token, normalizeEmail(email));
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        if (!"TEACHER".equalsIgnoreCase(teacher.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied");
        }

        List<Room> rooms = roomRepository.findByTeacherEmail(normalizeEmail(email));

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/student/{email}")
    public ResponseEntity<?> getStudentClasses(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                               @PathVariable String email) {

        String normalizedEmail = normalizeEmail(email);
        if (!isAuthenticated(token, normalizedEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<Room> rooms = roomRepository.findAll()
                .stream()
                .filter(r -> containsEmail(r.getStudents(), normalizedEmail))
                .toList();

        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getMessages(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                         @PathVariable String roomId) {

        if (!isAuthenticated(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        initializeRoomCollections(room);
        return ResponseEntity.ok(room.getMessages());
    }

    @GetMapping("/{roomId}/private-messages")
    public ResponseEntity<?> getPrivateMessages(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                                @PathVariable String roomId,
                                                @RequestParam String userEmail,
                                                @RequestParam String peerEmail) {

        String normalizedUserEmail = normalizeEmail(userEmail);
        String normalizedPeerEmail = normalizeEmail(peerEmail);

        if (!isAuthenticated(token, normalizedUserEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Room room = roomRepository.findByRoomId(roomId).orElse(null);

        if (room == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        initializeRoomCollections(room);

        boolean isTeacher = normalizeEmail(room.getTeacherEmail()).equals(normalizedUserEmail);
        boolean isTeacherPeer = normalizeEmail(room.getTeacherEmail()).equals(normalizedPeerEmail);
        boolean validParticipantPair = (isTeacher && isMemberOrBlocked(room, normalizedPeerEmail))
                || (isTeacherPeer && isMemberOrBlocked(room, normalizedUserEmail));

        if (!validParticipantPair) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Private messaging is only allowed between the teacher and class students");
        }

        List<Message> privateMessages = room.getPrivateMessages().stream()
                .filter(message -> sameConversation(message, normalizedUserEmail, normalizedPeerEmail))
                .toList();

        return ResponseEntity.ok(privateMessages);
    }

    private void publishRoomBlockEvent(Room room, String actorEmail, String studentEmail, boolean blocked) {
        SocketEventPayload roomEvent = SocketEventPayload.builder()
                .type(blocked ? "ROOM_MEMBER_BLOCKED" : "ROOM_MEMBER_UNBLOCKED")
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .senderEmail(actorEmail)
                .studentEmail(studentEmail)
                .blocked(blocked)
                .title(blocked ? "Student blocked" : "Student activated")
                .message(blocked
                        ? studentEmail + " was blocked in " + room.getRoomName()
                        : studentEmail + " was reactivated in " + room.getRoomName())
                .build();

        template.convertAndSend("/topic/room-events/" + room.getRoomId(), roomEvent);
        template.convertAndSend("/topic/notifications/" + topicKey(studentEmail), roomEvent);
        template.convertAndSend("/topic/notifications/" + topicKey(room.getTeacherEmail()), roomEvent);
    }

    private void initializeRoomCollections(Room room) {
        if (room.getStudents() == null) {
            room.setStudents(new ArrayList<>());
        }

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

    private boolean containsEmail(List<String> emails, String email) {
        if (emails == null || email == null) {
            return false;
        }

        String normalizedEmail = normalizeEmail(email);
        return emails.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeEmail)
                .anyMatch(normalizedEmail::equals);
    }

    private boolean isMemberOrBlocked(Room room, String email) {
        return containsEmail(room.getStudents(), email)
                || containsEmail(room.getBlockedStudents(), email);
    }

    private boolean sameConversation(Message message, String userEmail, String peerEmail) {
        String sender = normalizeEmail(message.getSender());
        String recipient = normalizeEmail(message.getRecipient());

        return (sender.equals(userEmail) && recipient.equals(peerEmail))
                || (sender.equals(peerEmail) && recipient.equals(userEmail));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String topicKey(String email) {
        return normalizeEmail(email).replaceAll("[^a-z0-9]", "_");
    }

    private boolean isAuthenticated(String token) {
        return token != null && !sessionService.getEmail(token).isBlank();
    }

    private boolean isAuthenticated(String token, String email) {
        return token != null && sessionService.isValid(token, email);
    }

    private User authenticatedUser(String token, String email) {
        if (!isAuthenticated(token, email)) {
            return null;
        }
        User user = userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        if (user == null || !user.isVerified()) {
            return null;
        }
        return user;
    }
}
