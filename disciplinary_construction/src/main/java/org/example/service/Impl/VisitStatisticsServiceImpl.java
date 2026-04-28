package org.example.service.Impl;

import org.example.service.VisitStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class VisitStatisticsServiceImpl implements VisitStatisticsService {
    
    private static final String VISIT_COUNT_KEY_PREFIX = "visit:count:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void recordVisit(String userId, String userType) {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String countKey = VISIT_COUNT_KEY_PREFIX + today;
            
            // 如果提供了userId，使用用户ID去重，同一用户同一天只记录一次
            if (userId != null && !userId.isEmpty()) {
                String userVisitKey = VISIT_COUNT_KEY_PREFIX + "user:" + today + ":" + userId;
                // 检查该用户今天是否已访问过
                Boolean alreadyVisited = redisTemplate.hasKey(userVisitKey);
                if (Boolean.TRUE.equals(alreadyVisited)) {
                    // 该用户今天已访问过，不重复计数
                    return;
                }
                // 标记该用户今天已访问，24小时过期
                redisTemplate.opsForValue().set(userVisitKey, "1", 24, java.util.concurrent.TimeUnit.HOURS);
            }
            
            // 增加今日总访问次数
            redisTemplate.opsForValue().increment(countKey);
            // 设置过期时间为7天，避免数据无限增长
            redisTemplate.expire(countKey, 7, java.util.concurrent.TimeUnit.DAYS);
        } catch (Exception e) {
            // Redis连接失败时静默处理
        }
    }
    
    @Override
    public long getTodayVisitCount() {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String key = VISIT_COUNT_KEY_PREFIX + today;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0;
        } catch (Exception e) {
            // Redis连接失败时返回0
            return 0;
        }
    }
    
    @Override
    public long getVisitCountByDate(String date) {
        try {
            String key = VISIT_COUNT_KEY_PREFIX + date;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}

