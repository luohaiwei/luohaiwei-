package com.lab.reservation.service;

import java.io.File;
import java.util.List;

/**
 * 数据库备份与恢复服务
 */
public interface DatabaseBackupService {

    /**
     * 执行手动备份，返回备份文件路径
     */
    String backup() throws Exception;

    /**
     * 从备份文件恢复
     */
    void restore(String backupFilePath) throws Exception;

    /**
     * 获取备份文件列表
     */
    List<File> listBackupFiles();
}
