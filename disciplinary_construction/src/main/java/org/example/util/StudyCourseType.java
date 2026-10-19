package org.example.util;

public final class StudyCourseType {
    public static final String DEGREE = "degree";
    public static final String NON_DEGREE = "non_degree";

    private StudyCourseType() { }

    public static String requireValid(String value) {
        if (DEGREE.equals(value) || NON_DEGREE.equals(value)) return value;
        throw new IllegalArgumentException("课程类别只能是学位课或非学位课");
    }
}
