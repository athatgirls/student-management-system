package org.example.service.Impl;

import org.example.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {
    
    private static final String ONLINE_USER_KEY_PREFIX = "online:user:";
    private static final int ONLINE_TIMEOUT_MINUTES = 30; // 30分钟无活动视为离线
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void recordUserOnline(String userId, String userType) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        try {
            String key = ONLINE_USER_KEY_PREFIX + userType + ":" + userId;
            // 设置用户在线，30分钟过期
            redisTemplate.opsForValue().set(key, System.currentTimeMillis(), ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            log.debug("记录用户在线成功: userId={}, userType={}, key={}", userId, userType, key);
        } catch (Exception e) {
            // Redis连接失败时静默处理，不影响JWT认证
            log.warn("记录用户在线失败（Redis可能未启动）: {}", e.getMessage());
        }
    }
    
    @Override
    public int getOnlineUserCount() {
        try {
            // 使用KEYS获取所有在线用户key
            Set<String> keys = redisTemplate.keys(ONLINE_USER_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                // 过滤掉已过期的key（虽然Redis会自动过期，但这里做双重检查）
                int count = 0;
                for (String key : keys) {
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                        count++;
                    }
                }
                log.debug("获取在线用户数: {}", count);
                return count;
            }
            log.debug("没有在线用户");
            return 0;
        } catch (Exception e) {
            // Redis连接失败时返回0
            log.warn("获取在线用户数失败: {}", e.getMessage());
            return 0;
        }
    }
    
    @Override
    public void cleanExpiredUsers() {
        // Redis的过期机制会自动清理，这里可以添加额外的清理逻辑
        // 如果需要，可以检查并删除过期的key
    }
}

