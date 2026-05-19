package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.PredictionConfig;
import com.lab.reservation.mapper.PredictionConfigMapper;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预测服务实现类
 * 采用霍尔特双指数平滑法进行时间序列预测
 */
@Service
public class PredictionServiceImpl implements PredictionService {

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private PredictionConfigMapper predictionConfigMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<Map<String, Object>> predictBookingTrend(int historyDays, int predictDays) {
        // 获取配置参数
        double alpha = getConfigDouble("prediction.booking.smoothing_alpha", 0.3);
        double beta = getConfigDouble("prediction.booking.smoothing_beta", 0.1);
        double confidenceInterval = getConfigDouble("prediction.booking.confidence_interval", 0.2);

        // 获取历史数据
        List<Integer> historyData = getBookingHistory(historyDays);

        // 霍尔特双指数平滑预测
        List<Double> predictions = holtWintersPredict(historyData, predictDays, alpha, beta);

        // 生成结果
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 添加历史数据（仅最后7天用于展示）
        int displayHistoryDays = Math.min(7, historyDays);
        for (int i = displayHistoryDays - 1; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            LocalDate date = today.minusDays(i);
            item.put("date", date.format(DATE_FMT));
            item.put("type", "history");
            item.put("value", historyData.get(historyData.size() - displayHistoryDays + (displayHistoryDays - 1 - i)));
            result.add(item);
        }

        // 添加预测数据
        double lastValue = historyData.isEmpty() ? 0 : historyData.get(historyData.size() - 1);
        for (int i = 0; i < predictDays; i++) {
            LocalDate date = today.plusDays(i + 1);
            double predictedValue = predictions.get(i);
            // 确保预测值非负
            predictedValue = Math.max(0, predictedValue);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DATE_FMT));
            item.put("type", "prediction");
            item.put("value", Math.round(predictedValue * 10) / 10.0);
            item.put("upper", Math.round(predictedValue * (1 + confidenceInterval) * 10) / 10.0);
            item.put("lower", Math.round(predictedValue * (1 - confidenceInterval) * 10) / 10.0);
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> predictDeviceUsage(Long deviceId, int historyDays, int predictDays) {
        // 获取历史数据
        List<Integer> historyData = getDeviceUsageHistory(deviceId, historyDays);

        // 简单移动平均预测
        List<Double> predictions = movingAveragePredict(historyData, predictDays);

        // 生成结果
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 添加历史数据
        int displayHistoryDays = Math.min(7, historyDays);
        for (int i = displayHistoryDays - 1; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            LocalDate date = today.minusDays(i);
            item.put("date", date.format(DATE_FMT));
            item.put("type", "history");
            item.put("value", historyData.get(historyData.size() - displayHistoryDays + (displayHistoryDays - 1 - i)));
            result.add(item);
        }

        // 添加预测数据
        for (int i = 0; i < predictDays; i++) {
            LocalDate date = today.plusDays(i + 1);
            double predictedValue = Math.max(0, predictions.get(i));

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(DATE_FMT));
            item.put("type", "prediction");
            item.put("value", Math.round(predictedValue * 10) / 10.0);
            result.add(item);
        }

        return result;
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        QueryWrapper<PredictionConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", key);
        // 库中若存在重复 config_key，selectOne 会抛异常；取第一条即可
        List<PredictionConfig> list = predictionConfigMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return defaultValue;
        }
        String v = list.get(0).getConfigValue();
        return v != null ? v : defaultValue;
    }

    /**
     * 获取预约历史数据
     */
    private List<Integer> getBookingHistory(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.ge("booking_date", startDate);
        wrapper.le("booking_date", endDate);
        wrapper.eq("status", 1); // 只统计已通过的预约

        List<BookingOrder> bookings = bookingOrderService.list(wrapper);

        // 按日期分组统计
        Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dateCountMap.put(endDate.minusDays(i), 0);
        }

        for (BookingOrder booking : bookings) {
            if (booking.getBookingDate() != null) {
                LocalDate date = booking.getBookingDate().toLocalDate();
                dateCountMap.merge(date, 1, Integer::sum);
            }
        }

        return new ArrayList<>(dateCountMap.values());
    }

    /**
     * 获取设备使用率历史数据
     */
    private List<Integer> getDeviceUsageHistory(Long deviceId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        QueryWrapper<BookingOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.ge("booking_date", startDate);
        wrapper.le("booking_date", endDate);
        wrapper.eq("status", 1);
        if (deviceId != null) {
            wrapper.eq("device_id", deviceId);
        }

        List<BookingOrder> bookings = bookingOrderService.list(wrapper);

        // 按日期分组统计
        Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            dateCountMap.put(endDate.minusDays(i), 0);
        }

        for (BookingOrder booking : bookings) {
            if (booking.getBookingDate() != null) {
                LocalDate date = booking.getBookingDate().toLocalDate();
                dateCountMap.merge(date, 1, Integer::sum);
            }
        }

        return new ArrayList<>(dateCountMap.values());
    }

    /**
     * 霍尔特双指数平滑预测
     * @param data 历史数据
     * @param periods 预测期数
     * @param alpha 水平平滑系数
     * @param beta 趋势平滑系数
     * @return 预测值列表
     */
    private List<Double> holtWintersPredict(List<Integer> data, int periods, double alpha, double beta) {
        if (data.isEmpty()) {
            return Collections.nCopies(periods, 0.0);
        }

        int n = data.size();
        double[] level = new double[n];
        double[] trend = new double[n];

        // 初始化
        level[0] = data.get(0);
        trend[0] = data.size() > 1 ? data.get(1) - data.get(0) : 0;

        // 平滑更新
        for (int i = 1; i < n; i++) {
            double prevLevel = level[i - 1];
            double prevTrend = trend[i - 1];
            double value = data.get(i);

            level[i] = alpha * value + (1 - alpha) * (prevLevel + prevTrend);
            trend[i] = beta * (level[i] - prevLevel) + (1 - beta) * prevTrend;
        }

        // 预测
        List<Double> forecasts = new ArrayList<>();
        double lastLevel = level[n - 1];
        double lastTrend = trend[n - 1];

        for (int i = 1; i <= periods; i++) {
            double forecast = lastLevel + i * lastTrend;
            forecasts.add(forecast);
        }

        return forecasts;
    }

    /**
     * 简单移动平均预测
     */
    private List<Double> movingAveragePredict(List<Integer> data, int periods) {
        if (data.isEmpty()) {
            return Collections.nCopies(periods, 0.0);
        }

        // 使用最近7天数据的平均
        int windowSize = Math.min(7, data.size());
        double sum = 0;
        for (int i = data.size() - windowSize; i < data.size(); i++) {
            sum += data.get(i);
        }
        double avg = sum / windowSize;

        // 计算趋势
        double trend = 0;
        if (data.size() >= 2) {
            int recent = Math.min(3, data.size());
            double recentSum = 0, prevSum = 0;
            for (int i = data.size() - recent; i < data.size(); i++) {
                recentSum += data.get(i);
            }
            for (int i = data.size() - 2 * recent; i < data.size() - recent; i++) {
                if (i >= 0) prevSum += data.get(i);
            }
            if (data.size() >= 2 * recent) {
                trend = (recentSum / recent - prevSum / recent) / recent;
            }
        }

        // 生成预测
        List<Double> forecasts = new ArrayList<>();
        for (int i = 0; i < periods; i++) {
            forecasts.add(avg + (i + 1) * trend);
        }

        return forecasts;
    }
}
