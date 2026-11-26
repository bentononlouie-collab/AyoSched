package com.ayosched.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import java.util.Base64;

@RestController
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true") 
public class ACCOUNT {
    
    private final JdbcTemplate jdbcTemplate;
    public ACCOUNT (JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello! AyoSched Backend is working!";
    }

    @PostMapping("/api/admin/login")
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Map<String, String>credentials, HttpSession session) {
        String email = credentials.get("email");
        String password = credentials.get("password");

            if(email == null || password == null || email.isBlank() || password.isBlank()){
                return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
            }
        
            Integer adminresult = jdbcTemplate.queryForObject("SELECT COUNT (*) FROM admins " + "WHERE email =? AND password = crypt(?, password)", Integer.class, email, password);
            
            if(adminresult != null && adminresult > 0){
                String fullName = jdbcTemplate.queryForObject("SELECT full_name FROM admins WHERE email =?", String.class, email);
                String firstName = fullName.trim().split("\\s+")[0];
                return ResponseEntity.ok(Map.of("message","Admin login successfully!", "role", "admin", "firstName", firstName));
            }

            Integer studentresult = jdbcTemplate.queryForObject("SELECT COUNT (*) FROM students " + "WHERE email =? AND password = crypt(?, password)", Integer.class, email, password);
            
            if(studentresult != null && studentresult > 0){
                Long studentId = jdbcTemplate.queryForObject("SELECT id FROM students WHERE email = ?", Long.class, email);
                String fullName = jdbcTemplate.queryForObject("SELECT full_name FROM students WHERE email =?", String.class, email);
                String firstName = fullName.trim().split("\\s+")[0];
                session.setAttribute("studentId", studentId);
                return ResponseEntity.ok(Map.of("message","Student login successfully!", "role", "student", "firstName", firstName, "fullName", fullName, "email", email, "studentId", studentId.toString()));
            }


        return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
    }

   @GetMapping("/api/student/session")
        public ResponseEntity<Map<String, String>> checkSession(HttpSession session) {
            Long studentId = (Long) session.getAttribute("studentId");
                if (studentId == null) {
                    return ResponseEntity.status(401)
                    .body(Map.of("message", "No active student session."));
                }
            return ResponseEntity.ok(
            Map.of("message", "Student session active!",
               "studentId", studentId.toString())
    );
    }
    
    @PutMapping("/api/student/profile")
public ResponseEntity<Map<String, String>> updateProfile(
        @RequestBody Map<String, String> data,
        HttpSession session) {

    Long studentId = (Long) session.getAttribute("studentId");

    if (studentId == null) {
        return ResponseEntity.status(401)
            .body(Map.of("message", "Please log in first."));
    }

    String fullName = data.get("fullName");
    String email = data.get("email");

    if (fullName == null || fullName.isBlank() ||
        email == null || email.isBlank()) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "All fields are required."));
    }

    if (!email.matches("^\\S+@\\S+\\.\\S+$")) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Invalid email address."));
    }

    Integer existingEmail = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM students WHERE email = ? AND id <> ?",
        Integer.class, email, studentId
    );

    if (existingEmail != null && existingEmail > 0) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Email is already being used."));
    }

    jdbcTemplate.update(
        "UPDATE students SET full_name = ?, email = ? WHERE id = ?",
        fullName.trim(), email.trim(), studentId
    );

    return ResponseEntity.ok(
        Map.of("message", "Profile updated successfully!")
    );
}

@PutMapping("/api/student/password")
public ResponseEntity<Map<String, String>> updatePassword(
        @RequestBody Map<String, String> data,
        HttpSession session) {

    Long studentId = (Long) session.getAttribute("studentId");

    if (studentId == null) {
        return ResponseEntity.status(401)
            .body(Map.of("message", "Please log in first."));
    }

    String currentPassword = data.get("currentPassword");
    String newPassword = data.get("newPassword");

    if (currentPassword == null || currentPassword.isBlank() ||
        newPassword == null || newPassword.isBlank()) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Please fill in all password fields."));
    }

    if (newPassword.length() < 8) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "New password must be at least 8 characters."));
    }

    if (currentPassword.equals(newPassword)) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "New password must be different."));
    }

    Integer validPassword = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM students WHERE id = ? AND password = crypt(?, password)",
        Integer.class, studentId, currentPassword
    );

    if (validPassword == null || validPassword == 0) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Current password is incorrect."));
    }

    jdbcTemplate.update(
        "UPDATE students SET password = crypt(?, gen_salt('bf')) WHERE id = ?",
        newPassword, studentId
    );

    return ResponseEntity.ok(
        Map.of("message", "Password updated successfully!")
    );
}

@PutMapping("/api/student/photo")
public ResponseEntity<Map<String, String>> updatePhoto(
        @RequestBody Map<String, String> data,
        HttpSession session) {

    Long studentId = (Long) session.getAttribute("studentId");

    if (studentId == null) {
        return ResponseEntity.status(401)
            .body(Map.of("message", "Please log in first."));
    }

    String photoData = data.get("photo");
    String photoType = data.get("photoType");
        if (!"image/jpeg".equals(photoType) &&
        !"image/png".equals(photoType) &&
        !"image/webp".equals(photoType) &&
        !"image/gif".equals(photoType)) {
        return ResponseEntity.badRequest()
        .body(Map.of("message", "Unsupported image format."));
}

    if (photoData == null || !photoData.startsWith("data:image/")) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Invalid image."));
    }

    try {
        String[] parts = photoData.split(",", 2);
        byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

        if (imageBytes.length > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Image must be under 2 MB."));
        }

        jdbcTemplate.update(
            "UPDATE students SET profile_photo = ?, profile_photo_type = ? WHERE id = ?",
            imageBytes, photoType, studentId
        );

        return ResponseEntity.ok(
            Map.of("message", "Profile picture saved successfully!")
        );

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Invalid image data."));
    }
}

@GetMapping("/api/student/photo")
public ResponseEntity<Map<String, String>> getPhoto(HttpSession session) {

    Long studentId = (Long) session.getAttribute("studentId");

    if (studentId == null) {
        return ResponseEntity.status(401)
            .body(Map.of("message", "Please log in first."));
    }

    byte[] photo = jdbcTemplate.queryForObject(
        "SELECT profile_photo FROM students WHERE id = ?",
        byte[].class,
        studentId
    );

    if (photo == null) {
        return ResponseEntity.ok(Map.of("photo", ""));
    }

    String photoType = jdbcTemplate.queryForObject("SELECT profile_photo_type FROM students WHERE id = ?",
        String.class,
        studentId
    );

    String encodedPhoto = "data:" + photoType + ";base64," + Base64.getEncoder().encodeToString(photo);
    return ResponseEntity.ok(Map.of("photo", encodedPhoto));
}
}
