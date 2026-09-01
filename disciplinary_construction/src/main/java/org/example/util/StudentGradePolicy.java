package org.example.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 学生年级的统一取值规则。
 */
public final class StudentGradePolicy {

    public static final List<String> DEFAULT_GRADES = Collections.unmodifiableList(
            Arrays.asList("2024级", "2025级", "2026级"));

    private StudentGradePolicy() {
    }

    public static String normalize(String grade) {
        if (grade == null) {
            return null;
        }
        String normalized = grade.trim();
        if (normalized.matches("\\d{4}")) {
            normalized += "级";
        }
        return normalized;
    }

    public static String requireValid(String grade) {
        String normalized = normalize(grade);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("请选择年级");
        }
        return normalized;
    }
}
