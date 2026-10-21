package org.example.controller;

import org.example.annotation.CurrentUser;
import org.example.model.AppointmentModel;
import org.example.service.AppointmentService;
import org.example.response.ResponseResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/appointments")
public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service) { this.service = service; }
    @GetMapping("/list")
    public ResponseResult<List<AppointmentModel>> list(@CurrentUser Map<String, Object> user) { return ResponseResult.success(service.list(user)); }
    @PostMapping("/create")
    public ResponseResult<AppointmentModel> create(@RequestBody AppointmentModel record, @CurrentUser Map<String, Object> user) {
        return ResponseResult.success(service.save(null, record, user));
    }
    @PostMapping("/{id}/update")
    public ResponseResult<AppointmentModel> update(@PathVariable String id, @RequestBody AppointmentModel record, @CurrentUser Map<String, Object> user) {
        return ResponseResult.success(service.save(id, record, user));
    }
    @PostMapping("/{id}/delete")
    public ResponseResult<String> delete(@PathVariable String id, @CurrentUser Map<String, Object> user) {
        service.delete(id, user); return ResponseResult.success("删除成功");
    }
}
