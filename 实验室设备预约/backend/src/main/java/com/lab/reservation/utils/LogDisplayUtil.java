package com.lab.reservation.utils;

/**
 * 日志审计展示：中文描述、IP 归属文案等
 */
public final class LogDisplayUtil {

    private LogDisplayUtil() {
    }

    /** HTTP 操作类型 → 中文 */
    public static String operationTypeZh(String operation) {
        if (operation == null || operation.isEmpty()) {
            return "其他";
        }
        switch (operation.trim().toUpperCase()) {
            case "CREATE":
            case "INSERT":
                return "新增";
            case "UPDATE":
            case "MODIFY":
                return "修改";
            case "DELETE":
                return "删除";
            case "QUERY":
            case "GET":
                return "查询";
            case "LOGIN":
                return "登录";
            case "LOGOUT":
                return "登出";
            case "AUDIT":
                return "审核";
            case "EXPORT":
                return "导出";
            case "SAVE":
                return "保存";
            default:
                return operation;
        }
    }

    /** 模块名英/混排 → 中文（与拦截器一致，并兼容历史英文 Controller 名） */
    public static String moduleToZh(String module) {
        if (module == null || module.isEmpty()) {
            return "系统";
        }
        String m = module.trim();
        switch (m) {
            case "用户管理":
            case "User":
                return "用户管理";
            case "设备管理":
            case "Device":
                return "设备管理";
            case "角色管理":
            case "Role":
                return "角色管理";
            case "权限分配":
            case "Permission":
                return "权限分配";
            case "系统消息":
            case "SysMessage":
                return "系统消息";
            case "认证登录":
            case "Auth":
                return "认证登录";
            case "预约管理":
            case "Booking":
                return "预约管理";
            case "数据统计":
            case "Statistics":
                return "数据统计";
            case "系统配置":
            case "SysConfig":
                return "系统配置";
            case "系统日志":
            case "SysLog":
                return "系统日志";
            case "维修工单":
            case "Repair":
                return "维修工单";
            case "设备分类":
            case "Category":
                return "设备分类";
            case "实验室管理":
            case "Laboratory":
                return "实验室管理";
            case "AI助手":
            case "AI":
            case "AiAssistant":
                return "AI助手";
            default:
                if (m.endsWith("Controller")) {
                    return moduleToZh(m.substring(0, m.length() - "Controller".length()));
                }
                return m;
        }
    }

    /** 操作描述：中文摘要 */
    public static String buildOperationDesc(String module, String operation, String method, String requestUrl) {
        String opZh = operationTypeZh(operation);
        String modZh = moduleToZh(module);
        StringBuilder sb = new StringBuilder();
        sb.append(opZh).append("「").append(modZh).append("」");
        if (method != null && !method.isEmpty()) {
            sb.append("（").append(method).append("）");
        }
        if (requestUrl != null && !requestUrl.isEmpty() && requestUrl.length() < 80) {
            sb.append(" ").append(requestUrl);
        }
        return sb.toString();
    }

    /**
     * IP 展示为中文说明（非精确地理库，满足审计可读性）
     */
    public static String ipToChineseLabel(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip.trim())) {
            return "未知";
        }
        String s = ip.trim();
        if ("127.0.0.1".equals(s) || "0:0:0:0:0:0:0:1".equals(s) || "::1".equals(s)) {
            return "本机回环地址";
        }
        if (s.startsWith("192.168.") || s.startsWith("10.") || s.startsWith("172.16.")
                || s.startsWith("172.17.") || s.startsWith("172.18.") || s.startsWith("172.19.")
                || s.startsWith("172.2") || s.startsWith("172.30.") || s.startsWith("172.31.")) {
            return "局域网地址";
        }
        if (s.contains(":")) {
            return "IPv6 地址";
        }
        return "公网或其他网络地址（" + s + "）";
    }

    /** 「IP 归属地」列：与登录 IP 列文案区分，便于阅读 */
    public static String ipLocationLabel(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip.trim())) {
            return "来源未知";
        }
        String s = ip.trim();
        if ("127.0.0.1".equals(s) || "0:0:0:0:0:0:0:1".equals(s) || "::1".equals(s)) {
            return "本机回环（非公网）";
        }
        if (s.startsWith("192.168.") || s.startsWith("10.") || s.startsWith("172.16.")
                || s.startsWith("172.17.") || s.startsWith("172.18.") || s.startsWith("172.19.")
                || s.startsWith("172.2") || s.startsWith("172.30.") || s.startsWith("172.31.")) {
            return "机构/局域网（非公网）";
        }
        if (s.contains(":")) {
            return "IPv6 网络";
        }
        return "公网或其他（" + s + "）";
    }
}
