package com.ayosched.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import jakarta.annotation.PostConstruct;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/schedule-times")
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true")
public class AdminScheduleTime {

    private final JdbcTemplate jdbcTemplate;

    public AdminScheduleTime(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init(){
        try{
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS schedule_times (" +
                "id SERIAL PRIMARY KEY, " +
                "category VARCHAR(50) NOT NULL, " +
                "label VARCHAR(100) NOT NULL, " +
                "start_time TIME NOT NULL, " +
                "end_time TIME NOT NULL, " +
                "is_default BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            // Default educational times
            jdbcTemplate.execute(
                "INSERT INTO schedule_times(category, label, start_time, end_time, is_default) VALUES " +
                "('educational', 'Morning Lecture', '08:00', '09:00', true), " +
                "('educational', 'Midday Lecture', '10:00', '11:00', true), " +
                "('educational', 'Afternoon Lecture', '13:00', '14:00', true), " +
                "('personal', 'Morning Task', '07:00', '08:00', true), " +
                "('personal', 'Evening Task', '17:00', '18:00', true) " +
                "ON CONFLICT DO NOTHING");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @GetMapping
    public List<Map<String, Object>> getScheduleTimes(){
        return jdbcTemplate.queryForList(
            "SELECT id, category, label, start_time, end_time, is_default FROM schedule_times ORDER BY category, start_time");
    }

    @GetMapping("/{category}")
    public List<Map<String, Object>> getScheduleTimesByCategory(@PathVariable String category){
        return jdbcTemplate.queryForList(
            "SELECT id, category, label, start_time, end_time, is_default FROM schedule_times WHERE LOWER(category) = LOWER(?) ORDER BY start_time",
            category);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createScheduleTime(@RequestBody Map<String, String> data){
        String category = data.get("category");
        String label = data.get("label");
        String startTime = data.get("startTime");
        String endTime = data.get("endTime");
        String isDefault = data.get("isDefault");

        if(category == null || label == null || startTime == null || endTime == null ||
           category.isBlank() || label.isBlank() || startTime.isBlank() || endTime.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required."));
        }

        try{
            jdbcTemplate.update(
                "INSERT INTO schedule_times(category, label, start_time, end_time, is_default) VALUES (?, ?, CAST(? AS TIME), CAST(? AS TIME), ?)",
                category.trim(), label.trim(), startTime, endTime,
                "true".equalsIgnoreCase(isDefault));
            return ResponseEntity.ok(Map.of("message", "Schedule time created successfully!"));
        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to create schedule time."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateScheduleTime(@PathVariable int id, @RequestBody Map<String, String> data){
        String label = data.get("label");
        String startTime = data.get("startTime");
        String endTime = data.get("endTime");

        if(label == null || label.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message", "Label is required."));
        }

        try{
            jdbcTemplate.update(
                "UPDATE schedule_times SET label = ?, start_time = CAST(? AS TIME), end_time = CAST(? AS TIME) WHERE id = ?",
                label.trim(), startTime, endTime, id);
            return ResponseEntity.ok(Map.of("message", "Schedule time updated successfully!"));
        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to update schedule time."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteScheduleTime(@PathVariable int id){
        int deleted = jdbcTemplate.update("DELETE FROM schedule_times WHERE id = ?", id);
        if(deleted == 0){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Schedule time deleted successfully."));
    }
}