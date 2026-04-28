package org.example.service;

public interface OnlineUserService {
    /**
     * 记录用户在线（用户登录或活动时调用）
     * @param userId 用户ID
     * @param userType 用户类型（student/admin）
     */
    void recordUserOnline(String userId, String userType);
    
    /**
     * 获取当前在线用户数
     * @return 在线用户数
     */
    int getOnlineUserCount();
    
    /**
     * 清理过期的在线用户记录（超过30分钟无活动）
     */
    void cleanExpiredUsers();
}

