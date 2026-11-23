package com.codecrafthub.exception;

/**
 * Exception thrown when a course with the requested ID does not exist.
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long courseId) {
        super("Course with ID " + courseId + " was not found");
    }
}