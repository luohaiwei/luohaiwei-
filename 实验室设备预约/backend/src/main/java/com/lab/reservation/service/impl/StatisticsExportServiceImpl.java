package com.lab.reservation.service.impl;

import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.RepairOrder;
import com.lab.reservation.entity.CalibrationRecord;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.BookingOrderService;
import com.lab.reservation.service.RepairOrderService;
import com.lab.reservation.service.CalibrationRecordService;
import com.lab.reservation.service.SysUserService;
import com.lab.reservation.service.SysLogService;
import com.lab.reservation.service.StatisticsExportService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统计数据导出实现
 */
@Service
public class StatisticsExportServiceImpl implements StatisticsExportService {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private BookingOrderService bookingOrderService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysLogService sysLogService;

    @Autowired
    private RepairOrderService repairOrderService;
    
    @Autowired
    private CalibrationRecordService calibrationRecordService;

    @Override
    public void exportExcel(OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("设备统计");
            int rowNum = 0;

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("统计项");
            headerRow.createCell(1).setCellValue("数值");

            rowNum = writeStatsToSheet(sheet, rowNum);

            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 3000);

            wb.write(out);
        }
    }

    @Override
    public void exportPdf(OutputStream out) throws Exception {
        exportPdfByType(out, null);
    }

    /** 按统计类型导出 PDF（供 StatisticsController.exportAdminReport 调用） */
    public void exportPdfByType(OutputStream out, String type) throws Exception {
        BaseFont bf = loadChineseBaseFont();
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(bf, 16, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(bf, 12, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font metaFont = new com.lowagie.text.Font(bf, 10);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(bf, 11, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font cellFont = new com.lowagie.text.Font(bf, 10);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("实验室设备预约系统 - 数据统计报表", titleFont));
        document.add(new Paragraph("导出时间：" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), metaFont));
        document.add(new Paragraph("报表类型：" + getTypeLabel(type), metaFont));
        document.add(Chunk.NEWLINE);

        // 按 type 写入不同数据；无 type 时写全量（兼容旧调用）
        writePdfSection(document, type, bf, sectionFont, headerFont, cellFont);

        document.close();
    }

    private String getTypeLabel(String type) {
        if (type == null) return "综合统计";
        switch (type) {
            case "user": return "用户统计";
            case "device": return "设备使用分析";
            case "booking": return "预约全量分析";
            case "maintenance": return "维护统计";
            case "calibration": return "校准达标率";
            case "user-profile": return "用户画像";
            default: return "综合统计";
        }
    }

    private void writePdfSection(Document document, String type,
            BaseFont bf, com.lowagie.text.Font sectionFont,
            com.lowagie.text.Font headerFont, com.lowagie.text.Font cellFont) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(new PdfPCell(new Phrase("统计项", headerFont)));
        table.addCell(new PdfPCell(new Phrase("数值", headerFont)));

        // null / all：写全量
        if (type == null || type.isEmpty() || "all".equalsIgnoreCase(type)) {
            Map<String, Object> all = gatherStats();
            for (Map.Entry<String, Object> e : all.entrySet()) {
                table.addCell(new PdfPCell(new Phrase(e.getKey(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(e.getValue()), cellFont)));
            }
        } else if ("user".equals(type)) {
            Map<String, Object> data = getUserStatisticsData();
            table.addCell(new PdfPCell(new Phrase("用户总数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("total") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("系统管理员", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("systemAdmin") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("实验室管理员", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("labAdmin") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("教师", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("teacher") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("学生", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("student") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("设备维护人员", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("maintainer") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本周活跃(登录去重)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("activeWeek") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本月活跃(登录去重)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("activeMonth") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("新增用户(本月)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("newMonth") : ""), cellFont)));
        } else if ("device".equals(type)) {
            Map<String, Object> data = getDeviceUsageAnalysisData();
            table.addCell(new PdfPCell(new Phrase("设备总数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("total") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("空闲设备", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("idle") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("使用中", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("using") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("维修中", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("maintaining") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("校准中", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("calibrating") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("已报废", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("scrapped") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("设备利用率(%)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("usageRate") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("平均利用率(%)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("avgUsageRate") : ""), cellFont)));
        } else if ("booking".equals(type)) {
            Map<String, Object> data = getBookingAnalysisData();
            table.addCell(new PdfPCell(new Phrase("本周预约总量", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("weekTotal") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("今日预约", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("todayTotal") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("预约成功率(%)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("successRate") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("预约取消率(%)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("cancelRate") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("待审核", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("pending") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("平均审核时长(h)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("avgAuditHours") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("平均等待时长(h)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("avgWaitHours") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本月预约总量", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("monthTotal") : ""), cellFont)));
        } else if ("maintenance".equals(type) || "fault".equals(type)) {
            Map<String, Object> data = getMaintenanceStatisticsData();
            table.addCell(new PdfPCell(new Phrase("本周维护次数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("weekCount") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本月维护次数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("monthCount") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("待处理工单", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("pending") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("处理中工单", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("processing") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("平均维修时长(h)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("avgRepairHours") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本月维修成本(元)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("monthCost") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本周校准次数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("weekCalibration") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("本月校准次数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("monthCalibration") : ""), cellFont)));
            document.add(table);
            // 故障类型列表
            List<Map<String, Object>> faultTypes = data != null ? (List<Map<String, Object>>) data.get("faultTypes") : null;
            if (faultTypes != null && !faultTypes.isEmpty()) {
                document.add(Chunk.NEWLINE);
                PdfPTable faultTable = new PdfPTable(2);
                faultTable.setWidthPercentage(100);
                faultTable.addCell(new PdfPCell(new Phrase("故障类型", headerFont)));
                faultTable.addCell(new PdfPCell(new Phrase("数量", headerFont)));
                for (Map<String, Object> ft : faultTypes) {
                    faultTable.addCell(new PdfPCell(new Phrase(String.valueOf(ft.get("name")), cellFont)));
                    faultTable.addCell(new PdfPCell(new Phrase(String.valueOf(ft.get("count")), cellFont)));
                }
                document.add(faultTable);
            }
            return;
        } else if ("calibration".equals(type)) {
            Map<String, Object> data = getCalibrationRateData();
            table.addCell(new PdfPCell(new Phrase("应校准总数", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("total") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("已完成", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("passed") : ""), cellFont)));
            table.addCell(new PdfPCell(new Phrase("达标率(%)", cellFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(data != null ? data.get("rate") : ""), cellFont)));
            document.add(table);
            
            document.add(Chunk.NEWLINE);
            List<String> months = data != null ? (List<String>) data.get("months") : null;
            List<Integer> totalList = data != null ? (List<Integer>) data.get("totalList") : null;
            List<Integer> passedList = data != null ? (List<Integer>) data.get("passedList") : null;
            List<Double> rateList = data != null ? (List<Double>) data.get("rateList") : null;
            if (months != null && !months.isEmpty()) {
                PdfPTable monthlyTable = new PdfPTable(4);
                monthlyTable.setWidthPercentage(100);
                monthlyTable.addCell(new PdfPCell(new Phrase("--- 月度校准数据（近6个月） ---", headerFont)));
                for (int i = 0; i < 3; i++) {
                    monthlyTable.addCell(new PdfPCell(new Phrase("", headerFont)));
                }
                monthlyTable.addCell(new PdfPCell(new Phrase("月份", headerFont)));
                monthlyTable.addCell(new PdfPCell(new Phrase("应校准数", headerFont)));
                monthlyTable.addCell(new PdfPCell(new Phrase("已完成", headerFont)));
                monthlyTable.addCell(new PdfPCell(new Phrase("达标率(%)", headerFont)));
                for (int i = 0; i < months.size(); i++) {
                    monthlyTable.addCell(new PdfPCell(new Phrase(months.get(i), cellFont)));
                    monthlyTable.addCell(new PdfPCell(new Phrase(totalList != null ? String.valueOf(totalList.get(i)) : "", cellFont)));
                    monthlyTable.addCell(new PdfPCell(new Phrase(passedList != null ? String.valueOf(passedList.get(i)) : "", cellFont)));
                    monthlyTable.addCell(new PdfPCell(new Phrase(rateList != null ? String.valueOf(rateList.get(i)) : "", cellFont)));
                }
                document.add(monthlyTable);
            }
            return;
        } else if ("user-profile".equals(type)) {
            document.add(table);
            document.add(Chunk.NEWLINE);
            
            Map<String, Object> profile = getUserProfileData();
            Map<String, Object> summary = profile != null ? (Map<String, Object>) profile.get("summary") : null;
            
            // 用户画像汇总
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.addCell(new PdfPCell(new Phrase("--- 用户画像汇总 ---", headerFont)));
            summaryTable.addCell(new PdfPCell(new Phrase("", headerFont)));
            summaryTable.addCell(new PdfPCell(new Phrase("用户总数", cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase(String.valueOf(summary != null ? summary.get("totalUsers") : ""), cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase("活跃用户", cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase(String.valueOf(summary != null ? summary.get("activeUsers") : ""), cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase("平均技能等级", cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase(String.valueOf(summary != null ? summary.get("avgSkillLevel") : ""), cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase("最受欢迎实验类型", cellFont)));
            summaryTable.addCell(new PdfPCell(new Phrase(String.valueOf(summary != null ? summary.get("mostPopularExpType") : ""), cellFont)));
            document.add(summaryTable);
            
            // 实验类型分布
            document.add(Chunk.NEWLINE);
            List<Map<String, Object>> expTypes = profile != null ? (List<Map<String, Object>>) profile.get("experimentType") : null;
            if (expTypes != null && !expTypes.isEmpty()) {
                PdfPTable expTable = new PdfPTable(2);
                expTable.setWidthPercentage(100);
                expTable.addCell(new PdfPCell(new Phrase("--- 实验类型分布 ---", headerFont)));
                expTable.addCell(new PdfPCell(new Phrase("", headerFont)));
                expTable.addCell(new PdfPCell(new Phrase("实验类型", headerFont)));
                expTable.addCell(new PdfPCell(new Phrase("数量", headerFont)));
                for (Map<String, Object> et : expTypes) {
                    expTable.addCell(new PdfPCell(new Phrase(String.valueOf(et.get("name")), cellFont)));
                    expTable.addCell(new PdfPCell(new Phrase(String.valueOf(et.get("value")), cellFont)));
                }
                document.add(expTable);
            }
            
            // 技能等级分布
            document.add(Chunk.NEWLINE);
            List<Map<String, Object>> skillLevels = profile != null ? (List<Map<String, Object>>) profile.get("skillLevel") : null;
            if (skillLevels != null && !skillLevels.isEmpty()) {
                PdfPTable skillTable = new PdfPTable(2);
                skillTable.setWidthPercentage(100);
                skillTable.addCell(new PdfPCell(new Phrase("--- 技能等级分布 ---", headerFont)));
                skillTable.addCell(new PdfPCell(new Phrase("", headerFont)));
                skillTable.addCell(new PdfPCell(new Phrase("技能等级", headerFont)));
                skillTable.addCell(new PdfPCell(new Phrase("数量", headerFont)));
                for (Map<String, Object> sl : skillLevels) {
                    skillTable.addCell(new PdfPCell(new Phrase(String.valueOf(sl.get("name")), cellFont)));
                    skillTable.addCell(new PdfPCell(new Phrase(String.valueOf(sl.get("value")), cellFont)));
                }
                document.add(skillTable);
            }
            
            // 用户使用频率排行 TOP 10
            document.add(Chunk.NEWLINE);
            List<Map<String, Object>> rankings = profile != null ? (List<Map<String, Object>>) profile.get("userUsageRanking") : null;
            if (rankings != null && !rankings.isEmpty()) {
                PdfPTable rankTable = new PdfPTable(7);
                rankTable.setWidthPercentage(100);
                rankTable.addCell(new PdfPCell(new Phrase("--- 用户使用频率排行 TOP 10 ---", headerFont)));
                for (int i = 0; i < 6; i++) {
                    rankTable.addCell(new PdfPCell(new Phrase("", headerFont)));
                }
                rankTable.addCell(new PdfPCell(new Phrase("排名", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("用户名", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("真实姓名", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("用户类型", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("实验类型", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("预约次数", headerFont)));
                rankTable.addCell(new PdfPCell(new Phrase("技能等级", headerFont)));
                for (Map<String, Object> rk : rankings) {
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("rank")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("username")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("realName")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("userType")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("experimentType")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("bookingCount")), cellFont)));
                    rankTable.addCell(new PdfPCell(new Phrase(String.valueOf(rk.get("skillLevel")), cellFont)));
                }
                document.add(rankTable);
            }
            return;
        } else {
            // 未知 type：回退全量
            Map<String, Object> all = gatherStats();
            for (Map.Entry<String, Object> e : all.entrySet()) {
                table.addCell(new PdfPCell(new Phrase(e.getKey(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(e.getValue()), cellFont)));
            }
        }

        document.add(table);
    }

    /**
     * 优先使用 classpath 下 fonts/NotoSansSC-Regular.otf；否则尝试常见系统中文字体（Windows/Linux）。
     * Helvetica 无法显示中文，会导致报表标题与"统计项"列为空白。
     */
    private BaseFont loadChineseBaseFont() throws Exception {
        String resource = "fonts/NotoSansSC-Regular.otf";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is != null) {
                byte[] bytes = IOUtils.toByteArray(is);
                return BaseFont.createFont("NotoSansSC-Regular.otf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, null, bytes);
            }
        }
        String[] fileCandidates = {
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/msyhbd.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        };
        for (String path : fileCandidates) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private int writeStatsToSheet(Sheet sheet, int rowNum) {
        Map<String, Object> stats = gatherStats();
        for (Map.Entry<String, Object> e : stats.entrySet()) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(String.valueOf(e.getValue()));
        }
        return rowNum;
    }

    private Map<String, Object> gatherStats() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("设备总数", deviceInfoService.count());
        m.put("空闲设备", deviceInfoService.countByStatus(0));
        m.put("使用中设备", deviceInfoService.countByStatus(1));
        m.put("维修中设备", deviceInfoService.countByStatus(2));
        m.put("校准中设备", deviceInfoService.countByStatus(3));
        m.put("报废设备", deviceInfoService.countByStatus(4));
        m.put("设备使用率(%)", String.format("%.1f", deviceInfoService.calculateUsageRate()));
        m.put("今日预约数", bookingOrderService.getTodayBookingCount());
        m.put("待审核预约", bookingOrderService.countPendingAudit());

        Double avgWaitTimeWeek = bookingOrderService.getAvgWaitTime("week");
        m.put("平均预约等待时长(小时)", avgWaitTimeWeek != null ? String.format("%.2f", avgWaitTimeWeek) : "0.00");

        List<Map<String, Object>> activityStats = sysLogService.getUserActivityStats(7, 10);
        m.put("本周活跃用户数", activityStats.size());

        return m;
    }

    // ------------------- 统计数据计算方法（与 StatisticsController 完全一致）-------------------
    
    private long countUsersByType(String userType) {
        return sysUserService.count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserType, userType));
    }
    
    private Map<String, Object> getUserStatisticsData() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", sysUserService.countAllUsers());
        data.put("systemAdmin", countUsersByType("SYSTEM_ADMIN"));
        data.put("labAdmin", countUsersByType("LAB_ADMIN"));
        data.put("teacher", countUsersByType("TEACHER"));
        data.put("student", countUsersByType("STUDENT"));
        data.put("maintainer", countUsersByType("MAINTAINER"));
        data.put("activeWeek", sysLogService.countDistinctLoginUsers(7));
        data.put("activeMonth", sysLogService.countDistinctLoginUsers(30));
        data.put("newMonth", sysUserService.count(
                Wrappers.<SysUser>lambdaQuery()
                        .apply("YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())")));
        return data;
    }
    
    private Map<String, Object> getDeviceUsageAnalysisData() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", deviceInfoService.count());
        data.put("idle", deviceInfoService.countByStatus(0));
        data.put("using", deviceInfoService.countByStatus(1));
        data.put("maintaining", deviceInfoService.countByStatus(2));
        data.put("calibrating", deviceInfoService.countByStatus(3));
        data.put("scrapped", deviceInfoService.countByStatus(4));
        data.put("usageRate", deviceInfoService.calculateUsageRate());
        data.put("avgUsageRate", deviceInfoService.calculateUsageRate());
        return data;
    }
    
    private Map<String, Object> getBookingAnalysisData() {
        Map<String, Object> data = new HashMap<>();
        data.put("weekTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(7).atStartOfDay())));
        data.put("todayTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .apply("DATE(create_time) = CURDATE()")));
        data.put("monthTotal", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0)
                .ge("create_time", LocalDate.now().minusDays(30).atStartOfDay())));
        long totalNonDeleted = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0));
        long approved = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 1));
        data.put("successRate", totalNonDeleted > 0 ? Math.round(approved * 1000.0 / totalNonDeleted) / 10.0 : 0);
        long cancelled = bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 4));
        data.put("cancelRate", totalNonDeleted > 0 ? Math.round(cancelled * 1000.0 / totalNonDeleted) / 10.0 : 0);
        data.put("pending", bookingOrderService.count(new QueryWrapper<BookingOrder>().eq("deleted", 0).eq("status", 0)));
        Double avgAudit = bookingOrderService.getAvgWaitTime("week");
        data.put("avgAuditHours", avgAudit != null ? avgAudit : 0);
        data.put("avgWaitHours", avgAudit != null ? avgAudit : 0);
        return data;
    }
    
    private Map<String, Object> getMaintenanceStatisticsData() {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);
        
        data.put("weekCount", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", weekStart.atStartOfDay())));
        data.put("monthCount", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", monthStart.atStartOfDay())));
        data.put("pending", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 0)));
        data.put("processing", repairOrderService.count(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 1)));
        
        List<RepairOrder> monthDone = repairOrderService.list(new QueryWrapper<RepairOrder>().eq("deleted", 0).eq("status", 2)
                .ge("handle_end_time", monthStart.atStartOfDay()));
        double avgH = 0;
        int n = 0;
        double monthCost = 0;
        for (RepairOrder r : monthDone) {
            if (r.getHandleStartTime() != null && r.getHandleEndTime() != null) {
                avgH += java.time.Duration.between(r.getHandleStartTime(), r.getHandleEndTime()).toMinutes() / 60.0;
                n++;
            }
            if (r.getRepairCost() != null) {
                monthCost += r.getRepairCost();
            }
        }
        data.put("avgRepairHours", n > 0 ? Math.round(avgH * 10.0 / n) / 10.0 : 0);
        data.put("monthCost", Math.round(monthCost * 100.0) / 100.0);
        
        // 校准统计（近7天和近30天）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime monthAgo = now.minusDays(30).toLocalDate().atStartOfDay();
        data.put("weekCalibration", calibrationRecordService.count(
                new QueryWrapper<CalibrationRecord>().eq("deleted", 0)
                        .ge("calibration_date", weekAgo)));
        data.put("monthCalibration", calibrationRecordService.count(
                new QueryWrapper<CalibrationRecord>().eq("deleted", 0)
                        .ge("calibration_date", monthAgo)));
        
        List<Map<String, Object>> faultTypes = new ArrayList<>();
        Map<String, Long> fc = new HashMap<>();
        List<RepairOrder> recent = repairOrderService.list(new QueryWrapper<RepairOrder>().eq("deleted", 0)
                .ge("create_time", monthStart.atStartOfDay()).last("LIMIT 200"));
        for (RepairOrder r : recent) {
            String key = r.getFaultDescription() != null && r.getFaultDescription().length() > 0
                    ? r.getFaultDescription().substring(0, Math.min(20, r.getFaultDescription().length()))
                    : "其他";
            fc.put(key, fc.getOrDefault(key, 0L) + 1);
        }
        for (Map.Entry<String, Long> e : fc.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("count", e.getValue().intValue());
            faultTypes.add(m);
        }
        data.put("faultTypes", faultTypes);
        
        return data;
    }
    
    private Map<String, Object> getCalibrationRateData() {
        Map<String, Object> data = new HashMap<>();
        
        // 查询最近6个月的校准记录
        LocalDate startDate = LocalDate.now().minusMonths(6 - 1).withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter monthLabelFmt = DateTimeFormatter.ofPattern("MM月");
        
        List<CalibrationRecord> records = calibrationRecordService.list(
                new QueryWrapper<CalibrationRecord>()
                        .ge("calibration_date", startDate.atStartOfDay())
                        .le("calibration_date", endDate.atTime(23, 59, 59))
                        .eq("deleted", 0)
                        .orderByDesc("calibration_date")
        );
        
        int totalCount = records.size();
        int passedCount = 0;
        Map<String, int[]> monthlyData = new java.util.LinkedHashMap<>();
        
        // 初始化每月数据
        for (int i = 6 - 1; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusMonths(i);
            String monthKey = d.format(monthFmt);
            monthlyData.put(monthKey, new int[]{0, 0}); // [total, passed]
        }
        
        // 统计每月数据
        for (CalibrationRecord record : records) {
            if (record.getCalibrationDate() == null) continue;
            String monthKey = record.getCalibrationDate().toLocalDate().format(monthFmt);
            if (monthlyData.containsKey(monthKey)) {
                int[] dataArr = monthlyData.get(monthKey);
                dataArr[0]++; // 总次数
                if (record.getResult() != null && record.getResult() == 1) {
                    dataArr[1]++; // 达标次数
                    passedCount++;
                }
            }
        }
        
        // 转换为前端需要的格式
        List<String> monthLabels = new ArrayList<>();
        List<Integer> totalList = new ArrayList<>();
        List<Integer> passedList = new ArrayList<>();
        List<Double> rateList = new ArrayList<>();
        
        for (Map.Entry<String, int[]> entry : monthlyData.entrySet()) {
            String monthKey = entry.getKey();
            LocalDate monthDate = LocalDate.parse(monthKey + "-01");
            monthLabels.add(monthDate.format(monthLabelFmt));
            int total = entry.getValue()[0];
            int passed = entry.getValue()[1];
            totalList.add(total);
            passedList.add(passed);
            double rate = total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0;
            rateList.add(rate);
        }
        
        // 计算总体达标率
        double overallRate = totalCount > 0 ? Math.round(passedCount * 1000.0 / totalCount) / 10.0 : 0.0;
        
        data.put("months", monthLabels);
        data.put("totalList", totalList);
        data.put("passedList", passedList);
        data.put("rateList", rateList);
        data.put("total", totalCount);
        data.put("passed", passedCount);
        data.put("rate", overallRate);
        
        return data;
    }
    
    private String formatUserType(String userType) {
        if (userType == null) return "未知";
        switch (userType) {
            case "SYSTEM_ADMIN": return "系统管理员";
            case "LAB_ADMIN": return "实验室管理员";
            case "TEACHER": return "教师";
            case "STUDENT": return "学生";
            case "MAINTAINER": return "设备维护人员";
            default: return userType;
        }
    }
    
    private Map<String, Object> getUserProfileData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 实验类型分布（饼图数据）
        List<SysUser> allUsers = sysUserService.list(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeleted, 0));
        Map<String, Integer> experimentTypeCount = new HashMap<>();
        Map<String, Integer> skillLevelCount = new HashMap<>();
        Map<String, Integer> userTypeCount = new HashMap<>();
        skillLevelCount.put("初学(1级)", 0);
        skillLevelCount.put("一般(2级)", 0);
        skillLevelCount.put("熟练(3级)", 0);

        for (SysUser user : allUsers) {
            // 统计实验类型
            String expType = user.getExperimentType();
            if (expType != null && !expType.isEmpty()) {
                experimentTypeCount.put(expType, experimentTypeCount.getOrDefault(expType, 0) + 1);
            } else {
                experimentTypeCount.put("未设置", experimentTypeCount.getOrDefault("未设置", 0) + 1);
            }

            // 统计技能等级
            Integer skillLevel = user.getSkillLevel();
            if (skillLevel != null) {
                switch (skillLevel) {
                    case 1:
                        skillLevelCount.put("初学(1级)", skillLevelCount.get("初学(1级)") + 1);
                        break;
                    case 2:
                        skillLevelCount.put("一般(2级)", skillLevelCount.get("一般(2级)") + 1);
                        break;
                    case 3:
                        skillLevelCount.put("熟练(3级)", skillLevelCount.get("熟练(3级)") + 1);
                        break;
                }
            }

            // 统计用户类型
            String userType = formatUserType(user.getUserType());
            userTypeCount.put(userType, userTypeCount.getOrDefault(userType, 0) + 1);
        }

        // 转换为前端需要的格式
        List<Map<String, Object>> experimentTypeData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : experimentTypeCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            experimentTypeData.add(item);
        }

        List<Map<String, Object>> skillLevelData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : skillLevelCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            skillLevelData.add(item);
        }

        result.put("experimentType", experimentTypeData);
        result.put("skillLevel", skillLevelData);
        result.put("userType", userTypeCount);

        // 2. 用户使用频率排行（柱状图数据 - Top 10）
        Map<Long, Integer> userBookingCount = new HashMap<>();

        // 直接从booking_order统计用户预约次数
        QueryWrapper<BookingOrder> bookingWrapper = new QueryWrapper<>();
        bookingWrapper.eq("deleted", 0);
        List<BookingOrder> bookings = bookingOrderService.list(bookingWrapper);
        for (BookingOrder bo : bookings) {
            if (bo.getUserId() != null) {
                userBookingCount.put(bo.getUserId(), userBookingCount.getOrDefault(bo.getUserId(), 0) + 1);
            }
        }

        List<Map<String, Object>> userUsageRanking = new ArrayList<>();
        List<Map.Entry<Long, Integer>> sortedUserBookings = new ArrayList<>(userBookingCount.entrySet());
        sortedUserBookings.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedUserBookings) {
            if (rank > 10) break;
            SysUser user = sysUserService.getById(entry.getKey());
            if (user != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", rank++);
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("realName", user.getRealName() != null ? user.getRealName() : user.getUsername());
                item.put("userType", formatUserType(user.getUserType()));
                item.put("experimentType", user.getExperimentType() != null ? user.getExperimentType() : "未设置");
                item.put("bookingCount", entry.getValue());
                item.put("skillLevel", user.getSkillLevel() != null ? user.getSkillLevel() : 1);
                userUsageRanking.add(item);
            }
        }
        result.put("userUsageRanking", userUsageRanking);

        // 3. 汇总统计
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalUsers", allUsers.size());
        summary.put("activeUsers", sortedUserBookings.size());
        summary.put("avgSkillLevel", calculateAvgSkillLevel(allUsers));
        summary.put("mostPopularExpType", findMostPopularExperimentType(experimentTypeCount));
        result.put("summary", summary);

        return result;
    }
    
    private double calculateAvgSkillLevel(List<SysUser> users) {
        if (users.isEmpty()) return 0;
        int total = 0;
        int count = 0;
        for (SysUser user : users) {
            if (user.getSkillLevel() != null) {
                total += user.getSkillLevel();
                count++;
            }
        }
        return count > 0 ? Math.round(total * 10.0 / count) / 10.0 : 0;
    }
    
    private String findMostPopularExperimentType(Map<String, Integer> expTypeCount) {
        String mostPopular = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : expTypeCount.entrySet()) {
            if (!"未设置".equals(entry.getKey()) && entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostPopular = entry.getKey();
            }
        }
        return mostPopular != null ? mostPopular : "未设置";
    }
}
