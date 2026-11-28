package com.ayosched.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.HashMap;
import jakarta.annotation.PostConstruct;


@RestController
public class Schedule {
    private final JdbcTemplate jdbcTemplate;

    public Schedule(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
public void init(){
    try{
        jdbcTemplate.execute("ALTER TABLE schedules ADD COLUMN IF NOT EXISTS category VARCHAR(30) DEFAULT 'educational'");
        jdbcTemplate.execute("ALTER TABLE schedules ADD COLUMN IF NOT EXISTS description TEXT");
        jdbcTemplate.execute("ALTER TABLE schedules ADD COLUMN IF NOT EXISTS schedule_date DATE");
    }catch(Exception e){
        e.printStackTrace();
    }
}

    private Map<String, Integer> loadPriorityMap(){
        Map<String, Integer> map = new HashMap<>();

    try{
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT category, schedule_type, priority_level FROM priorities"
        );

        for(Map<String, Object> row : rows){
            String category = (String) row.get("category");
            String type = (String) row.get("schedule_type");
            Number level = (Number) row.get("priority_level");

            if(category != null && type != null && level != null){
                String key = category.toLowerCase().trim() + "|" +
                             type.toLowerCase().trim();

                map.put(key, level.intValue());
            }
        }

    }catch(Exception e){
        e.printStackTrace();
    }

    return map;
}
    

    @PostMapping("/api/student/schedules")
    public ResponseEntity<Map<String, String>> addSchedule(@RequestBody Map<String, String>schedule){
        String studentId = schedule.get("studentId");
        String subjectName = schedule.get("subjectName");
        String instructor = schedule.get("instructor");
        String location = schedule.get("location");
        String day = schedule.get("day");
        String scheduleDate = schedule.get("scheduleDate");
        String scheduleType = schedule.get("scheduleType");
        String startTime = schedule.get("startTime");
        System.out.println("TYPE =[" + scheduleType + "] START =[" + startTime + "]");
        String endTime = schedule.get("endTime");
        String notes = schedule.get("notes");
        String description = schedule.get("description");
        String category = schedule.get("category");
        if(category == null || category.isBlank()){
            category = "educational";
        }

        boolean isEducational = category.equalsIgnoreCase("educational");

        if(studentId == null || subjectName == null || day == null || scheduleDate == null || startTime == null || endTime == null ||
           studentId.isBlank() || subjectName.isBlank() || day.isBlank() || scheduleDate.isBlank() || startTime.isBlank() || endTime.isBlank()
        ){
            return ResponseEntity.badRequest().body(Map.of("message", "Please complete all required fields."));
        }

        if(isEducational){
            if(instructor == null || location == null || scheduleType == null ||
               instructor.isBlank() || location.isBlank() || scheduleType.isBlank()
            ){
                return ResponseEntity.badRequest().body(Map.of("message", "Please complete all required fields."));
            }
        }else{
            instructor = instructor == null ? "" : instructor;
            location = location == null ? "" : location;
            scheduleType = scheduleType == null ? "" : scheduleType;
        }

        Algorithm algorithm = new Algorithm();
        Map<String, Integer> priorityMap = loadPriorityMap();
            String priorityKey = category.toLowerCase().trim() + "|" + scheduleType.toLowerCase();
                int priority = priorityMap.containsKey(priorityKey)
                    ? priorityMap.get(priorityKey)
                    : algorithm.computePriority(scheduleType, startTime);

        String sql = "INSERT INTO schedules " +
    "(student_id, subject_name, instructor, location, day, schedule_date, schedule_type, start_time, end_time, notes, priority, category, description) " +
    "VALUES (?, ?, ?, ?, ?, CAST(? AS DATE), ?, CAST(? AS TIME), CAST(? AS TIME), ?, ?, ?, ?)";
            try{
                jdbcTemplate.update(
                    sql,
                    Long.parseLong(studentId),subjectName,instructor,location,day,scheduleDate,scheduleType,startTime,endTime,notes == null ? "" : notes, priority, category, description == null ? "" : description
                );

                    return ResponseEntity.ok(Map.of("message", "Schedule added successfully!"));
                }catch(Exception e){
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(Map.of("message", "Unable to add schedule."));
                }
    } 
    @GetMapping("/api/student/schedules/conflicts")
    public ResponseEntity<?>checkConflicts(@RequestParam Long studentId){
        String sql = "SELECT id, subject_name, day, schedule_date, start_time, end_time, schedule_type, category " + "FROM schedules WHERE student_id = ? ORDER BY id ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, studentId);
        List<Algorithm.ScheduleEntry>schedules = new ArrayList <>();

        Map<String, Integer> priorityMap = loadPriorityMap();

            for(Map<String, Object> row : rows){

    Object dateValue = row.get("schedule_date");
    LocalDate scheduleDate = null;

    if(dateValue instanceof java.sql.Date){
        scheduleDate = ((java.sql.Date) dateValue).toLocalDate();
    } else if(dateValue instanceof LocalDate){
        scheduleDate = (LocalDate) dateValue;
    }

    schedules.add(new Algorithm.ScheduleEntry(
        ((Number) row.get("id")).longValue(),
        (String) row.get("subject_name"),
        (String) row.get("day"),
        scheduleDate,
        ((java.sql.Time) row.get("start_time")).toLocalTime(),
        ((java.sql.Time) row.get("end_time")).toLocalTime(),
        (String) row.get("schedule_type"),
        (String) row.get("category")
    ));
}

            Algorithm algorithm = new Algorithm();
            Algorithm.AllocationResult allocation = algorithm.allocateGreedy(schedules, priorityMap);
            List<Long> recommendedScheduleIds = new ArrayList<>();
                for(Algorithm.ScheduleEntry schedule : allocation.conflictingSchedules){
                    recommendedScheduleIds.add(schedule.id);
                }
            List<Algorithm.ConflictPair>conflicts = algorithm.findConflicts(schedules, priorityMap);
            // Build conflict list with priority info
            List<Map<String, Object>> conflictDetails = new ArrayList<>();
            for(Algorithm.ConflictPair cp : conflicts){
                Map<String, Object> detail = new HashMap<>();
                detail.put("firstScheduleId", cp.firstSchedule.id);
                detail.put("firstSubjectName", cp.firstSchedule.subjectName);
                detail.put("firstPriority", cp.firstSchedule.priority);
                detail.put("firstPriorityRank", cp.firstSchedule.priorityRank);
                detail.put("secondScheduleId", cp.secondSchedule.id);
                detail.put("secondSubjectName", cp.secondSchedule.subjectName);
                detail.put("secondPriority", cp.secondSchedule.priority);
                detail.put("secondPriorityRank", cp.secondSchedule.priorityRank);
                detail.put("higherPriority", cp.higherPriority);
                detail.put("lowerPriority", cp.lowerPriority);
                conflictDetails.add(detail);
            }
            Map<String, Object> response = new HashMap<>();
                response.put("conflicts", conflictDetails);
                response.put("recommendedScheduleIds", recommendedScheduleIds);
            return ResponseEntity.ok(response);
    }

    @GetMapping("/api/student/schedules")
public List<Map<String, Object>> getSchedules(@RequestParam Long studentId) {

    String sql = "SELECT id, subject_name AS \"subjectName\", instructor, location, day, " +
            "schedule_date AS \"scheduleDate\", " + 
            "schedule_type AS \"scheduleType\", " +
            "start_time AS \"startTime\", end_time AS \"endTime\", notes, priority, category, description, " +
            "false AS \"mutedToday\" " +
            "FROM schedules WHERE student_id = ? AND schedule_date >= CURRENT_DATE " + "ORDER BY schedule_date, start_time";

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, studentId);

    List<Algorithm.ScheduleEntry> schedules = new ArrayList<>();

    for(Map<String, Object> row : rows){
        java.sql.Date sqlDate = (java.sql.Date) row.get("schedule_date");
        LocalDate scheduleDate = sqlDate == null ? null : sqlDate.toLocalDate();
        schedules.add(new Algorithm.ScheduleEntry(
            ((Number) row.get("id")).longValue(),
            (String) row.get("subjectName"),
            (String) row.get("day"),
            scheduleDate,
            ((java.sql.Time) row.get("startTime")).toLocalTime(),
            ((java.sql.Time) row.get("endTime")).toLocalTime(),
            (String) row.get("scheduleType"),
            (String) row.get("category")
        ));
    }

    Algorithm algorithm = new Algorithm();
    algorithm.findConflicts(schedules, loadPriorityMap());

    Map<Long, Algorithm.ScheduleEntry> scheduleMap = new HashMap<>();

    for(Algorithm.ScheduleEntry s : schedules){
        scheduleMap.put(s.id, s);
    }

    for(Map<String, Object> row : rows){

        Long id = ((Number) row.get("id")).longValue();
        Algorithm.ScheduleEntry s = scheduleMap.get(id);

        if(s != null){
            row.put("priority", s.priority);
            row.put("priorityRank", s.priorityRank);
        }
    }

    return rows;
}

    @GetMapping("/api/student/schedules/upcoming")
public List<Map<String, Object>> getUpcoming(
        @RequestParam Long studentId,
        @RequestParam(defaultValue = "30") int minutesAhead) {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime windowEnd = now.plusMinutes(minutesAhead);

    String sql = "SELECT id, subject_name AS \"subjectName\", description, location, day, " +
            "schedule_date AS \"scheduleDate\", " +
            "start_time AS \"startTime\", end_time AS \"endTime\", priority, category " +
            "FROM schedules " +
            "WHERE student_id = ? " +
            "AND (schedule_date + start_time) BETWEEN ? AND ? " +
            "ORDER BY schedule_date, start_time";

    return jdbcTemplate.queryForList(sql, studentId, now, windowEnd);
}

    @GetMapping("/api/student/schedules/free-slots")
    public List<Map<String, String>> freeSlots(@RequestParam Long studentId,
           @RequestParam String day, @RequestParam long minutes, @RequestParam Long excludeId){
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT start_time, end_time FROM schedules " +
                "WHERE student_id = ? AND LOWER(day) = LOWER(?) AND id <> ? ORDER BY start_time",
                studentId, day, excludeId);

        LocalTime cursor = LocalTime.of(7, 0);
        LocalTime dayEnd = LocalTime.of(20, 0);

        List<Map<String, String>> slots = new ArrayList<>();

            for(Map<String, Object> r : rows){
                LocalTime s = ((java.sql.Time) r.get("start_time")).toLocalTime();
                LocalTime e = ((java.sql.Time) r.get("end_time")).toLocalTime();

                if(Duration.between(cursor, s).toMinutes() >= minutes){
                    slots.add(Map.of("start", cursor.toString(), "end", cursor.plusMinutes(minutes).toString()));
                }

                if(e.isAfter(cursor)) cursor = e;
            }

            if(Duration.between(cursor, dayEnd).toMinutes() >= minutes){
                slots.add(Map.of("start", cursor.toString(), "end", cursor.plusMinutes(minutes).toString()));
            }

            return slots.size() > 3 ? slots.subList(0, 3) : slots;
           };

    @PutMapping("/api/student/schedules/{id}/time")
    public ResponseEntity<Map<String, String>> moveSchedule(@PathVariable Long id, @RequestBody Map<String, String> body){
        jdbcTemplate.update("UPDATE schedules SET start_time = ?, end_time = ? WHERE id = ?", java.sql.Time.valueOf(LocalTime.parse(body.get("startTime"))), java.sql.Time.valueOf(LocalTime.parse(body.get("endTime"))), id);
        return ResponseEntity.ok(Map.of("message", "Schedule updated"));
    }

    @PostMapping("/api/student/schedules/{scheduleId}/mute")
    public ResponseEntity<?> muteSchedule(@PathVariable Long scheduleId, @RequestParam Long studentId) {
        String checksql = "SELECT COUNT (*) FROM schedules WHERE id = ? AND student_id = ?";
        Integer count = jdbcTemplate.queryForObject(checksql, Integer.class, scheduleId, studentId);
        if (count == null || count == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Schedule not found."));
        }
        return ResponseEntity.ok(Map.of("message", "Schedule muted for today (feature requires schedule_mutes table)."));
    }

    @DeleteMapping("/api/student/schedules/{scheduleId}")
    public ResponseEntity<?>deleteSchedule(@PathVariable Long scheduleId, @RequestParam Long studentId){
        String sql = "DELETE FROM schedules WHERE id = ? AND student_id = ?";
        int deleted = jdbcTemplate.update(sql, scheduleId, studentId);
            if(deleted == 0){
                return ResponseEntity.badRequest().body(Map.of("message", "Schedule not found."));
            }
        return ResponseEntity.ok(Map.of("message", "Schedule deleted successfully."));
    }
}
