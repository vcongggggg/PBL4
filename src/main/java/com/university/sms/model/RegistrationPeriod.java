package com.university.sms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model cho thời gian đăng ký tín chỉ
 */
public class RegistrationPeriod implements Serializable {
    private static final long serialVersionUID = 1L;

    private int periodId;
    private String academicYear;
    private int semester;
    private Timestamp startDate;
    private Timestamp endDate;
    private PeriodStatus status;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer createdBy; // Admin user_id
    private Integer closedBy; // Admin user_id who closed the period

    public enum PeriodStatus {
        DRAFT,      // Chưa mở
        OPEN,       // Đang mở đăng ký
        CLOSED,     // Đã đóng đăng ký
        PROCESSING, // Đang xử lý tự động duyệt
        COMPLETED   // Đã hoàn tất
    }

    // Constructors
    public RegistrationPeriod() {
        this.status = PeriodStatus.DRAFT;
    }

    public RegistrationPeriod(String academicYear, int semester, Timestamp startDate, Timestamp endDate) {
        this();
        this.academicYear = academicYear;
        this.semester = semester;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(Integer closedBy) {
        this.closedBy = closedBy;
    }

    /**
     * Check if registration is currently open
     */
    public boolean isOpen() {
        if (status != PeriodStatus.OPEN) {
            return false;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.after(startDate) && now.before(endDate);
    }

    /**
     * Check if registration period has ended
     */
    public boolean hasEnded() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.after(endDate) || status == PeriodStatus.CLOSED || 
               status == PeriodStatus.PROCESSING || status == PeriodStatus.COMPLETED;
    }

    @Override
    public String toString() {
        return "RegistrationPeriod{" +
                "periodId=" + periodId +
                ", academicYear='" + academicYear + '\'' +
                ", semester=" + semester +
                ", status=" + status +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}

