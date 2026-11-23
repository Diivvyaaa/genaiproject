package com.codecrafthub.repository;

import com.codecrafthub.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CourseFileRepository {

    private final ObjectMapper objectMapper;
    private final Path filePath;

    public CourseFileRepository(
            ObjectMapper objectMapper,
            @Value("${courses.file-path:data/courses.json}") String filePath
    ) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.filePath = Paths.get(filePath);
    }

    public List<Course> findAll() {
        try {
            if (Files.notExists(filePath)) {
                return new ArrayList<>();
            }

            String json = Files.readString(filePath);

            if (json.isBlank()) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<Course>>() {
                    }
            );
        } catch (IOException exception) {
            throw new RuntimeException("Could not read courses file", exception);
        }
    }

    public void saveAll(List<Course> courses) {
        try {
            Path parent = filePath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(courses);

            Files.writeString(filePath, json);
        } catch (IOException exception) {
            throw new RuntimeException("Could not write courses file", exception);
        }
    }
}