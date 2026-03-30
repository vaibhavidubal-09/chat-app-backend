package com.substring.chat.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomConnectionService {

    // roomId → set of active users
    private final Map<String, Set<String>> activeConnections = new ConcurrentHashMap<>();

    // ============================================================
    // CONNECT USER
    // ============================================================
    public void connectUser(String roomId, String userName) {

        activeConnections
                .computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet())
                .add(userName);
    }

    // ============================================================
    // DISCONNECT USER
    // ============================================================
    public void disconnectUser(String roomId, String userName) {

        Set<String> users = activeConnections.get(roomId);

        if (users != null) {
            users.remove(userName);

            // Remove room if empty
            if (users.isEmpty()) {
                activeConnections.remove(roomId);
            }
        }
    }

    // ============================================================
    // GET USERS IN ROOM
    // ============================================================
    public Set<String> getUsersInRoom(String roomId) {
        return activeConnections.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
    }

    // ============================================================
    // GET ALL ACTIVE CONNECTIONS
    // ============================================================
    public Map<String, Set<String>> getActiveConnections() {
        return activeConnections;
    }
}