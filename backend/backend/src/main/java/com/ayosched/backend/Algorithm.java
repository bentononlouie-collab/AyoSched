package com.ayosched.backend;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Algorithm{
    
    public static class ScheduleEntry{
        public Long id;
        public String subjectName;
        public String day; 
        public LocalTime startTime;
        public LocalTime endTime;
        public String scheduleType;

        public ScheduleEntry (Long id, String subjectName, String day, LocalTime startTime, LocalTime endTime, String scheduleType ){
            this.id = id;
            this.subjectName = subjectName;
            this.day = day;
            this.startTime = startTime;
            this.endTime = endTime;
            this.scheduleType = scheduleType;
        }
    }

    public static class ConflictPair{
        public ScheduleEntry firstSchedule;
        public ScheduleEntry secondSchedule;

        public ConflictPair(ScheduleEntry first, ScheduleEntry second){
            this.firstSchedule = first;
            this.secondSchedule = second;
        }
    }

    public static class AllocationResult{
        public List<ScheduleEntry>allocatedSchedules = new ArrayList<>();
        public List<ScheduleEntry>conflictingSchedules = new ArrayList<>();
    }

    public AllocationResult allocateGreedy(List<ScheduleEntry> schedules) {
        AllocationResult result = new AllocationResult();
            List<ScheduleEntry> sorted = new ArrayList<>(schedules);
                sorted.sort((a, b) -> {
                int priorityA = computePriority(
                a.scheduleType, a.startTime.toString());
                int priorityB = computePriority(
                b.scheduleType, b.startTime.toString());
                return Integer.compare(priorityB, priorityA);
            });

    for (ScheduleEntry current : sorted) {

        boolean conflict = false;

        for (ScheduleEntry allocated : result.allocatedSchedules) {

            if (hasConflict(
                current.day,
                current.startTime,
                current.endTime,
                allocated.day,
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
        String day1,
        LocalTime start1,
        LocalTime end1,
        String day2,
        LocalTime start2,
        LocalTime end2
    ){
        if(!day1.equalsIgnoreCase(day2)){
            return false;
        }

        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public List<ConflictPair>findConflicts(List<ScheduleEntry>schedules){
        List<ConflictPair> conflicts = new ArrayList<>();
            for(int i = 0; i<schedules.size(); i++){
                for(int j = i + 1; j<schedules.size(); j++){
                    ScheduleEntry first = schedules.get(i);
                    ScheduleEntry second = schedules.get(j);

                        if(hasConflict(first.day, first.startTime, first.endTime, second.day, second.startTime, second.endTime)){
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

