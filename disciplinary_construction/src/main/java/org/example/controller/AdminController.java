package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.AdminModel;
import org.example.service.AdminService;
import org.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/msi/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private org.example.service.LoginAttemptLimiter loginAttemptLimiter;
    
    @Autowired
    private org.example.service.OnlineUserService onlineUserService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginForm) {
        loginAttemptLimiter.check("admin", loginForm.get("username"));
        String username = loginForm.get("username");
        String password = loginForm.get("password");
        
        Map<String, Object> result = new HashMap<>();
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            result.put("code", 400);
            result.put("data", null);
            result.put("msg", "用户名和密码不能为空");
            return ResponseEntity.badRequest().body(result);
        }
        AdminModel admin = adminService.login(username, password);
        
        if (admin != null) {
            // 生成JWT token
            String token = jwtUtil.generateToken(admin.getId(), "admin", admin.getUsername());
            
            // 记录用户在线
            try {
                onlineUserService.recordUserOnline(admin.getId(), "admin");
            } catch (Exception e) {
                // Redis连接失败不影响登录
            }
            
            // 返回token和基本用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", admin.getId());
            data.put("adminId", admin.getAdminId());
            data.put("username", admin.getUsername());
            data.put("realName", admin.getRealName());
            data.put("userType", "admin");
            
            result.put("code", 200);
            result.put("data", data);
            result.put("msg", "管理员登录成功");
        } else {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "用户名或密码错误");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAdmin(
            @RequestBody AdminModel admin,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查当前用户是否为超级管理员
            String userId = (String) currentUser.get("userId");
            AdminModel currentAdmin = adminService.findById(userId);
            if (currentAdmin == null || !"super_admin".equals(currentAdmin.getRole())) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "只有超级管理员可以创建管理员");
                return ResponseEntity.ok(result);
            }
            
            // 限制只能创建普通管理员
            if ("super_admin".equals(admin.getRole())) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "不能创建超级管理员");
                return ResponseEntity.ok(result);
            }
            
            AdminModel createdAdmin = adminService.createAdmin(admin);
            result.put("code", 200);
            result.put("data", createdAdmin);
            result.put("msg", "管理员创建成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "创建失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 获取当前管理员信息
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String userId = (String) currentUser.get("userId");
            String userType = (String) currentUser.get("userType");
            
            if ("admin".equals(userType)) {
                AdminModel admin = adminService.findById(userId);
                if (admin != null) {
                    // 只返回基本信息，不包含敏感信息
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", admin.getId());
                    userInfo.put("adminId", admin.getAdminId());
                    userInfo.put("username", admin.getUsername());
                    userInfo.put("realName", admin.getRealName());
                    userInfo.put("userType", "admin");
                    userInfo.put("role", admin.getRole());
                    
                    result.put("code", 200);
                    result.put("data", userInfo);
                    result.put("msg", "获取当前用户信息成功");
                } else {
                    result.put("code", 404);
                    result.put("data", null);
                    result.put("msg", "用户信息不存在");
                }
            } else {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "用户类型不匹配");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取当前用户信息失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // Token刷新接口
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> result = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            result.put("code", 401);
            result.put("data", null);
            result.put("msg", "缺少Authorization头");
            return ResponseEntity.ok(result);
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 即使token即将过期，也允许刷新
            if (jwtUtil.validateToken(token)) {
                // 检查token是否即将过期
                if (jwtUtil.isTokenNearExpiration(token)) {
                    // 生成新的token
                    String newToken = jwtUtil.refreshToken(token);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", newToken);
                    data.put("userId", jwtUtil.getUserIdFromToken(newToken));
                    data.put("userType", jwtUtil.getUserTypeFromToken(newToken));
                    data.put("username", jwtUtil.getUsernameFromToken(newToken));
                    
                    result.put("code", 200);
                    result.put("data", data);
                    result.put("msg", "Token刷新成功");
                } else {
                    result.put("code", 200);
                    result.put("data", null);
                    result.put("msg", "Token尚未过期，无需刷新");
                }
            } else {
                result.put("code", 401);
                result.put("data", null);
                result.put("msg", "Token无效或已过期");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "Token刷新失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateAdmin(
            @RequestBody AdminModel admin,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查当前用户是否为超级管理员
            String userId = (String) currentUser.get("userId");
            AdminModel currentAdmin = adminService.findById(userId);
            if (currentAdmin == null || !"super_admin".equals(currentAdmin.getRole())) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "只有超级管理员可以更新管理员信息");
                return ResponseEntity.ok(result);
            }
            
            // 检查要更新的管理员是否存在
            AdminModel existingAdmin = adminService.findByAdminId(admin.getAdminId());
            if (existingAdmin == null) {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "管理员不存在");
                return ResponseEntity.ok(result);
            }
            
            // 不能将普通管理员升级为超级管理员
            if (!"super_admin".equals(existingAdmin.getRole()) && "super_admin".equals(admin.getRole())) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "不能将普通管理员升级为超级管理员");
                return ResponseEntity.ok(result);
            }
            
            AdminModel updatedAdmin = adminService.updateAdmin(admin);
            result.put("code", 200);
            result.put("data", updatedAdmin);
            result.put("msg", "管理员信息更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "更新失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{adminId}")
    public ResponseEntity<Map<String, Object>> deleteAdmin(
            @PathVariable String adminId,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查当前用户是否为超级管理员
            String userId = (String) currentUser.get("userId");
            AdminModel currentAdmin = adminService.findById(userId);
            if (currentAdmin == null || !"super_admin".equals(currentAdmin.getRole())) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "只有超级管理员可以删除管理员");
                return ResponseEntity.ok(result);
            }
            
            // 检查要删除的管理员是否存在且不是超级管理员
            AdminModel targetAdmin = adminService.findByAdminId(adminId);
            if (targetAdmin == null) {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "管理员不存在");
                return ResponseEntity.ok(result);
            }
            
            // 不能删除超级管理员
            if ("super_admin".equals(targetAdmin.getRole())) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "不能删除超级管理员");
                return ResponseEntity.ok(result);
            }
            
            boolean success = adminService.deleteAdmin(adminId);
            if (success) {
                result.put("code", 200);
                result.put("msg", "管理员删除成功");
            } else {
                result.put("code", 404);
                result.put("msg", "管理员不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "删除失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllAdmins(
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查当前用户是否为超级管理员
            String userId = (String) currentUser.get("userId");
            AdminModel currentAdmin = adminService.findById(userId);
            if (currentAdmin == null || !"super_admin".equals(currentAdmin.getRole())) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "只有超级管理员可以查看管理员列表");
                return ResponseEntity.ok(result);
            }
            
            List<AdminModel> admins = adminService.findAllAdmins();
            result.put("code", 200);
            result.put("data", admins);
            result.put("msg", "获取管理员列表成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取管理员列表失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAdminById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        AdminModel admin = adminService.findById(id);
        if (admin != null) {
            result.put("code", 200);
            result.put("data", admin);
            result.put("msg", "获取管理员信息成功");
        } else {
            result.put("code", 404);
            result.put("data", null);
            result.put("msg", "管理员不存在");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<Map<String, Object>> getAdminsByRole(@PathVariable String role) {
        Map<String, Object> result = new HashMap<>();
        List<AdminModel> admins = adminService.findByRole(role);
        result.put("code", 200);
        result.put("data", admins);
        result.put("msg", "获取角色管理员列表成功");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/status/{adminId}")
    public ResponseEntity<Map<String, Object>> updateAdminStatus(
            @PathVariable String adminId, 
            @RequestBody Map<String, Boolean> statusMap,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查当前用户是否为超级管理员
            String userId = (String) currentUser.get("userId");
            AdminModel currentAdmin = adminService.findById(userId);
            if (currentAdmin == null || !"super_admin".equals(currentAdmin.getRole())) {
                result.put("code", 403);
                result.put("data", null);
                result.put("msg", "只有超级管理员可以更新管理员状态");
                return ResponseEntity.ok(result);
            }
            
            // 不能禁用超级管理员
            AdminModel targetAdmin = adminService.findByAdminId(adminId);
            if (targetAdmin != null && "super_admin".equals(targetAdmin.getRole()) && 
                Boolean.FALSE.equals(statusMap.get("isActive"))) {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "不能禁用超级管理员");
                return ResponseEntity.ok(result);
            }
            
            Boolean isActive = statusMap.get("isActive");
            boolean success = adminService.updateAdminStatus(adminId, isActive);
            if (success) {
                result.put("code", 200);
                result.put("msg", "管理员状态更新成功");
            } else {
                result.put("code", 404);
                result.put("msg", "管理员不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "更新状态失败: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/permission/{adminId}/{permission}")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @PathVariable String adminId, 
            @PathVariable String permission) {
        Map<String, Object> result = new HashMap<>();
        boolean hasPermission = adminService.hasPermission(adminId, permission);
        result.put("code", 200);
        result.put("data", hasPermission);
        result.put("msg", hasPermission ? "有权限" : "无权限");
        return ResponseEntity.ok(result);
    }
}
