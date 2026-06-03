package com.capstone.eqh.domain.quiz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuizType {
    MULTIPLE_CHOICE,
    SHORT_ANSWER;

    @JsonCreator
    public static QuizType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "MULTIPLE_CHOICE", "MCQ", "MULTIPLE", "OBJECTIVE", "CHOICE" -> MULTIPLE_CHOICE;
            case "SHORT_ANSWER", "SHORT", "SUBJECTIVE", "ESSAY" -> SHORT_ANSWER;
            default -> QuizType.valueOf(normalized);
        };
    }
}
