package com.university.sms.model;

import java.io.Serializable;

/**
 * Model cho một entry trong thời khóa biểu
 * Được tạo từ Course và dùng để hiển thị lịch
 */
public class TimetableEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int courseId;
    private String courseCode;
    private String subjectName;
    private String teacherName;
    private String room;
    private int credits;
    
    // Schedule information
    private DayOfWeek dayOfWeek;
    private int startPeriod;  // Tiết bắt đầu (1-12)
    private int endPeriod;    // Tiết kết thúc (1-12)
    private String timeRange; // e.g., "07:00-09:00"
    
    // Visual properties
    private String color;     // Color code for display
    private String facultyName;
    
    public enum DayOfWeek {
        MONDAY("Thứ 2", 1),
        TUESDAY("Thứ 3", 2),
        WEDNESDAY("Thứ 4", 3),
        THURSDAY("Thứ 5", 4),
        FRIDAY("Thứ 6", 5),
        SATURDAY("Thứ 7", 6),
        SUNDAY("Chủ nhật", 0);
        
        private final String displayName;
        private final int dayNumber;
        
        DayOfWeek(String displayName, int dayNumber) {
            this.displayName = displayName;
            this.dayNumber = dayNumber;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getDayNumber() {
            return dayNumber;
        }
        
        public static DayOfWeek fromString(String day) {
            if (day == null || day.trim().isEmpty()) return null;
            day = day.toLowerCase().trim();
            
            // Handle Vietnamese format: "Thứ 2", "Thứ hai", etc.
            if (day.contains("2") || day.contains("hai") || day.equals("monday") || day.equals("mon")) return MONDAY;
            if (day.contains("3") || day.contains("ba") || day.equals("tuesday") || day.equals("tue")) return TUESDAY;
            if (day.contains("4") || day.contains("tư") || day.equals("wednesday") || day.equals("wed")) return WEDNESDAY;
            if (day.contains("5") || day.contains("năm") || day.equals("thursday") || day.equals("thu")) return THURSDAY;
            if (day.contains("6") || day.contains("sáu") || day.equals("friday") || day.equals("fri")) return FRIDAY;
            if (day.contains("7") || day.contains("bảy") || day.equals("saturday") || day.equals("sat")) return SATURDAY;
            if (day.contains("cn") || day.contains("chủ nhật") || day.contains("chủnhật") || day.equals("sunday") || day.equals("sun")) return SUNDAY;
            
            return null;
        }
    }
    
    // Constructors
    public TimetableEntry() {}
    
    public TimetableEntry(Course course) {
        this.courseId = course.getCourseId();
        this.courseCode = course.getCourseCode();
        this.subjectName = course.getSubjectName();
        this.teacherName = course.getTeacherName();
        this.room = course.getRoom();
        this.credits = course.getCredits();
        
        // Parse schedule
        parseSchedule(course.getScheduleDay(), course.getScheduleTime());
    }
    
    /**
     * Parse schedule từ course
     * Ví dụ: scheduleDay = "Thứ 2", scheduleTime = "Tiết 1-3 (07:00-09:30)"
     */
    private void parseSchedule(String scheduleDay, String scheduleTime) {
        // Default values
        this.startPeriod = 1;
        this.endPeriod = 1;
        
        // Parse day
        this.dayOfWeek = DayOfWeek.fromString(scheduleDay);
        
        // Parse time and periods
        if (scheduleTime != null && !scheduleTime.isEmpty()) {
            // Extract periods (e.g., "Tiết 1-3" or "1-3")
            boolean periodsParsed = false;
            
            // Try format: "Tiết 1-3 (07:00-09:30)"
            if (scheduleTime.toLowerCase().contains("tiết")) {
                String[] parts = scheduleTime.split("(?i)tiết");
                if (parts.length > 1) {
                    String periodPart = parts[1].trim().split("\\(")[0].trim();
                    String[] periods = periodPart.split("-");
                    if (periods.length == 2) {
                        try {
                            this.startPeriod = Integer.parseInt(periods[0].trim());
                            this.endPeriod = Integer.parseInt(periods[1].trim());
                            periodsParsed = true;
                        } catch (NumberFormatException e) {
                            // Ignore, will use default
                        }
                    }
                }
            }
            
            // Try format: "1-3" or "07:00-09:00"
            if (!periodsParsed) {
                // Try to extract numbers that look like periods (1-12)
                String[] parts = scheduleTime.split("-");
                if (parts.length >= 2) {
                    try {
                        int first = Integer.parseInt(parts[0].trim());
                        int second = Integer.parseInt(parts[1].trim().split("\\s")[0]);
                        if (first >= 1 && first <= 12 && second >= 1 && second <= 12) {
                            this.startPeriod = first;
                            this.endPeriod = second;
                            periodsParsed = true;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
            
            // Extract time range (e.g., "07:00-09:30")
            if (scheduleTime.contains("(") && scheduleTime.contains(")")) {
                int start = scheduleTime.indexOf("(");
                int end = scheduleTime.indexOf(")");
                this.timeRange = scheduleTime.substring(start + 1, end).trim();
            } else if (scheduleTime.matches(".*\\d{2}:\\d{2}.*")) {
                // If time is directly in the string
                this.timeRange = scheduleTime.replaceAll("[^0-9:-]", "");
            }
        }
    }
    
    /**
     * Kiểm tra xung đột lịch với entry khác
     */
    public boolean conflictsWith(TimetableEntry other) {
        if (other == null || this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        
        // Check if periods overlap
        return !(this.endPeriod < other.startPeriod || this.startPeriod > other.endPeriod);
    }
    
    /**
     * Format hiển thị cho cell trong timetable
     */
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append(subjectName != null ? subjectName : courseCode);
        if (room != null && !room.isEmpty()) {
            sb.append("\n").append(room);
        }
        if (teacherName != null && !teacherName.isEmpty()) {
            sb.append("\n").append(teacherName);
        }
        return sb.toString();
    }
    
    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }
    
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
    
    public String getSubjectName() {
        return subjectName;
    }
    
    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
    
    public String getTeacherName() {
        return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
    
    public String getRoom() {
        return room;
    }
    
    public void setRoom(String room) {
        this.room = room;
    }
    
    public int getCredits() {
        return credits;
    }
    
    public void setCredits(int credits) {
        this.credits = credits;
    }
    
    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public int getStartPeriod() {
        return startPeriod;
    }
    
    public void setStartPeriod(int startPeriod) {
        this.startPeriod = startPeriod;
    }
    
    public int getEndPeriod() {
        return endPeriod;
    }
    
    public void setEndPeriod(int endPeriod) {
        this.endPeriod = endPeriod;
    }
    
    public String getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getFacultyName() {
        return facultyName;
    }
    
    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s - %s (Tiết %d-%d)", 
            dayOfWeek != null ? dayOfWeek.getDisplayName() : "", 
            subjectName, room, startPeriod, endPeriod);
    }
}

