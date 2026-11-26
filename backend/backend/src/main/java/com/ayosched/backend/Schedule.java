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
import java.time.ZoneId;
import java.util.Locale;
import java.util.HashMap;


@RestController 
@CrossOrigin (origins = "*")
public class Schedule {
    private final JdbcTemplate jdbcTemplate;

    public Schedule(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/api/student/schedules")
    public ResponseEntity<Map<String, String>> addSchedule(@RequestBody Map<String, String>schedule){
        String studentId = schedule.get("studentId");
        String subjectName = schedule.get("subjectName");
        String instructor = schedule.get("instructor");
        String location = schedule.get("location");
        String day = schedule.get("day");
        String scheduleType = schedule.get("scheduleType");
        String startTime = schedule.get("startTime");
        System.out.println("TYPE =[" + scheduleType + "] START =[" + startTime + "]");
        String endTime = schedule.get("endTime");
        String notes = schedule.get("notes");

        Algorithm algorithm = new Algorithm();
        int priority = algorithm.computePriority(scheduleType, startTime);

            if(studentId == null || subjectName == null || instructor == null || location == null || day == null || scheduleType == null || startTime == null || endTime == null || 
               studentId.isBlank() || subjectName.isBlank() || instructor.isBlank() || location.isBlank() ||  day.isBlank() || scheduleType.isBlank() || startTime.isBlank() || endTime.isBlank()
            ){
                return ResponseEntity.badRequest().body(Map.of("message", "Please complete all required fields."));
            }
            String sql = "INSERT INTO schedules" + "(student_id, subject_name, instructor, location, " + "day, schedule_type, start_time, end_time, notes, priority)" + "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS TIME), CAST(? AS TIME), ?, ?)";
                try{
                    jdbcTemplate.update(
                        sql,
                        Long.parseLong(studentId),subjectName,instructor,location,day,scheduleType,startTime,endTime,notes == null ? "" : notes, priority
                    );

                    return ResponseEntity.ok(Map.of("message", "Schedule added successfully!"));
                }catch(Exception e){
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(Map.of("message", "Unable to add schedule."));
                }
    } 
    @GetMapping("/api/student/schedules/conflicts")
    public ResponseEntity<?>checkConflicts(@RequestParam Long studentId){
        String sql = "SELECT id, subject_name, day, start_time, end_time, schedule_type " + "FROM schedules WHERE student_id = ? ORDER BY id ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, studentId);
        List<Algorithm.ScheduleEntry>schedules = new ArrayList <>();

            for(Map<String, Object>row : rows){
                schedules.add(new Algorithm.ScheduleEntry(((Number)row.get("id")).longValue(),
                (String)row.get("subject_name"), 
                (String)row.get("day"),
                ((java.sql.Time)row.get("start_time")).toLocalTime(),
                ((java.sql.Time)row.get("end_time")).toLocalTime(),
                (String)row.get("schedule_type")));
            }

            Algorithm algorithm = new Algorithm();
            Algorithm.AllocationResult allocation = algorithm.allocateGreedy(schedules);
            List<Long> recommendedScheduleIds = new ArrayList<>();
                for(Algorithm.ScheduleEntry schedule : allocation.conflictingSchedules){
                    recommendedScheduleIds.add(schedule.id);
                }
            List<Algorithm.ConflictPair>conflicts = algorithm.findConflicts(schedules);
            Map<String, Object> response = new HashMap<>();
                response.put("conflicts", conflicts);
                response.put("recommendedScheduleIds", recommendedScheduleIds);
            return ResponseEntity.ok(response);
    }

    @GetMapping("/api/student/schedules")
    public List<Map<String, Object>>getSchedules(@RequestParam Long studentId){
        String sql = "SELECT id, " + "subject_name AS \"subjectName\", " + "instructor, location, day, " + "schedule_type AS \"scheduleType\" ," + "start_time AS \"startTime\", " + "end_time AS \"endTime\", notes, priority, " + "EXISTS (" + "SELECT 1 FROM schedule_mutes sm " + "WHERE sm.schedule_id = schedules.id " + "AND sm.muted_date = CURRENT_DATE" + ") AS \"mutedToday\" " + "FROM schedules " + "WHERE student_id = ? " + "ORDER BY day, start_time";
        return jdbcTemplate.queryForList(sql, studentId);
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
    public ResponseEntity<?> muteSchedule(@PathVariable Long scheduleId, @RequestParam Long studentId){
        LocalDate todayDate = LocalDate.now(ZoneId.of("Asia/Manila"));
        String today = todayDate.getDayOfWeek().name();
        String checksql = "SELECT COUNT (*) FROM schedules " + "WHERE id = ? AND student_id = ? " + "AND UPPER(day) IN (?, ?)";
        Integer count = jdbcTemplate.queryForObject(checksql, Integer.class, scheduleId, studentId, today, today.substring(0, 3));
            if(count == null || count == 0){
                return ResponseEntity.badRequest().body(Map.of("message", "Schedule not found or not scheduled today."));
            }
        String mutesql = "INSERT INTO schedule_mutes (schedule_id, muted_date) " + "VALUES (?, ?) " + "ON CONFLICT (schedule_id, muted_date) DO NOTHING";
            jdbcTemplate.update(mutesql, scheduleId, todayDate);
        return ResponseEntity.ok(Map.of("message", "Schedule muted for today."));
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
