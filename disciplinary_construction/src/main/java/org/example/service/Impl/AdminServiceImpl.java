package org.example.service.Impl;

import org.example.model.AdminModel;
import org.example.repository.AdminRepository;
import org.example.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.bootstrap.admin.username:admin}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap.admin.password:admin123}")
    private String bootstrapAdminPassword;

    @Override
    public AdminModel login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        Optional<AdminModel> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            AdminModel admin = adminOpt.get();
            String storedPassword = admin.getPassword();
            boolean passwordMatches = storedPassword != null &&
                    (storedPassword.startsWith("$2")
                            ? passwordEncoder.matches(password, storedPassword)
                            : storedPassword.equals(password));
            if (!passwordMatches) {
                return null;
            }

            // Seamlessly migrate legacy plaintext administrator passwords.
            if (!storedPassword.startsWith("$2")) {
                admin.setPassword(passwordEncoder.encode(password));
            }
            // 检查管理员是否激活
            if (admin.getIsActive() != null && admin.getIsActive()) {
                // 更新最后登录时间
                admin.setLastLoginTime(new Date());
                adminRepository.save(admin);
                return admin;
            }
        }
        return null;
    }

    @Override
    public AdminModel createAdmin(AdminModel admin) {
        if (admin.getPassword() == null || admin.getPassword().length() < 8) {
            throw new IllegalArgumentException("管理员密码不能少于8位");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setCreateTime(new Date());
        admin.setUpdateTime(new Date());
        admin.setIsActive(true);
        return adminRepository.save(admin);
    }

    @Override
    public AdminModel updateAdmin(AdminModel admin) {
        AdminModel existing = adminRepository.findByAdminId(admin.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("管理员不存在"));

        existing.setUsername(admin.getUsername());
        existing.setRealName(admin.getRealName());
        existing.setEmail(admin.getEmail());
        existing.setPhone(admin.getPhone());
        existing.setRole(admin.getRole());
        existing.setDepartment(admin.getDepartment());
        existing.setPosition(admin.getPosition());
        existing.setPermissions(admin.getPermissions());
        existing.setRemark(admin.getRemark());
        existing.setUpdateBy(admin.getUpdateBy());
        if (admin.getPassword() != null && !admin.getPassword().isBlank()) {
            if (admin.getPassword().length() < 8) {
                throw new IllegalArgumentException("管理员密码不能少于8位");
            }
            existing.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        existing.setUpdateTime(new Date());
        return adminRepository.save(existing);
    }

    @Override
    public boolean deleteAdmin(String adminId) {
        Optional<AdminModel> adminOpt = adminRepository.findByAdminId(adminId);
        if (adminOpt.isPresent()) {
            adminRepository.delete(adminOpt.get());
            return true;
        }
        return false;
    }

    @Override
    public AdminModel findById(String id) {
        return adminRepository.findById(id).orElse(null);
    }

    @Override
    public AdminModel findByUsername(String username) {
        return adminRepository.findByUsername(username).orElse(null);
    }

    @Override
    public AdminModel findByAdminId(String adminId) {
        return adminRepository.findByAdminId(adminId).orElse(null);
    }

    @Override
    public List<AdminModel> findAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public List<AdminModel> findByRole(String role) {
        return adminRepository.findByRole(role);
    }

    @Override
    public boolean updateAdminStatus(String adminId, boolean isActive) {
        Optional<AdminModel> adminOpt = adminRepository.findByAdminId(adminId);
        if (adminOpt.isPresent()) {
            AdminModel admin = adminOpt.get();
            admin.setIsActive(isActive);
            admin.setUpdateTime(new Date());
            adminRepository.save(admin);
            return true;
        }
        return false;
    }

    @Override
    public void updateLastLoginTime(String adminId) {
        Optional<AdminModel> adminOpt = adminRepository.findByAdminId(adminId);
        if (adminOpt.isPresent()) {
            AdminModel admin = adminOpt.get();
            admin.setLastLoginTime(new Date());
            adminRepository.save(admin);
        }
    }

    @Override
    public boolean hasPermission(String adminId, String permission) {
        Optional<AdminModel> adminOpt = adminRepository.findByAdminId(adminId);
        if (adminOpt.isPresent()) {
            AdminModel admin = adminOpt.get();
            // 超级管理员拥有所有权限
            if ("super_admin".equals(admin.getRole())) {
                return true;
            }
            // 检查普通管理员是否有特定权限
            return admin.getPermissions() != null && admin.getPermissions().contains(permission);
        }
        return false;
    }

    @Override
    public void createDefaultAdmin() {
        // 检查是否已存在默认管理员
        Optional<AdminModel> existingAdmin = adminRepository.findByUsername(bootstrapAdminUsername);
        if (!existingAdmin.isPresent()) {
            AdminModel defaultAdmin = new AdminModel();
            defaultAdmin.setAdminId("ADMIN001");
            defaultAdmin.setUsername(bootstrapAdminUsername);
            defaultAdmin.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
            defaultAdmin.setRealName("系统管理员");
            defaultAdmin.setEmail("admin@hbut.edu.cn");
            defaultAdmin.setPhone("13800000000");
            defaultAdmin.setRole("super_admin");
            defaultAdmin.setDepartment("信息化建设小组");
            defaultAdmin.setPosition("系统管理员");
            defaultAdmin.setPermissions(Arrays.asList(
                "student:view", "student:edit", "student:delete",
                "alumni:view", "alumni:edit", "alumni:delete",
                "internship:view", "internship:edit", "internship:delete",
                "competition:view", "competition:edit", "competition:delete",
                "paper:view", "paper:edit", "paper:delete",
                "patent:view", "patent:edit", "patent:delete",
                "project:view", "project:edit", "project:delete",
                "party:view", "party:edit", "party:delete",
                "admin:manage"
            ));
            defaultAdmin.setIsActive(true);
            defaultAdmin.setCreateTime(new Date());
            defaultAdmin.setUpdateTime(new Date());
            defaultAdmin.setCreateBy("system");
            defaultAdmin.setRemark("系统默认管理员");
            
            adminRepository.save(defaultAdmin);
        }
    }
}
