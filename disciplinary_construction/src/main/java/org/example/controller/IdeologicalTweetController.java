package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.annotation.CurrentUser;
import org.example.dto.StudentRef;
import org.example.model.IdeologicalTweetModel;
import org.example.response.ResponseResult;
import org.example.service.CurrentUserAccessService;
import org.example.service.IdeologicalTweetService;
import org.example.service.StudentRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/msi/ideological-tweet")
@Tag(name = "思想动态管理", description = "思想动态相关接口")
public class IdeologicalTweetController {

    @Autowired
    private IdeologicalTweetService ideologicalTweetService;

    @Autowired
    private StudentRefService studentRefService;

    @Autowired
    private CurrentUserAccessService currentUserAccessService;

    @Operation(summary = "发布思想动态")
    @PostMapping("/add")
    public ResponseResult<IdeologicalTweetModel> addIdeologicalTweet(@RequestBody IdeologicalTweetModel ideologicalTweet, @CurrentUser Map<String, Object> currentUser) {
        // 学生本人发布：身份一律取本人档案，不信任请求体
        fillStudentInfo(ideologicalTweet, studentRefService.current(currentUser));
        IdeologicalTweetModel result = ideologicalTweetService.addIdeologicalTweet(ideologicalTweet);
        return ResponseResult.success(result);
    }

    @Operation(summary = "更新思想动态")
    @PostMapping("/update")
    public ResponseResult<IdeologicalTweetModel> updateIdeologicalTweet(@RequestBody IdeologicalTweetModel ideologicalTweet, @CurrentUser Map<String, Object> currentUser) {
        // 学生本人维护：身份一律取本人档案，不信任请求体
        fillStudentInfo(ideologicalTweet, studentRefService.current(currentUser));
        IdeologicalTweetModel result = ideologicalTweetService.updateIdeologicalTweet(ideologicalTweet);
        return ResponseResult.success(result);
    }

    @Operation(summary = "删除思想动态")
    @PostMapping("/delete/{id}")
    public ResponseResult<String> deleteIdeologicalTweet(@PathVariable String id) {
        ideologicalTweetService.deleteIdeologicalTweet(id);
        return ResponseResult.success("删除成功");
    }

    @Operation(summary = "获取学生思想动态")
    @GetMapping("/student/{studentAccountId}")
    public ResponseResult<List<IdeologicalTweetModel>> getStudentIdeologicalTweets(@PathVariable String studentAccountId,
                                                                                  @CurrentUser Map<String, Object> currentUser) {
        currentUserAccessService.requireStudentRecord(currentUser, studentAccountId);
        List<IdeologicalTweetModel> result = ideologicalTweetService.getStudentIdeologicalTweets(studentAccountId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据ID获取思想动态")
    @GetMapping("/{id}")
    public ResponseResult<IdeologicalTweetModel> getIdeologicalTweetById(@PathVariable String id) {
        IdeologicalTweetModel result = ideologicalTweetService.getIdeologicalTweetById(id);
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取待审核思想动态列表")
    @GetMapping("/pending")
    public ResponseResult<List<IdeologicalTweetModel>> getPendingIdeologicalTweets() {
        List<IdeologicalTweetModel> result = ideologicalTweetService.getPendingIdeologicalTweets();
        return ResponseResult.success(result);
    }

    @Operation(summary = "审核思想动态")
    @PostMapping("/audit/{id}")
    public ResponseResult<IdeologicalTweetModel> auditIdeologicalTweet(
            @PathVariable String id,
            @RequestBody Map<String, String> auditData,
            @CurrentUser Map<String, Object> currentUser) {
        String auditorId = currentUserAccessService.requireUserId(currentUser);
        String auditStatus = auditData.get("auditStatus");
        String auditComment = auditData.get("auditComment");
        
        IdeologicalTweetModel result = ideologicalTweetService.auditIdeologicalTweet(id, auditStatus, auditComment, auditorId);
        return ResponseResult.success(result);
    }

    @Operation(summary = "获取所有思想动态列表")
    @GetMapping("/list")
    public ResponseResult<List<IdeologicalTweetModel>> getAllIdeologicalTweets() {
        List<IdeologicalTweetModel> result = ideologicalTweetService.getAllIdeologicalTweets();
        return ResponseResult.success(result);
    }

    @Operation(summary = "搜索思想动态")
    @GetMapping("/search")
    public ResponseResult<List<IdeologicalTweetModel>> searchIdeologicalTweets(@RequestParam String keyword) {
        List<IdeologicalTweetModel> result = ideologicalTweetService.searchIdeologicalTweets(keyword);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据动态类型获取思想动态列表")
    @GetMapping("/type/{tweetType}")
    public ResponseResult<List<IdeologicalTweetModel>> getIdeologicalTweetsByType(@PathVariable String tweetType) {
        List<IdeologicalTweetModel> result = ideologicalTweetService.getIdeologicalTweetsByType(tweetType);
        return ResponseResult.success(result);
    }

    @Operation(summary = "根据状态获取思想动态列表")
    @GetMapping("/status/{status}")
    public ResponseResult<List<IdeologicalTweetModel>> getIdeologicalTweetsByStatus(@PathVariable String status) {
        List<IdeologicalTweetModel> result = ideologicalTweetService.getIdeologicalTweetsByStatus(status);
        return ResponseResult.success(result);
    }

    /** 写入学生标识：studentAccountId 是关联键，studentId（学号）与姓名仅展示。 */
    private void fillStudentInfo(IdeologicalTweetModel ideologicalTweet, StudentRef ref) {
        ideologicalTweet.setStudentAccountId(ref.getAccountId());
        ideologicalTweet.setStudentId(ref.getStudentId());
        ideologicalTweet.setStudentName(ref.getName());
    }
} 
