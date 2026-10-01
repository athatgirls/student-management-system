package org.example.controller;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.annotation.CurrentUser;
import org.example.model.InternshipEmploymentModel;
import org.example.service.CurrentUserAccessService;
import org.example.service.InternshipEmploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * 实习就业控制器
 * 提供实习就业相关的REST API接口
 */
@RestController
@RequestMapping("/msi/internship-employment")
public class InternshipEmploymentController {
    @Autowired
    private InternshipEmploymentService service;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    /**
     * 添加实习就业记录
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody InternshipEmploymentModel model, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!currentUserAccessService.isAdmin(currentUser)) {
                model.setUserId(currentUserAccessService.requireUserId(currentUser));
                model.setStudentId(currentUserAccessService.requireStudentNumber(currentUser));
                model.setStudentName((String) currentUser.get("username"));
            }
            
            InternshipEmploymentModel savedModel = service.add(model);
            
            result.put("code", 200);
            result.put("data", savedModel);
            result.put("msg", "添加成功");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "添加失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 更新实习就业记录
     */
    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> update(@RequestBody InternshipEmploymentModel model, @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            InternshipEmploymentModel existing = service.getById(model.getId());
            if (existing == null) {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "记录不存在");
                return ResponseEntity.ok(result);
            }
            currentUserAccessService.requireStudentAccess(currentUser,
                    existing.getUserId() != null ? existing.getUserId() : existing.getStudentId());
            model.setUserId(existing.getUserId());
            model.setStudentId(existing.getStudentId());
            model.setStudentName(existing.getStudentName());
            if (!currentUserAccessService.isAdmin(currentUser)) {
                model.setApprovalStatus("待审批");
                model.setApprovalRemark(null);
                model.setApprovalBy(null);
                model.setApprovalTime(null);
            }
            
            InternshipEmploymentModel updatedModel = service.update(model);
            result.put("code", 200);
            result.put("data", updatedModel);
            result.put("msg", "更新成功");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "更新失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 删除实习就业记录（软删除）
     */
    @RequestMapping(value = "/delete/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id,
                                                      @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            InternshipEmploymentModel existing = service.getById(id);
            if (existing != null) {
                currentUserAccessService.requireStudentAccess(currentUser,
                        existing.getUserId() != null ? existing.getUserId() : existing.getStudentId());
            }
            service.delete(id);
            result.put("code", 200);
            result.put("data", null);
            result.put("msg", "删除成功");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "删除失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取实习就业记录
     */
    @GetMapping("/get/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id,
                                                       @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            InternshipEmploymentModel model = service.getById(id);
            if (model != null) {
                currentUserAccessService.requireStudentAccess(currentUser,
                        model.getUserId() != null ? model.getUserId() : model.getStudentId());
                result.put("code", 200);
                result.put("data", model);
                result.put("msg", "获取成功");
            } else {
                result.put("code", 404);
                result.put("data", null);
                result.put("msg", "记录不存在");
            }
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前用户的实习就业记录
     */
    @GetMapping("/my-records")
    public ResponseEntity<Map<String, Object>> getMyRecords(@CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String userId = (String) currentUser.get("userId");
            List<InternshipEmploymentModel> models = service.getByUserId(userId);
            result.put("code", 200);
            result.put("data", models);
            result.put("msg", "获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有实习就业记录
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<InternshipEmploymentModel> models = service.getAll();
            result.put("code", 200);
            result.put("data", models);
            result.put("msg", "获取成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 基础查询API
    @GetMapping("/student/{studentId}")
    public ResponseEntity<Map<String, Object>> getByStudentId(@PathVariable String studentId,
                                                              @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentAccess(currentUser, studentId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByStudentId(studentId));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getByStatus(@PathVariable String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByStatus(status));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/approval-status/{approvalStatus}")
    public ResponseEntity<Map<String, Object>> getByApprovalStatus(@PathVariable String approvalStatus) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByApprovalStatus(approvalStatus));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, Object>> getByType(@PathVariable String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByType(type));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/company-type/{companyType}")
    public ResponseEntity<Map<String, Object>> getByCompanyType(@PathVariable String companyType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByCompanyType(companyType));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/industry/{industry}")
    public ResponseEntity<Map<String, Object>> getByIndustry(@PathVariable String industry) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByIndustry(industry));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/work-type/{workType}")
    public ResponseEntity<Map<String, Object>> getByWorkType(@PathVariable String workType) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByWorkType(workType));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/date-range")
    public ResponseEntity<Map<String, Object>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByDateRange(startDate, endDate));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/student-status")
    public ResponseEntity<Map<String, Object>> getByStudentIdAndStatus(
            @RequestParam String studentId, 
            @RequestParam String status) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByStudentIdAndStatus(studentId, status));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/student-approval")
    public ResponseEntity<Map<String, Object>> getByStudentIdAndApprovalStatus(
            @RequestParam String studentId, 
            @RequestParam String approvalStatus) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getByStudentIdAndApprovalStatus(studentId, approvalStatus));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    // 审批相关API
    @PostMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable String id,
            @RequestBody Map<String, String> requestBody,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String approvalRemark = requestBody.get("approvalRemark");
            String approvalBy = currentUserAccessService.requireUserId(currentUser);
            
            InternshipEmploymentModel model = service.approve(id, approvalRemark, approvalBy);
            if (model != null) {
                result.put("code", 200);
                result.put("data", model);
                result.put("msg", "审批成功");
            } else {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "审批失败");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "审批失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable String id,
            @RequestBody Map<String, String> requestBody,
            @CurrentUser Map<String, Object> currentUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            String approvalRemark = requestBody.get("approvalRemark");
            String approvalBy = currentUserAccessService.requireUserId(currentUser);
            
            InternshipEmploymentModel model = service.reject(id, approvalRemark, approvalBy);
            if (model != null) {
                result.put("code", 200);
                result.put("data", model);
                result.put("msg", "拒绝成功");
            } else {
                result.put("code", 400);
                result.put("data", null);
                result.put("msg", "操作失败");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "操作失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // 基础统计API
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> statistics = service.getStatistics();
            result.put("code", 200);
            result.put("data", statistics);
            result.put("msg", "获取统计信息成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取统计信息失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/status")
    public ResponseEntity<Map<String, Object>> getStatusStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getStatusStatistics());
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/industry")
    public ResponseEntity<Map<String, Object>> getIndustryStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getIndustryStatistics());
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/company-type")
    public ResponseEntity<Map<String, Object>> getCompanyTypeStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getCompanyTypeStatistics());
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/approval-status")
    public ResponseEntity<Map<String, Object>> getApprovalStatusStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.getApprovalStatusStatistics());
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    // 新增统计API
    @GetMapping("/statistics/technology-stack")
    public ResponseEntity<Map<String, Object>> getTechnologyStackStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Long> techStats = service.getTechnologyStackStatistics();
            result.put("code", 200);
            result.put("data", techStats);
            result.put("msg", "获取技术栈统计成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取技术栈统计失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/salary-range")
    public ResponseEntity<Map<String, Object>> getSalaryRangeStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Long> salaryStats = service.getSalaryRangeStatistics();
            result.put("code", 200);
            result.put("data", salaryStats);
            result.put("msg", "获取薪资范围统计成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取薪资范围统计失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/satisfaction")
    public ResponseEntity<Map<String, Object>> getSatisfactionStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> satisfactionStats = service.getSatisfactionStatistics();
            result.put("code", 200);
            result.put("data", satisfactionStats);
            result.put("msg", "获取满意度统计成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取满意度统计失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/employment-trend")
    public ResponseEntity<Map<String, Object>> getEmploymentTrendAnalysis() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> trendAnalysis = service.getEmploymentTrendAnalysis();
            result.put("code", 200);
            result.put("data", trendAnalysis);
            result.put("msg", "获取就业趋势分析成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取就业趋势分析失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/top-technology-stacks")
    public ResponseEntity<Map<String, Object>> getTopTechnologyStacks(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> topTechStacks = service.getTopTechnologyStacks(limit);
            result.put("code", 200);
            result.put("data", topTechStacks);
            result.put("msg", "获取热门技术栈成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取热门技术栈失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/top-industries")
    public ResponseEntity<Map<String, Object>> getTopIndustries(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> topIndustries = service.getTopIndustries(limit);
            result.put("code", 200);
            result.put("data", topIndustries);
            result.put("msg", "获取热门行业成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("data", null);
            result.put("msg", "获取热门行业失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export/statistics")
    public ResponseEntity<byte[]> exportStatisticsReport() throws IOException {
        Map<String, Object> statistics = service.getStatistics();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("实习就业统计");
            int rowIndex = 0;
            rowIndex = addStatisticRow(sheet, rowIndex, "统计项", "数值");
            rowIndex = addStatisticRow(sheet, rowIndex, "记录总数", statistics.get("totalCount"));
            rowIndex = addStatisticRow(sheet, rowIndex, "实习人数", statistics.get("internshipCount"));
            rowIndex = addStatisticRow(sheet, rowIndex, "就业人数", statistics.get("employmentCount"));
            rowIndex = addStatisticRow(sheet, rowIndex, "待审批", statistics.get("pendingApprovalCount"));
            rowIndex = addStatisticRow(sheet, rowIndex, "已通过", statistics.get("approvedCount"));
            addStatisticRow(sheet, rowIndex, "已拒绝", statistics.get("rejectedCount"));
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            workbook.write(output);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=internship-statistics.xlsx");
            return ResponseEntity.ok().headers(headers).body(output.toByteArray());
        }
    }

    private int addStatisticRow(Sheet sheet, int rowIndex, String label, Object value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value == null ? "0" : String.valueOf(value));
        return rowIndex + 1;
    }

    // 搜索相关API
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.search(keyword));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/student")
    public ResponseEntity<Map<String, Object>> searchByStudentName(@RequestParam String studentName) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.searchByStudentName(studentName));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/company")
    public ResponseEntity<Map<String, Object>> searchByCompany(@RequestParam String company) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.searchByCompany(company));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/position")
    public ResponseEntity<Map<String, Object>> searchByPosition(@RequestParam String position) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", service.searchByPosition(position));
        result.put("msg", "success");
        return ResponseEntity.ok(result);
    }
}
