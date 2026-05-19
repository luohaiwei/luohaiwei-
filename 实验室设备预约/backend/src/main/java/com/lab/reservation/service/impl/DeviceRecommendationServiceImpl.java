package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.mapper.BookingOrderMapper;
import com.lab.reservation.mapper.DeviceInfoMapper;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.DeviceRecommendationService;
import com.lab.reservation.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备智能推荐服务实现
 * 基于协同过滤：找相似用户（相同设备类型偏好）推荐其使用过的设备
 * 优化：添加缓存机制、置信度评分、设备使用率因子
 */
@Service
public class DeviceRecommendationServiceImpl implements DeviceRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRecommendationServiceImpl.class);

    @Autowired
    private BookingOrderMapper bookingOrderMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "device_recommend:";
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(30);

    private static final String[] TIME_SLOTS = {"08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"};

    @Override
    public List<Map<String, Object>> recommendDevices(Long userId, int limit) {
        // 尝试从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + userId;
        try {
            if (redisTemplate != null) {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cachedList = (List<Map<String, Object>>) cached;
                    // 空列表不采用缓存（避免长期命中「无推荐」）
                    if (!cachedList.isEmpty()) {
                        logger.debug("从缓存获取用户 {} 的推荐设备", userId);
                        return cachedList.stream().limit(limit).collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            // 缓存异常不影响主流程
            logger.warn("读取缓存失败: {}", e.getMessage());
        }

        List<Map<String, Object>> result = computeRecommendations(userId, limit);

        // 仅缓存非空结果，避免首次无预约用户长期看不到热门设备兜底
        try {
            if (redisTemplate != null && !result.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRY);
                logger.debug("缓存用户 {} 的推荐设备，共 {} 个", userId, result.size());
            }
        } catch (Exception e) {
            // 缓存异常不影响主流程
            logger.warn("写入缓存失败: {}", e.getMessage());
        }

        return result;
    }

    private List<Map<String, Object>> computeRecommendations(Long userId, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 1. 获取当前用户信息（用于匹配实验类型偏好）
        SysUser currentUser = getUserById(userId);
        if (currentUser == null) {
            // 用户不存在，返回空结果
            logger.warn("用户 {} 不存在，无法推荐设备", userId);
            return result;
        }

        logger.info("用户 {} - 实验室: {}, 用户类型: {}", userId, currentUser.getLaboratory(), currentUser.getUserType());

        // 2. 先统计所有设备使用次数（用于后续评分和兜底推荐；提前声明以便 null 用户走 fallbackHotDevices 时可用）
        Map<Long, Integer> deviceUsageCountMap = new HashMap<>();
        QueryWrapper<BookingOrder> allWrapper = new QueryWrapper<>();
        allWrapper.in("status", 1, 3).eq("deleted", 0);
        List<BookingOrder> allBookings = bookingOrderMapper.selectList(allWrapper);
        for (BookingOrder b : allBookings) {
            if (b.getDeviceId() != null) {
                deviceUsageCountMap.merge(b.getDeviceId(), 1, Integer::sum);
            }
        }


        String userExperimentType = currentUser.getExperimentType();
        Integer userSkillLevel = currentUser.getSkillLevel();

        // 3. 获取当前用户历史预约的设备及分类
        QueryWrapper<BookingOrder> userWrapper = new QueryWrapper<>();
        userWrapper.eq("user_id", userId).in("status", 1, 3).eq("deleted", 0);
        List<BookingOrder> userBookings = bookingOrderMapper.selectList(userWrapper);
        Set<Long> userBookedDeviceIds = userBookings.stream()
                .map(BookingOrder::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userCategoryIds = new HashSet<>();
        for (BookingOrder b : userBookings) {
            if (b.getDeviceId() != null) {
                DeviceInfo d = deviceInfoService.getById(b.getDeviceId());
                if (d != null && d.getCategoryId() != null) {
                    userCategoryIds.add(d.getCategoryId());
                }
            }
        }

        // 4. 多因子协同过滤评分
        Map<Long, Double> deviceScoreMap = new HashMap<>();
        int maxUsage = deviceUsageCountMap.values().stream().max(Integer::compareTo).orElse(1);

        // 为每个设备计算个性化分数
        QueryWrapper<DeviceInfo> deviceWrapper = new QueryWrapper<>();
        deviceWrapper.eq("status", 0).eq("deleted", 0);
        List<DeviceInfo> allIdleDevices = deviceInfoMapper.selectList(deviceWrapper);

        logger.info("用户 {} - 可用空闲设备数量: {}", userId, allIdleDevices.size());

        for (DeviceInfo d : allIdleDevices) {
            // 检查实验室权限：如果用户有所属实验室，只推荐同一实验室的设备
            if (currentUser.getLaboratory() != null && !currentUser.getLaboratory().isEmpty()
                    && d.getLaboratory() != null && !d.getLaboratory().equals(currentUser.getLaboratory())) {
                logger.debug("跳过设备 {} - 实验室不匹配 (用户实验室: {}, 设备实验室: {})", 
                    d.getDeviceName(), currentUser.getLaboratory(), d.getLaboratory());
                continue; // 跳过其他实验室的设备
            }

            // 计算相似度分数
            double score = 0.0;

            // 因子1：设备分类匹配（核心相似度）
            boolean categoryMatch = userCategoryIds.contains(d.getCategoryId());
            if (categoryMatch) {
                score += 30;
            }

            // 因子2：实验类型偏好匹配
            if (userExperimentType != null && d.getDescription() != null
                    && d.getDescription().contains(userExperimentType)) {
                score += 20;
            }

            // 因子3：设备使用率因子（热门但不过于拥挤）
            int usageCount = deviceUsageCountMap.getOrDefault(d.getId(), 0);
            double usageRate = (double) usageCount / maxUsage;
            score += (1 - usageRate) * 20; // 越少人用分数越高

            // 因子4：用户数量因子（多人使用说明设备好）
            int similarUserCount = countSimilarUsers(userId, userCategoryIds, allBookings);
            score += Math.min(similarUserCount * 2, 20);

            // 因子5：熟练度匹配
            if (userSkillLevel != null && d.getPrecisionLevel() != null) {
                if (userSkillLevel >= d.getPrecisionLevel()) {
                    score += 10;
                }
            }

            // 因子6：个性化随机因子（确保不同用户推荐结果不同）
            int userHash = userId.intValue() % 100;
            int deviceHash = d.getId().intValue() % 100;
            double randomFactor = (userHash + deviceHash) % 10 / 10.0;
            score += randomFactor * 5;

            // 因子7：实验室匹配度（同一实验室的设备优先）
            if (currentUser.getLaboratory() != null && !currentUser.getLaboratory().isEmpty()
                    && d.getLaboratory() != null && d.getLaboratory().equals(currentUser.getLaboratory())) {
                score += 15;
            }

            // 累加分数
            deviceScoreMap.put(d.getId(), score);
        }

        logger.info("用户 {} - 符合条件的设备数量: {}", userId, deviceScoreMap.size());

        // 4. 排除用户已预约过的，按得分排序
        List<Map.Entry<Long, Double>> sorted = deviceScoreMap.entrySet().stream()
                .filter(e -> !userBookedDeviceIds.contains(e.getKey()))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit * 2) // 多取一些用于计算置信度
                .collect(Collectors.toList());

        double maxScore = sorted.isEmpty() ? 1.0 : sorted.get(0).getValue();

        for (Map.Entry<Long, Double> e : sorted) {
            if (result.size() >= limit) break;
            DeviceInfo device = deviceInfoService.getById(e.getKey());
            if (device != null && device.getStatus() == 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("device", device);
                item.put("score", e.getValue());
                item.put("confidence", calculateConfidence(e.getValue(), maxScore));
                item.put("reason", generateRecommendationReason(device, e.getValue(), maxScore, userCategoryIds.contains(device.getCategoryId()), !userBookedDeviceIds.isEmpty()));
                item.put("usageCount", deviceUsageCountMap.getOrDefault(device.getId(), 0));
                result.add(item);
            }
        }

        // 5. 若推荐不足，补充热门空闲设备（按使用率）
        if (result.size() < limit) {
            QueryWrapper<DeviceInfo> dw = new QueryWrapper<>();
            dw.eq("status", 0).eq("deleted", 0);
            List<DeviceInfo> idleDevices = deviceInfoMapper.selectList(dw);
            for (DeviceInfo d : idleDevices) {
                if (result.size() >= limit) break;
                
                // 检查实验室权限：如果用户有所属实验室，只推荐同一实验室的设备
                if (currentUser.getLaboratory() != null && !currentUser.getLaboratory().isEmpty()
                        && d.getLaboratory() != null && !d.getLaboratory().equals(currentUser.getLaboratory())) {
                    continue; // 跳过其他实验室的设备
                }
                
                // 排除已在推荐结果中的设备（兜底推荐与协同过滤结果不重复）
                if (result.stream().anyMatch(r -> {
                    DeviceInfo rd = (DeviceInfo) r.get("device");
                    return rd != null && rd.getId().equals(d.getId());
                })) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("device", d);
                item.put("score", 5.0);
                item.put("confidence", 0.3);
                item.put("reason", "当前空闲设备，可直接预约");
                item.put("usageCount", deviceUsageCountMap.getOrDefault(d.getId(), 0));
                result.add(item);
            }
        }

        // 6. 仍无任何推荐：库内无空闲设备时，按历史预约热度展示热门设备（可含非空闲），避免新用户/全忙场景下区块永远空白
        if (result.isEmpty()) {
            return fallbackHotDevices(limit, 2.0, deviceUsageCountMap, currentUser);
        }

        return result;
    }

    private static String buildHotDeviceReason(Integer status) {
        if (status == null) {
            return "系统热门设备（请关注设备状态后再预约）";
        }
        switch (status) {
            case 0:
                return "热门设备，当前空闲可预约";
            case 1:
                return "热门设备（使用中，可稍后再试预约）";
            case 2:
                return "热门设备（维修中，暂不可预约）";
            case 3:
                return "热门设备（校准中，暂不可预约）";
            default:
                return "热门设备（请关注设备状态）";
        }
    }

    private int countSimilarUsers(Long userId, Set<Long> categoryIds, List<BookingOrder> allBookings) {
        Set<Long> similarUserIds = new HashSet<>();
        for (BookingOrder b : allBookings) {
            if (Objects.equals(b.getUserId(), userId)) {
                continue;
            }
            DeviceInfo d = deviceInfoService.getById(b.getDeviceId());
            if (d != null && categoryIds.contains(d.getCategoryId())) {
                similarUserIds.add(b.getUserId());
            }
        }
        return similarUserIds.size();
    }

    private double calculateConfidence(double score, double maxScore) {
        if (maxScore <= 0) return 0.5;
        double confidence = score / maxScore;
        return Math.round(confidence * 100.0) / 100.0; // 保留2位小数
    }

    /**
     * 生成推荐理由
     * @param hasUserHistory 用户是否有历史预约记录（用于判断是否显示个性化推荐文案）
     */
    private String generateRecommendationReason(DeviceInfo device, double score, double maxScore, boolean categoryMatch, boolean hasUserHistory) {
        double percentage = maxScore > 0 ? (score / maxScore) * 100 : 50;
        
        // 新用户无历史记录时，直接显示热门/空闲推荐，不使用个性化文案
        if (!hasUserHistory) {
            // 根据设备状态返回合适的推荐理由
            if (device != null && device.getStatus() != null && device.getStatus() == 0) {
                return "热门推荐：当前空闲的设备，新用户首选";
            } else {
                return "系统热门设备，请关注设备状态";
            }
        }
        
        // 有历史记录的用户，显示个性化推荐文案
        if (percentage >= 80) {
            return "强烈推荐：与您的历史偏好高度匹配，相似用户使用率高";
        } else if (percentage >= 50) {
            if (categoryMatch) {
                return "推荐：与您常用的设备类型相同，热门且空闲";
            } else {
                return "推荐：热门设备，当前可预约";
            }
        } else {
            return "可选：当前空闲的设备";
        }
    }

    private SysUser getUserById(Long userId) {
        if (sysUserService != null) {
            return sysUserService.getById(userId);
        }
        return null;
    }

    /**
     * 兜底推荐：返回热门空闲设备（无历史偏好时也保证有推荐结果）
     * @param limit       推荐数量
     * @param score       兜底分数（供前端显示）
     * @param usageCountMap 设备使用次数统计（用于按热度排序）
     * @param currentUser 当前用户信息
     */
    private List<Map<String, Object>> fallbackHotDevices(int limit, double score,
            Map<Long, Integer> usageCountMap, SysUser currentUser) {
        List<Map<String, Object>> result = new ArrayList<>();
        QueryWrapper<DeviceInfo> qw = new QueryWrapper<>();
        qw.eq("status", 0).eq("deleted", 0);
        List<DeviceInfo> idleDevices = deviceInfoMapper.selectList(qw);
        
        // 对设备进行排序，优先推荐使用次数多的设备
        idleDevices.sort((a, b) -> Integer.compare(
                usageCountMap.getOrDefault(b.getId(), 0),
                usageCountMap.getOrDefault(a.getId(), 0)));
        
        for (DeviceInfo d : idleDevices) {
            if (result.size() >= limit) break;
            
            // 检查实验室权限：如果用户有所属实验室，只推荐同一实验室的设备
            if (currentUser.getLaboratory() != null && !currentUser.getLaboratory().isEmpty()
                    && d.getLaboratory() != null && !d.getLaboratory().equals(currentUser.getLaboratory())) {
                continue; // 跳过其他实验室的设备
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("device", d);
            item.put("score", score);
            item.put("confidence", 0.3);
            item.put("reason", "热门空闲设备，欢迎预约使用");
            item.put("usageCount", usageCountMap.getOrDefault(d.getId(), 0));
            result.add(item);
        }
        
        // 库内无任何空闲设备时：展示所有未报废设备（按热度排序）
        if (result.isEmpty()) {
            QueryWrapper<DeviceInfo> anyQw = new QueryWrapper<>();
            anyQw.ne("status", 4).eq("deleted", 0);
            List<DeviceInfo> candidates = deviceInfoMapper.selectList(anyQw);
            candidates.sort((a, b) -> Integer.compare(
                    usageCountMap.getOrDefault(b.getId(), 0),
                    usageCountMap.getOrDefault(a.getId(), 0)));
            for (DeviceInfo d : candidates) {
                if (result.size() >= limit) break;
                
                // 检查实验室权限：如果用户有所属实验室，只推荐同一实验室的设备
                if (currentUser.getLaboratory() != null && !currentUser.getLaboratory().isEmpty()
                        && d.getLaboratory() != null && !d.getLaboratory().equals(currentUser.getLaboratory())) {
                    continue; // 跳过其他实验室的设备
                }
                
                Map<String, Object> item = new HashMap<>();
                item.put("device", d);
                item.put("score", score);
                item.put("confidence", 0.2);
                item.put("reason", "系统热门设备（请关注设备状态）");
                item.put("usageCount", usageCountMap.getOrDefault(d.getId(), 0));
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> recommendTimeSlots(Long deviceId, String dateStr) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return result;
        }

        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId)
                .apply("DATE(booking_date) = {0}", date)
                .in("status", 0, 1)
                .eq("deleted", 0);
        List<BookingOrder> bookings = bookingOrderMapper.selectList(wrapper);
        Set<String> occupiedStarts = new HashSet<>();
        for (BookingOrder b : bookings) {
            occupiedStarts.add(b.getStartTime());
        }

        for (int i = 0; i < TIME_SLOTS.length - 1; i++) {
            String start = TIME_SLOTS[i];
            String end = TIME_SLOTS[i + 1];
            boolean available = !occupiedStarts.contains(start);

            // 计算空闲指数
            int recentUsage = countRecentSlotUsage(deviceId, start);
            double freeIndex = 1.0 - (recentUsage / 10.0); // 假设最多10次/时段

            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", start);
            slot.put("endTime", end);
            slot.put("available", available);
            slot.put("freeIndex", Math.round(freeIndex * 100.0) / 100.0);
            slot.put("label", start + "-" + end);
            result.add(slot);
        }
        return result;
    }

    private int countRecentSlotUsage(Long deviceId, String startTime) {
        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId)
                .eq("start_time", startTime)
                .in("status", 1, 3)
                .eq("deleted", 0)
                .apply("booking_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)");
        return Long.valueOf(bookingOrderMapper.selectCount(wrapper)).intValue();
    }

    @Override
    public void clearUserCache(Long userId) {
        if (redisTemplate != null) {
            String cacheKey = CACHE_KEY_PREFIX + userId;
            try {
                redisTemplate.delete(cacheKey);
                logger.info("已清除用户 {} 的推荐缓存", userId);
            } catch (Exception e) {
                logger.warn("清除用户 {} 的推荐缓存失败: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    public void clearAllCache() {
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    logger.info("已清除所有推荐缓存，共 {} 个", keys.size());
                }
            } catch (Exception e) {
                logger.warn("清除所有推荐缓存失败: {}", e.getMessage());
            }
        }
    }
}
