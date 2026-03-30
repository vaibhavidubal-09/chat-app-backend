package com.substring.chat.repositories;

import com.substring.chat.entities.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends MongoRepository<Room, String> {

    // Find a room by classroom code
    Optional<Room> findByRoomId(String roomId);

    // Get all classes created by a teacher
    List<Room> findByTeacherEmail(String teacherEmail);

    // Get classes where student has joined
    List<Room> findByStudentsContaining(String email);

    // Check if room code already exists
    boolean existsByRoomId(String roomId);
}