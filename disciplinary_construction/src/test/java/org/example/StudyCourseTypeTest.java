package org.example;

import org.example.util.StudyCourseType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudyCourseTypeTest {
    @Test
    void acceptsOnlyExplicitCourseTypes() {
        assertEquals("degree", StudyCourseType.requireValid("degree"));
        assertEquals("non_degree", StudyCourseType.requireValid("non_degree"));
        assertThrows(IllegalArgumentException.class, () -> StudyCourseType.requireValid(null));
        assertThrows(IllegalArgumentException.class, () -> StudyCourseType.requireValid("unknown"));
    }
}
