package com.ayosched.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/priorities")
public class Priority {
 
    private final JdbcTemplate jdbcTemplate;

    public Priority(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> savePriority(
            @RequestBody Map<String, Object> data) {
        
        String category = (String) data.get("category");
        String scheduleType = (String) data.get("scheduleType");
        Object levelObject = data.get("priorityLevel");

        if (category == null || (!category.equals("educational") && !category.equals("personal")) || scheduleType == null || scheduleType.isBlank()
                || levelObject == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please complete all fields."));
        }

        int priorityLevel;

        try {
            priorityLevel = Integer.parseInt(levelObject.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Priority must be a number."));
        }

        if (priorityLevel < 1 || priorityLevel > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Priority must be between 1 and 5."));
        }

        jdbcTemplate.update(
            "INSERT INTO priorities (category, schedule_type, priority_level) " +
            "VALUES (?, ?, ?) ON CONFLICT (schedule_type) " +
            "DO UPDATE SET category = EXCLUDED.category, " + 
            "priority_level = EXCLUDED.priority_level",
            category, scheduleType.trim(), priorityLevel
        );

        return ResponseEntity.ok(
            Map.of("message", "Priority saved successfully!")
        );
    }

    @GetMapping
    public List<Map<String, Object>> getPriorities() {
        return jdbcTemplate.queryForList(
            "SELECT id, category, schedule_type, priority_level " +
            "FROM priorities ORDER BY id"
        );
    }

    @PutMapping("/{id}")
public ResponseEntity<Map<String, String>> updatePriority(
        @PathVariable int id,
        @RequestBody Map<String, Object> data) {

    String category = (String) data.get("category");
    String scheduleType = (String) data.get("scheduleType");
    Object levelObject = data.get("priorityLevel");

    if (category == null ||
            (!category.equals("educational") && !category.equals("personal")) ||
            scheduleType == null || scheduleType.isBlank() ||
            levelObject == null) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Please complete all fields."));
    }

    int priorityLevel;

    try {
        priorityLevel = Integer.parseInt(levelObject.toString());
    } catch (NumberFormatException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Priority must be a number."));
    }

    if (priorityLevel < 1 || priorityLevel > 5) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Priority must be between 1 and 5."));
    }

    int updated = jdbcTemplate.update(
        "UPDATE priorities SET category = ?, schedule_type = ?, priority_level = ? WHERE id = ?",
        category, scheduleType.trim(), priorityLevel, id
    );

    if (updated == 0) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(
        Map.of("message", "Priority updated successfully!")
    );
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePriority(
            @PathVariable int id) {

        int deleted = jdbcTemplate.update(
            "DELETE FROM priorities WHERE id = ?", id
        );

        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
            Map.of("message", "Priority deleted successfully!")
        );
    }
}
