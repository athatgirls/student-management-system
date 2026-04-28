package org.example.service;

public interface VisitStatisticsService {
    /**
     * 记录一次访问
     * @param userId 用户ID（可为null，表示未登录用户）
     * @param userType 用户类型（student/admin/null）
     */
    void recordVisit(String userId, String userType);
    
    /**
     * 获取今日访问次数
     * @return 今日访问次数
     */
    long getTodayVisitCount();
    
    /**
     * 获取指定日期的访问次数
     * @param date 日期字符串（格式：yyyy-MM-dd）
     * @return 访问次数
     */
    long getVisitCountByDate(String date);
}

