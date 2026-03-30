package com.substring.chat.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "http://localhost:5173")
public class FileController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    // ============================================================
    // 📂 FILE UPLOAD
    // ============================================================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {

        try {

            // ❌ Empty file validation
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // 📏 Limit file size (20MB)
            if (file.getSize() > 20 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File too large");
            }

            // 📁 Create upload directory if not exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 🔑 Generate unique file name
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            // 📦 Destination
            File destination = new File(UPLOAD_DIR + fileName);

            // Save file
            file.transferTo(destination);

            // Return file URL
            String fileUrl = "/uploads/" + fileName;

            return ResponseEntity.ok(fileUrl);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed");

        }
    }
}