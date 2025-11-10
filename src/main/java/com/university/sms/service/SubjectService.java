package com.university.sms.service;

import com.university.sms.dao.SubjectDAO;
import com.university.sms.model.Subject;

import java.util.List;
import java.util.logging.Logger;

public class SubjectService {
    private static final Logger LOGGER = Logger.getLogger(SubjectService.class.getName());
    private SubjectDAO subjectDAO;

    public SubjectService() {
        this.subjectDAO = new SubjectDAO();
    }

    public List<Subject> getAllSubjects() {
        return subjectDAO.findAll();
    }

    public Subject getSubjectById(int subjectId) {
        return subjectDAO.findById(subjectId);
    }

    public Subject getSubjectByCode(String subjectCode) {
        return subjectDAO.findByCode(subjectCode);
    }

    public List<Subject> getSubjectsByFaculty(String facultyCode) {
        return subjectDAO.findByFaculty(facultyCode);
    }

    public List<Subject> searchSubjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSubjects();
        }
        return subjectDAO.search(keyword.trim());
    }

    public boolean addSubject(Subject subject) {
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

    public boolean updateSubject(Subject subject) {
        if (subject.getSubjectId() <= 0) {
            LOGGER.warning("Invalid subject ID");
            return false;
        }

        Subject existing = subjectDAO.findById(subject.getSubjectId());
        if (existing == null) {
            LOGGER.warning("Subject not found: " + subject.getSubjectId());
            return false;
        }

        Subject duplicateCode = subjectDAO.findByCode(subject.getSubjectCode());
        if (duplicateCode != null && duplicateCode.getSubjectId() != subject.getSubjectId()) {
            LOGGER.warning("Subject code already exists: " + subject.getSubjectCode());
            return false;
        }

        return subjectDAO.update(subject);
    }

    public boolean deleteSubject(String subjectCode) {
        Subject existing = subjectDAO.findByCode(subjectCode);
        if (existing == null) {
            LOGGER.warning("Subject not found: " + subjectCode);
            return false;
        }

        return subjectDAO.delete(existing.getSubjectId()); // Use primary key for deletion
    }
}
