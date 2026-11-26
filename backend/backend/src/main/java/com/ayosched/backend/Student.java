package com.ayosched.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;


@RestController 
@CrossOrigin(origins = "*")
public class Student {
    
    private final JdbcTemplate jdbcTemplate;

    public Student(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/api/student/signup")
    public ResponseEntity<Map<String, String>> studentSignup(@RequestBody Map<String, String>student) {
        String fullName = student.get("fullName");
        String email = student.get("email");
        String password = student.get("password");

            if(fullName == null || email == null || password == null || fullName.isBlank() || email.isBlank() || password.isBlank()){
                return ResponseEntity.badRequest().body(Map.of("message", "Please complete all fields."));
            }

            if(password.length() < 8){
                return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 8 characters."));
            }

            Integer existingEmail = jdbcTemplate.queryForObject(
                "SELECT COUNT (*) FROM students WHERE email =? ",
                Integer.class,
                email
            );

            if(existingEmail != null && existingEmail > 0){
                return ResponseEntity.badRequest().body(Map.of("message", "Email is already registered."));
            }

            jdbcTemplate.update(
                "INSERT INTO students(full_name, email, password)" +
                "VALUES (?, ?, crypt(?, gen_salt('bf', 12)))",
                fullName, email, password
            );
        
        return ResponseEntity.ok(Map.of("message", "Student account created successfully!"));
    }
}
