package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DataScopeContext;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.DeviceScrapApplication;
import com.lab.reservation.entity.DeviceStatusLog;
import com.lab.reservation.entity.SysUser;
import com.lab.reservation.service.DataScopeService;
import com.lab.reservation.service.DeviceScrapApplicationService;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.DeviceRecommendationService;
import com.lab.reservation.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lab.reservation.config.FileUploadConfig;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@RestController
@RequestMapping("/device")
@CrossOrigin
public class DeviceController {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DeviceRecommendationService deviceRecommendationService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Autowired
    private DeviceScrapApplicationService deviceScrapApplicationService;

    @Autowired
    private DataScopeService dataScopeService;

    @Autowired
    private com.lab.reservation.mapper.BookingOrderMapper bookingOrderMapper;

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:add') or hasAuthority('device:edit')")
    public ResponseEntity<Map<String, Object>> uploadDeviceImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> body = new HashMap<>();
        if (file == null || file.isEmpty()) {
            body.put("message", "请选择图片文件");
            return ResponseEntity.badRequest().body(body);
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            body.put("message", "文件名不合法");
            return ResponseEntity.badRequest().body(body);
        }
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            body.put("message", "仅支持 jpg、jpeg、png、gif、webp 图片");
            return ResponseEntity.badRequest().body(body);
        }
        try {
            DataScopeContext ctx = dataScopeService.getCurrentDataScope();
            if (ctx != null && !ctx.isSystemAdmin() && !"MAINTAINER".equals(ctx.getUserType()) && !ctx.canEditDevice()) {
                body.put("message", "无设备编辑数据权限，无法上传图片");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
            }
            String base = fileUploadConfig.getUploadPath();
            if (base == null || base.trim().isEmpty()) {
                body.put("message", "文件上传路径未配置");
                return ResponseEntity.badRequest().body(body);
            }
            
            if (!new File(base).isAbsolute()) {
                String projectRoot = System.getProperty("user.dir");
                base = projectRoot + File.separator + base;
            }
            
            base = base.replace("\\", "/").trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            
            Path uploadDir = Paths.get(base, "device");
            
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            if (!Files.isWritable(uploadDir)) {
                body.put("message", "上传目录无写入权限：" + uploadDir.toString());
                return ResponseEntity.badRequest().body(body);
            }
            
            String name = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(name);
            
            file.transferTo(target.toFile());
            
            String relative = "/uploads/device/" + name;
            body.put("path", relative);
            body.put("message", "上传成功");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "保存文件失败：" + e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String laboratory,
            @RequestParam(required = false) Integer precisionLevel) {
        
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            wrapper.like("device_name", deviceName.trim());
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (laboratory != null && !laboratory.trim().isEmpty()) {
            wrapper.eq("laboratory", laboratory.trim());
        }
        if (precisionLevel != null) {
            wrapper.eq("precision_level", precisionLevel);
        }
        
        if (scopeContext != null) {
            applyDeviceDataScope(wrapper, scopeContext);
        }
        
        wrapper.orderByDesc("create_time");
        
        Page<DeviceInfo> page = new Page<>(pageNum, pageSize);
        Page<DeviceInfo> result = deviceInfoService.page(page, wrapper);

        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        res.put("pages", result.getPages());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:add') or hasAuthority('device:edit')")
    public ResponseEntity<byte[]> exportDevices(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String laboratory,
            @RequestParam(required = false) Integer precisionLevel) throws IOException {
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            wrapper.like("device_name", deviceName.trim());
        }
        if (categoryId != null) {
            wrapper.eq("category_id", categoryId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (laboratory != null && !laboratory.trim().isEmpty()) {
            wrapper.eq("laboratory", laboratory.trim());
        }
        if (precisionLevel != null) {
            wrapper.eq("precision_level", precisionLevel);
        }
        if (scopeContext != null) {
            applyDeviceDataScope(wrapper, scopeContext);
        }
        wrapper.orderByDesc("create_time");
        List<DeviceInfo> devices = deviceInfoService.list(wrapper);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("设备列表");
            String[] headers = {
                    "设备编号", "设备名称", "分类ID", "型号", "厂商", "购买日期", "价格(元)",
                    "实验室", "位置", "精度", "状态", "校准周期(天)", "简介", "创建时间"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int r = 1;
            for (DeviceInfo d : devices) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nvl(d.getDeviceNo()));
                row.createCell(1).setCellValue(nvl(d.getDeviceName()));
                row.createCell(2).setCellValue(d.getCategoryId() != null ? d.getCategoryId().toString() : "");
                row.createCell(3).setCellValue(nvl(d.getModel()));
                row.createCell(4).setCellValue(nvl(d.getManufacturer()));
                row.createCell(5).setCellValue(d.getPurchaseDate() != null ? d.getPurchaseDate().toString() : "");
                row.createCell(6).setCellValue(d.getPrice() != null ? d.getPrice() : 0);
                row.createCell(7).setCellValue(nvl(d.getLaboratory()));
                row.createCell(8).setCellValue(nvl(d.getLocation()));
                row.createCell(9).setCellValue(precisionLabel(d.getPrecisionLevel()));
                row.createCell(10).setCellValue(deviceStatusLabel(d.getStatus()));
                row.createCell(11).setCellValue(d.getCalibrationCycle() != null ? d.getCalibrationCycle() : 0);
                row.createCell(12).setCellValue(nvl(d.getDescription()));
                LocalDateTime ct = d.getCreateTime();
                row.createCell(13).setCellValue(ct != null ? ct.format(dtf) : "");
            }
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(bos);
        }
        byte[] bytes = bos.toByteArray();
        String filename = "设备列表_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String deviceStatusLabel(Integer s) {
        if (s == null) return "";
        switch (s) {
            case 0: return "空闲";
            case 1: return "使用中";
            case 2: return "维修中";
            case 3: return "校准中";
            case 4: return "报废";
            default: return String.valueOf(s);
        }
    }

    private static String precisionLabel(Integer p) {
        if (p == null) return "";
        switch (p) {
            case 1: return "低";
            case 2: return "中";
            case 3: return "高";
            default: return String.valueOf(p);
        }
    }
    
    private void applyDeviceDataScope(QueryWrapper<DeviceInfo> wrapper, DataScopeContext context) {
        if (context.isSystemAdmin() || "MAINTAINER".equals(context.getUserType())) {
            return;
        }
        if (!context.canBrowseReservableDevices()) {
            wrapper.eq("1", 0);
            return;
        }

        String st = context.getScopeType() != null ? context.getScopeType().trim().toUpperCase() : "";
        boolean isStudentOrTeacher = "STUDENT".equals(context.getUserType()) || "TEACHER".equals(context.getUserType());
        switch (st.isEmpty() ? "SELF" : st) {
            case "ALL":
                break;
            case "DEPT":
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else if (isStudentOrTeacher) {
                } else {
                    wrapper.eq("1", 0);
                }
                break;

            case "SELF":
                if (context.getLaboratory() != null && !context.getLaboratory().isEmpty()) {
                    wrapper.eq("laboratory", context.getLaboratory());
                } else if (isStudentOrTeacher) {
                } else {
                    wrapper.eq("1", 0);
                }
                break;

            case "CUSTOM":
                if (context.getCustomLabIds() != null && !context.getCustomLabIds().trim().isEmpty()) {
                    List<String> labs = Arrays.stream(context.getCustomLabIds().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    if (labs.isEmpty()) {
                        if (!isStudentOrTeacher) {
                            wrapper.eq("1", 0);
                        }
                    } else {
                        wrapper.in("laboratory", labs);
                    }
                } else {
                    if (!isStudentOrTeacher) {
                        wrapper.eq("1", 0);
                    }
                }
                break;

            default:
                if (context.canBrowseReservableDevices()) {
                    wrapper.eq("status", 0);
                } else {
                    wrapper.eq("1", 0);
                }
                break;
        }
    }

    @GetMapping("/status-logs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('device-track') or hasAuthority('device-status') or hasAuthority('device-list')")
    public ResponseEntity<Map<String, Object>> globalStatusLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = null;
        LocalDate end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        }

        DataScopeContext scopeCtx = dataScopeService.getCurrentDataScope();
        String labFilter = null;
        if (scopeCtx != null && !scopeCtx.isSystemAdmin() && !"MAINTAINER".equals(scopeCtx.getUserType())
                && scopeCtx.getLaboratory() != null && !scopeCtx.getLaboratory().isEmpty()) {
            labFilter = scopeCtx.getLaboratory();
        }

        Map<String, Object> body = deviceInfoService.pageGlobalStatusLogs(
                pageNum, pageSize, deviceName, deviceNo, changeType, operator, start, end, labFilter);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/status-logs/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('device-track') or hasAuthority('device-status') or hasAuthority('device-list')")
    public ResponseEntity<byte[]> exportGlobalStatusLogs(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = null;
        LocalDate end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        Map<String, Object> data = deviceInfoService.pageGlobalStatusLogs(
                1, 10000, deviceName, deviceNo, changeType, operator, start, end, null);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("list");
        StringBuilder csv = new StringBuilder("\uFEFF设备名称,设备编号,变更后类型,原状态,新状态,操作人,备注,变更时间\n");
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                csv.append(escapeCsv(r.get("deviceName"))).append(',')
                        .append(escapeCsv(r.get("deviceNo"))).append(',')
                        .append(escapeCsv(r.get("changeType"))).append(',')
                        .append(escapeCsv(r.get("fromStatus"))).append(',')
                        .append(escapeCsv(r.get("toStatus"))).append(',')
                        .append(escapeCsv(r.get("operator"))).append(',')
                        .append(escapeCsv(r.get("remark"))).append(',')
                        .append(escapeCsv(r.get("createTime"))).append('\n');
            }
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "device_status_logs_" + LocalDate.now() + ".csv";
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8"))
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(bytes);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeCsv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @GetMapping("/scrap/list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<Map<String, Object>> scrapList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeviceScrapApplication> page =
                deviceScrapApplicationService.pageApplications(pageNum, pageSize, deviceName, deviceNo, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/scrap/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<DeviceScrapApplication> scrapDetail(@PathVariable Long id) {
        DeviceScrapApplication one = deviceScrapApplicationService.getById(id);
        if (one == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(one);
    }

    @GetMapping("/scrap/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<byte[]> exportScrap(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeviceScrapApplication> page =
                deviceScrapApplicationService.pageApplications(1, 5000, deviceName, deviceNo, status);
        StringBuilder csv = new StringBuilder("\uFEFF设备名称,设备编号,设备型号,购买日期,购买价格,报废原因,申请人,状态,申请时间\n");
        for (DeviceScrapApplication row : page.getRecords()) {
            csv.append(esc(row.getDeviceName())).append(',')
               .append(esc(row.getDeviceNo())).append(',')
               .append(esc(row.getModel())).append(',')
               .append(esc(row.getPurchaseDate())).append(',')
               .append(esc(row.getPurchasePrice())).append(',')
               .append(esc(row.getScrapReason())).append(',')
               .append(esc(row.getApplicant())).append(',')
               .append(esc(row.getStatus())).append(',')
               .append(esc(row.getCreateTime())).append('\n');
        }
        try {
            String filename = "device_scrap_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/scrap")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','TEACHER') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<Map<String, Object>> scrapSubmit(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long deviceId = body.get("deviceId") != null ? Long.valueOf(body.get("deviceId").toString()) : null;
            String reason = body.get("scrapReason") != null ? body.get("scrapReason").toString() : null;
            Long uid = getCurrentUserId();
            deviceScrapApplicationService.submit(deviceId, reason, uid);
            res.put("message", "提交成功");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PutMapping("/scrap/{id}/approve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<Map<String, Object>> scrapApprove(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            deviceScrapApplicationService.approve(id);
            res.put("message", "已通过");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PutMapping("/scrap/{id}/reject")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<Map<String, Object>> scrapReject(@PathVariable Long id,
            @RequestParam(required = false) String opinion) {
        Map<String, Object> res = new HashMap<>();
        try {
            deviceScrapApplicationService.reject(id, opinion);
            res.put("message", "已拒绝");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PutMapping("/scrap/{id}/archive")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device-scrap') or hasAuthority('device:scrap')")
    public ResponseEntity<Map<String, Object>> scrapArchive(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            deviceScrapApplicationService.archive(id);
            res.put("message", "已归档");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceInfo>> getAllDevices() {
        QueryWrapper<DeviceInfo> wrapper = new QueryWrapper<>();
        DataScopeContext scopeContext = dataScopeService.getCurrentDataScope();
        if (scopeContext != null) {
            applyDeviceDataScope(wrapper, scopeContext);
        }
        List<DeviceInfo> list = deviceInfoService.list(wrapper);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{deviceId}/recommend-slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recommendTimeSlots(
            @PathVariable Long deviceId,
            @RequestParam String date) {
        List<Map<String, Object>> list = deviceRecommendationService.recommendTimeSlots(deviceId, date);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/status-logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeviceStatusLog>> getDeviceStatusLogs(@PathVariable Long id) {
        DeviceInfo dev = deviceInfoService.getById(id);
        if (dev == null) {
            return ResponseEntity.notFound().build();
        }
        if (!dataScopeService.canViewDevice(dev.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<DeviceStatusLog> logs = deviceInfoService.getStatusLogs(id);
        return ResponseEntity.ok(logs);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:add')")
    public ResponseEntity<String> addDevice(@RequestBody DeviceInfo device) {
        sanitizeDeviceEntity(device);
        validateDevicePayload(device, false);
        if (!dataScopeService.canEditDeviceInLab(device.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无设备编辑数据权限");
        }
        
        // 检查设备编号是否已存在
        if (device.getDeviceNo() != null && !device.getDeviceNo().trim().isEmpty()) {
            QueryWrapper<DeviceInfo> checkWrapper = new QueryWrapper<>();
            checkWrapper.eq("device_no", device.getDeviceNo().trim()).eq("deleted", 0);
            Long count = deviceInfoService.count(checkWrapper);
            if (count > 0) {
                return ResponseEntity.badRequest().body("设备编号已存在，请使用其他编号");
            }
        }
        
        device.setId(null);
        deviceInfoService.save(device);
        return ResponseEntity.ok("添加成功");
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:edit')")
    public ResponseEntity<String> updateDevice(@RequestBody DeviceInfo device) {
        return doUpdateDevice(device.getId(), device);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:edit')")
    public ResponseEntity<String> updateDeviceById(@PathVariable Long id, @RequestBody DeviceInfo device) {
        return doUpdateDevice(id, device);
    }

    private ResponseEntity<String> doUpdateDevice(Long id, DeviceInfo device) {
        if (device == null) {
            return ResponseEntity.badRequest().body("请求体不能为空");
        }
        if (id != null) {
            device.setId(id);
        }
        sanitizeDeviceEntity(device);
        validateDevicePayload(device, true);
        DeviceInfo existing = deviceInfoService.getById(device.getId());
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("设备不存在");
        }
        if (!dataScopeService.canEditDeviceInLab(existing.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无设备编辑数据权限");
        }
        
        // 检查设备编号是否已被其他设备使用（排除当前设备本身）
        if (device.getDeviceNo() != null && !device.getDeviceNo().trim().isEmpty()) {
            String newDeviceNo = device.getDeviceNo().trim();
            // 如果设备编号有变化，检查是否与其他设备冲突
            if (!newDeviceNo.equals(existing.getDeviceNo())) {
                QueryWrapper<DeviceInfo> checkWrapper = new QueryWrapper<>();
                checkWrapper.eq("device_no", newDeviceNo)
                          .eq("deleted", 0)
                          .ne("id", device.getId());
                Long count = deviceInfoService.count(checkWrapper);
                if (count > 0) {
                    return ResponseEntity.badRequest().body("设备编号已存在，请使用其他编号");
                }
            }
        }
        
        if (device.getLaboratory() != null && !device.getLaboratory().trim().isEmpty()
                && !device.getLaboratory().trim().equals(existing.getLaboratory() != null ? existing.getLaboratory().trim() : "")
                && !dataScopeService.canEditDeviceInLab(device.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无目标实验室的设备编辑数据权限");
        }
        deviceInfoService.updateById(device);
        return ResponseEntity.ok("更新成功");
    }

    private static void sanitizeDeviceEntity(DeviceInfo d) {
        if (d == null) {
            return;
        }
        d.setCategoryName(null);
        d.setCreateTime(null);
        d.setUpdateTime(null);
    }

    private static void validateDevicePayload(DeviceInfo d, boolean update) {
        if (d.getDeviceNo() == null || d.getDeviceNo().trim().isEmpty()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        if (d.getDeviceName() == null || d.getDeviceName().trim().isEmpty()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (d.getCategoryId() == null) {
            throw new IllegalArgumentException("请选择设备分类");
        }
        if (d.getLaboratory() == null || d.getLaboratory().trim().isEmpty()) {
            throw new IllegalArgumentException("请选择或填写所在实验室");
        }
        if (update && d.getId() == null) {
            throw new IllegalArgumentException("缺少设备ID，无法更新");
        }
        if (d.getPrecisionLevel() == null) {
            d.setPrecisionLevel(2);
        }
        if (d.getStatus() == null) {
            d.setStatus(0);
        }
        if (d.getCalibrationCycle() == null) {
            d.setCalibrationCycle(180);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN') or hasAuthority('device:delete')")
    public ResponseEntity<String> deleteDevice(@PathVariable Long id) {
        DeviceInfo existing = deviceInfoService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("设备不存在");
        }
        if (!dataScopeService.canDeleteDeviceInLab(existing.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无设备删除数据权限");
        }
        deviceInfoService.removeById(id);
        return ResponseEntity.ok("删除成功");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN','MAINTAINER') or hasAuthority('device:edit')")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        DeviceInfo existing = deviceInfoService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("设备不存在");
        }
        if (!dataScopeService.canChangeDeviceStatusInLab(existing.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无变更设备状态的数据权限");
        }
        deviceInfoService.updateDeviceStatus(id, status);
        return ResponseEntity.ok("状态更新成功");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        SysUser user = sysUserService.getByUsername(auth.getName());
        return user != null ? user.getId() : null;
    }

    @GetMapping("/usage/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<byte[]> exportUsageRecords(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<Map<String, Object>> all = bookingOrderMapper.selectUsageRecords(deviceName, userName, startDate, endDate, null);
            StringBuilder csv = new StringBuilder("\uFEFF设备名称,设备编号,使用人,部门/班级,使用日期,开始时间,结束时间,使用时长(h),实验项目,备注\n");
            for (Map<String, Object> r : all) {
                csv.append(esc(r.get("deviceName"))).append(',')
                   .append(esc(r.get("deviceNo"))).append(',')
                   .append(esc(r.get("userName"))).append(',')
                   .append(esc(r.get("department"))).append(',')
                   .append(esc(r.get("usageDate"))).append(',')
                   .append(esc(r.get("startTime"))).append(',')
                   .append(esc(r.get("endTime"))).append(',')
                   .append(esc(r.get("durationHours"))).append(',')
                   .append(esc(r.get("experimentProject"))).append(',')
                   .append(esc(r.get("remark"))).append('\n');
            }
            String filename = "device_usage_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(csv.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsageRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        DataScopeContext scopeCtx = dataScopeService.getCurrentDataScope();
        String labFilter = null;
        if (scopeCtx != null && !scopeCtx.isSystemAdmin() && scopeCtx.getLaboratory() != null && !scopeCtx.getLaboratory().isEmpty()) {
            labFilter = scopeCtx.getLaboratory();
        }

        List<Map<String, Object>> all = bookingOrderMapper.selectUsageRecords(deviceName, userName, startDate, endDate, labFilter);
        int total = all != null ? all.size() : 0;
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> page = (all != null && from < total) ? all.subList(from, to) : java.util.Collections.emptyList();
        Map<String, Object> result = new HashMap<>();
        result.put("list", page);
        result.put("total", total);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recommend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> recommendDevices(
            @RequestParam(defaultValue = "5") Integer limit) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "请先登录");
            return ResponseEntity.badRequest().body(err);
        }
        DataScopeContext scopeCtx = dataScopeService.getCurrentDataScope();
        if (scopeCtx != null && !scopeCtx.isSystemAdmin() && !"MAINTAINER".equals(scopeCtx.getUserType())
                && !scopeCtx.canBrowseReservableDevices()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("list", java.util.Collections.emptyList());
            return ResponseEntity.ok(empty);
        }
        List<Map<String, Object>> list = deviceRecommendationService.recommendDevices(userId, limit);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recommend/clear-cache")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearRecommendCache() {
        Map<String, Object> result = new HashMap<>();
        try {
            deviceRecommendationService.clearAllCache();
            result.put("message", "推荐缓存已清除");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/recommend/clear-my-cache")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> clearMyRecommendCache() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("message", "用户未登录");
            return ResponseEntity.badRequest().body(result);
        }
        try {
            deviceRecommendationService.clearUserCache(userId);
            result.put("message", "您的推荐缓存已清除");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("message", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeviceInfo> getDevice(@PathVariable Long id) {
        DeviceInfo device = deviceInfoService.getDeviceDetail(id);
        if (device == null) {
            return ResponseEntity.notFound().build();
        }
        if (!dataScopeService.canViewDevice(device.getLaboratory())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(device);
    }

    private String esc(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
