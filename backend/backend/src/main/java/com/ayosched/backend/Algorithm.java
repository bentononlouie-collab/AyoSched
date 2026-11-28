package com.ayosched.backend;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;

public class Algorithm{

    public static class ScheduleEntry{
        public Long id;
        public String subjectName;
        public String day;
        public LocalDate scheduleDate;
        public LocalTime startTime;
        public LocalTime endTime;
        public String scheduleType;
        public String category;
        public int priority;
        public int priorityRank;

        public ScheduleEntry (Long id, String subjectName, String day, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, String scheduleType, String category ){
            this.id = id;
            this.subjectName = subjectName;
            this.day = day;
            this.scheduleDate = scheduleDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.scheduleType = scheduleType;
            this.category = category;
            this.priority = 1;
            this.priorityRank = 0;
        }
    }

    public static class ConflictPair{
        public ScheduleEntry firstSchedule;
        public ScheduleEntry secondSchedule;
        public int firstPriority;
        public int secondPriority;
        public String higherPriority;
        public String lowerPriority;

        public ConflictPair(ScheduleEntry first, ScheduleEntry second){
            this.firstSchedule = first;
            this.secondSchedule = second;
            this.firstPriority = first.priority;
            this.secondPriority = second.priority;
            if(first.priority >= second.priority){
                this.higherPriority = first.subjectName;
                this.lowerPriority = second.subjectName;
            } else {
                this.higherPriority = second.subjectName;
                this.lowerPriority = first.subjectName;
            }
        }
    }

    public static class AllocationResult{
        public List<ScheduleEntry>allocatedSchedules = new ArrayList<>();
        public List<ScheduleEntry>conflictingSchedules = new ArrayList<>();
    }

    public AllocationResult allocateGreedy(List<ScheduleEntry> schedules, Map<String, Integer> priorityMap){
        AllocationResult result = new AllocationResult();
        List<ScheduleEntry> sorted = new ArrayList<>(schedules);
        sorted.removeIf(s -> s.scheduleDate == null);

        // Assign priority and rank from DB map
        for(ScheduleEntry s : sorted){
            String key = (s.category == null ? "" : s.category.trim().toLowerCase()) + "|" + (s.scheduleType == null ? "" : s.scheduleType.trim().toLowerCase());
            s.priority = priorityMap != null && priorityMap.containsKey(key)
                ? priorityMap.get(key)
                : computePriority(s.scheduleType, s.startTime.toString());
        }

        // Sort by priority descending (higher priority first), then by start time
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.priority, a.priority);
            if(cmp != 0) return cmp;
            return a.startTime.compareTo(b.startTime);
        });

        // Assign priority rank (1 = highest)
        int rank = 1;
        for(ScheduleEntry s : sorted){
            s.priorityRank = rank++;
        }

        for (ScheduleEntry current : sorted) {
            boolean conflict = false;

            for (ScheduleEntry allocated : result.allocatedSchedules) {

                if (hasConflict(
                    current.scheduleDate,
                    current.startTime,
                    current.endTime,
                    allocated.scheduleDate,
                    allocated.startTime,
                    allocated.endTime
                )) {
                    conflict = true;
                    break;
                }
            }

            if (!conflict) {
                result.allocatedSchedules.add(current);
            } else {
                result.conflictingSchedules.add(current);
            }
        }

        return result;
    }

    public boolean hasConflict(
    LocalDate date1,
    LocalTime start1,
    LocalTime end1,
    LocalDate date2,
    LocalTime start2,
    LocalTime end2
){
    if(date1 == null || date2 == null || !date1.equals(date2)){
        return false;
    }

    return start1.isBefore(end2) && start2.isBefore(end1);
}

    public List<ConflictPair>findConflicts(List<ScheduleEntry>schedules, Map<String, Integer> priorityMap){
        List<ConflictPair> conflicts = new ArrayList<>();
        schedules = new ArrayList<>(schedules);
        schedules.removeIf(s -> s.scheduleDate == null);
        System.out.println("SCHEDULES AFTER NULL DATE FILTER: " + schedules.size());

for(ScheduleEntry s : schedules){
    System.out.println(
        s.id + " | " + s.subjectName + " | " +
        s.scheduleDate + " | " + s.startTime + " - " + s.endTime
    );
}

        // Assign priorities
        for(ScheduleEntry s : schedules){
            String key = (s.category == null ? "" : s.category.trim().toLowerCase()) + "|" + (s.scheduleType == null ? "" : s.scheduleType.trim().toLowerCase());
            s.priority = priorityMap != null && priorityMap.containsKey(key)
                ? priorityMap.get(key)
                : computePriority(s.scheduleType, s.startTime.toString());
        }

        // Sort by priority descending for consistent ranking
        List<ScheduleEntry> sorted = new ArrayList<>(schedules);
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.priority, a.priority);
            if(cmp != 0) return cmp;
            return a.startTime.compareTo(b.startTime);
        });

        // Assign ranks
        int rank = 1;
        for(ScheduleEntry s : sorted){
            s.priorityRank = rank;
            rank++;
        }

        for(int i = 0; i < sorted.size(); i++){
    for(int j = i + 1; j < sorted.size(); j++){
        ScheduleEntry first = sorted.get(i);
        ScheduleEntry second = sorted.get(j);

        if(hasConflict(
            first.scheduleDate, first.startTime, first.endTime,
            second.scheduleDate, second.startTime, second.endTime
        )){
            conflicts.add(new ConflictPair(first, second));
        }
    }
}
        return conflicts;
    }

    public int computePriority(String scheduleType, String startTime){
        String type = scheduleType == null ? "" : scheduleType.trim().toLowerCase();

        if(type.contains("exam") || type.contains("project") || type.contains("assessment") || type.contains("assignment")){
            return 5;
        }

        if(type.contains("lecture")){
            return 4;
        }

        return 1;
    }
}

