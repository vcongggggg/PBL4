package com.university.sms.service;

import com.university.sms.dao.SubjectDAO;
import com.university.sms.model.Subject;

import java.util.List;
import java.util.logging.Logger;

/**
 * Service class for Subject business logic
 */
public class SubjectService {
    private static final Logger LOGGER = Logger.getLogger(SubjectService.class.getName());
    private SubjectDAO subjectDAO;

    public SubjectService() {
        this.subjectDAO = new SubjectDAO();
    }

    /**
     * Get all subjects
     */
    public List<Subject> getAllSubjects() {
        return subjectDAO.findAll();
    }

    /**
     * Get subject by ID
     */
    public Subject getSubjectById(int subjectId) {
        return subjectDAO.findById(subjectId);
    }

    /**
     * Get subject by code
     */
    public Subject getSubjectByCode(String subjectCode) {
        return subjectDAO.findByCode(subjectCode);
    }

    /**
     * Get subjects by faculty
     */
    public List<Subject> getSubjectsByFaculty(int facultyId) {
        return subjectDAO.findByFaculty(facultyId);
    }

    /**
     * Search subjects
     */
    public List<Subject> searchSubjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSubjects();
        }
        return subjectDAO.search(keyword.trim());
    }

    /**
     * Add new subject
     */
    public boolean addSubject(Subject subject) {
        // Validation
        if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
            LOGGER.warning("Subject code is required");
            return false;
        }

        if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            LOGGER.warning("Subject name is required");
            return false;
        }

        if (subject.getCredits() <= 0 || subject.getCredits() > 10) {
            LOGGER.warning("Invalid credits: " + subject.getCredits());
            return false;
        }

        // Check duplicate code
        Subject existing = subjectDAO.findByCode(subject.getSubjectCode());
        if (existing != null) {
            LOGGER.warning("Subject code already exists: " + subject.getSubjectCode());
            return false;
        }

        return subjectDAO.insert(subject);
    }

    /**
     * Update subject
     */
    public boolean updateSubject(Subject subject) {
        // Validation
        if (subject.getSubjectId() <= 0) {
            LOGGER.warning("Invalid subject ID");
            return false;
        }

        Subject existing = subjectDAO.findById(subject.getSubjectId());
        if (existing == null) {
            LOGGER.warning("Subject not found: " + subject.getSubjectId());
            return false;
        }

        // Check duplicate code (excluding current subject)
        Subject duplicateCode = subjectDAO.findByCode(subject.getSubjectCode());
        if (duplicateCode != null && duplicateCode.getSubjectId() != subject.getSubjectId()) {
            LOGGER.warning("Subject code already exists: " + subject.getSubjectCode());
            return false;
        }

        return subjectDAO.update(subject);
    }

    /**
     * Delete subject
     */
    public boolean deleteSubject(int subjectId) {
        Subject existing = subjectDAO.findById(subjectId);
        if (existing == null) {
            LOGGER.warning("Subject not found: " + subjectId);
            return false;
        }

        // TODO: Check if subject is being used in courses/requests
        // For now, just delete
        return subjectDAO.delete(subjectId);
    }
}


