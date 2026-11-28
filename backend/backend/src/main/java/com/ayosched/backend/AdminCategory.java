package com.ayosched.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true")
public class AdminCategory {

    private final JdbcTemplate jdbcTemplate;

    public AdminCategory(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init(){
        try{
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS categories (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL UNIQUE, " +
                "type VARCHAR(20) NOT NULL DEFAULT 'personal', " +
                "color VARCHAR(20) DEFAULT '#6366f1', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS priorities (" +
                "id SERIAL PRIMARY KEY, " +
                "schedule_type VARCHAR(100) NOT NULL UNIQUE, " +
                "priority_level INTEGER NOT NULL DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            jdbcTemplate.execute(
                "INSERT INTO categories(name, type, color) VALUES " +
                "('Educational', 'educational', '#3b82f6'), " +
                "('Personal', 'personal', '#10b981') " +
                "ON CONFLICT (name) DO NOTHING");
            jdbcTemplate.execute(
                "INSERT INTO priorities(schedule_type, priority_level) VALUES " +
                "('lecture', 4), " +
                "('exam', 5), " +
                "('project', 5), " +
                "('assignment', 4), " +
                "('task', 2), " +
                "('personal', 1) " +
                "ON CONFLICT (schedule_type) DO NOTHING");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @GetMapping
    public List<Map<String, Object>> getCategories(){
        return jdbcTemplate.queryForList(
            "SELECT id, name, type, color FROM categories ORDER BY id");
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createCategory(@RequestBody Map<String, String> data){
        String name = data.get("name");
        String type = data.get("type");
        String color = data.get("color");

        if(name == null || name.isBlank() || type == null || type.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message", "Name and type are required."));
        }

        if(color == null || color.isBlank()){
            color = "#6366f1";
        }

        try{
            jdbcTemplate.update(
                "INSERT INTO categories(name, type, color) VALUES (?, ?, ?)",
                name.trim(), type.trim(), color);
            return ResponseEntity.ok(Map.of("message", "Category created successfully!"));
        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to create category."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateCategory(@PathVariable int id, @RequestBody Map<String, String> data){
        String name = data.get("name");
        String type = data.get("type");
        String color = data.get("color");

        if(name == null || name.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required."));
        }

        try{
            jdbcTemplate.update(
                "UPDATE categories SET name = ?, type = ?, color = ? WHERE id = ?",
                name.trim(), type != null ? type.trim() : "personal", color != null ? color : "#6366f1", id);
            return ResponseEntity.ok(Map.of("message", "Category updated successfully!"));
        }catch(Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to update category."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable int id){
        int deleted = jdbcTemplate.update("DELETE FROM categories WHERE id = ?", id);
        if(deleted == 0){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully."));
    }
}