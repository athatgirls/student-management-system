package org.example.service;

import org.example.model.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;

/** Current credential state is consulted on every token validation, including refresh and file reads. */
@Service
public class AccountTokenStateService {
    private final StudentRepository students;
    private final AdminRepository admins;
    public AccountTokenStateService(StudentRepository students, AdminRepository admins) {
        this.students = students; this.admins = admins;
    }
    public String state(String id, String type) {
        if ("student".equals(type)) {
            StudentModel s = students.findById(id).orElse(null);
            if (s == null || s.getPassword() == null || Boolean.TRUE.equals(s.getPasswordChangeRequired())) return null;
            return "student:" + s.getPassword();
        }
        if ("admin".equals(type)) {
            AdminModel a = admins.findById(id).orElse(null);
            if (a == null || !Boolean.TRUE.equals(a.getIsActive()) || a.getPassword() == null) return null;
            return "admin:" + a.getPassword() + ":" + a.getRole() + ":" + a.getAuthVersion();
        }
        return null;
    }
}
