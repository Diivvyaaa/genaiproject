package com.codecrafthub.exception;

/**
 * Exception thrown when courses.json cannot be created, read, or written.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}