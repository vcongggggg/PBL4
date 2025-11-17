package com.university.sms.service;

import com.university.sms.dao.SubjectDAO;
import com.university.sms.dao.FacultyDAO;
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

        // Validate facultyCode exists
        if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
            LOGGER.warning("Faculty code is required");
            return false;
        }
        FacultyDAO facultyDAO = new FacultyDAO();
        if (facultyDAO.findByCode(subject.getFacultyCode()) == null) {
            LOGGER.warning("Faculty code does not exist: " + subject.getFacultyCode());
            return false;
        }

        // Validate prerequisiteSubjectCode exists (if provided)
        if (subject.getPrerequisiteSubjectCode() != null && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
            Subject prerequisite = subjectDAO.findByCode(subject.getPrerequisiteSubjectCode());
            if (prerequisite == null) {
                LOGGER.warning("Prerequisite subject code does not exist: " + subject.getPrerequisiteSubjectCode());
                return false;
            }
            // Check circular prerequisite: subject cannot be prerequisite of itself
            if (subject.getSubjectCode().equals(subject.getPrerequisiteSubjectCode())) {
                LOGGER.warning("Circular prerequisite: Subject cannot be prerequisite of itself");
                return false;
            }
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

        // Validate facultyCode exists
        if (subject.getFacultyCode() == null || subject.getFacultyCode().trim().isEmpty()) {
            LOGGER.warning("Faculty code is required");
            return false;
        }
        FacultyDAO facultyDAO = new FacultyDAO();
        if (facultyDAO.findByCode(subject.getFacultyCode()) == null) {
            LOGGER.warning("Faculty code does not exist: " + subject.getFacultyCode());
            return false;
        }

        // Validate prerequisiteSubjectCode exists (if provided)
        if (subject.getPrerequisiteSubjectCode() != null && !subject.getPrerequisiteSubjectCode().trim().isEmpty()) {
            Subject prerequisite = subjectDAO.findByCode(subject.getPrerequisiteSubjectCode());
            if (prerequisite == null) {
                LOGGER.warning("Prerequisite subject code does not exist: " + subject.getPrerequisiteSubjectCode());
                return false;
            }
            // Check circular prerequisite: subject cannot be prerequisite of itself
            if (subject.getSubjectCode().equals(subject.getPrerequisiteSubjectCode())) {
                LOGGER.warning("Circular prerequisite: Subject cannot be prerequisite of itself");
                return false;
            }
            // Check deeper circular: if prerequisite has this subject as prerequisite
            // (indirect circular)
            if (prerequisite.getPrerequisiteSubjectCode() != null
                    && prerequisite.getPrerequisiteSubjectCode().equals(subject.getSubjectCode())) {
                LOGGER.warning("Circular prerequisite detected: Indirect circular dependency");
                return false;
            }
        }

        return subjectDAO.update(subject);
    }

    public boolean deleteSubject(String subjectCode) {
        Subject existing = subjectDAO.findByCode(subjectCode);
        if (existing == null) {
            LOGGER.warning("Subject not found: " + subjectCode);
            return false;
        }

        // Check if other subjects use this subject as prerequisite
        List<Subject> dependentSubjects = subjectDAO.findByPrerequisite(subjectCode);
        if (!dependentSubjects.isEmpty()) {
            LOGGER.warning("Cannot delete subject: Other subjects depend on it as prerequisite. " +
                    "Dependent subjects: " + dependentSubjects.size());
            return false;
        }

        return subjectDAO.delete(existing.getSubjectId()); // Use primary key for deletion
    }
}
