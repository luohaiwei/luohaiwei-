package com.lab.reservation.service;

import java.io.OutputStream;

/**
 * 统计数据导出服务
 */
public interface StatisticsExportService {

    /**
     * 导出统计报表为Excel
     */
    void exportExcel(OutputStream out) throws Exception;

    /**
     * 导出统计报表为PDF
     */
    void exportPdf(OutputStream out) throws Exception;

    /**
     * 按统计类型导出PDF（type=null 时导出全量）
     */
    void exportPdfByType(OutputStream out, String type) throws Exception;
}
