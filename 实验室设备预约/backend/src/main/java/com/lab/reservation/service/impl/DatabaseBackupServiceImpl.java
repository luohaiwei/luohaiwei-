package com.lab.reservation.service.impl;

import com.lab.reservation.service.DatabaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据库备份与恢复实现
 * 使用纯 JDBC 方式备份和恢复，不依赖 mysqldump/mysql 命令
 */
@Service
public class DatabaseBackupServiceImpl implements DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupServiceImpl.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/lab_reservation}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    private static final String BACKUP_DIR = "D:/lab-backup/";

    /** MySQL 系统辅助表（不需要备份，恢复时可能因权限问题报错） */
    private static final Set<String> MYSQL_SYSTEM_TABLES = new HashSet<>(Arrays.asList(
            "slave_master_info", "slave_relay_log_info", "slave_worker_info",
            "innodb_index_stats", "innodb_table_stats", "gtid_executed",
            "server_cost", "engine_cost", "server_variables", "time_zone_name",
            "time_zone", "time_zone_transition", "time_zone_transition_type",
            "time_zone_leap_second", "global_grants", "password_history"
    ));

    /**
     * 备份数据库到 SQL 文件
     */
    @Override
    public String backup() throws Exception {
        Path dir = Paths.get(BACKUP_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String filename = "lab_reservation_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        Path backupPath = dir.resolve(filename);

        try (Connection conn = getConnection()) {
            String dbName = extractDbName(jdbcUrl);
            // 一次性查询所有表的生成列，避免每张表都查一次
            Map<String, Set<String>> allGeneratedCols = getAllGeneratedColumnNames(conn, dbName);

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(backupPath.toFile()), StandardCharsets.UTF_8))) {

                writer.write("-- ==========================================\n");
                writer.write("-- Lab Reservation System Database Backup\n");
                writer.write("-- Backup Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("-- Note: INSERT statements exclude MySQL generated columns (e.g. device_no_active, username_active)\n");
                writer.write("-- ==========================================\n\n");
                writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

                List<String> tables = getAllTables(conn);

                for (String tableName : tables) {
                    if (MYSQL_SYSTEM_TABLES.contains(tableName.toLowerCase())) {
                        continue;
                    }
                    Set<String> generatedCols = new HashSet<>(
                            allGeneratedCols.getOrDefault(tableName.toLowerCase(), Collections.emptySet())
                    );
                    generatedCols.addAll(getGeneratedColumnNamesByShow(conn, tableName));

                    writer.write("-- ----------------------------\n");
                    writer.write("-- Table: " + tableName + "\n");
                    writer.write("-- ----------------------------\n");

                    writer.write("DROP TABLE IF EXISTS `" + tableName + "`;\n\n");

                    String createSQL = getCreateTableSQL(conn, tableName);
                    writer.write(createSQL + ";\n\n");

                    writeTableData(writer, conn, tableName, generatedCols);

                    writer.write("\n");
                }

                writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            }
        }

        log.info("备份成功: {}", backupPath);
        return backupPath.toString();
    }

    /**
     * 一次性查询所有表的生成列（MySQL 5.7+）。
     * Map: tableName -> Set<columnName>
     */
    private Map<String, Set<String>> getAllGeneratedColumnNames(Connection conn, String dbName) {
        Map<String, Set<String>> result = new HashMap<>();
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND IS_GENERATED_COLUMN = 'ALWAYS'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String table = rs.getString(1);
                    String col = rs.getString(2);
                    result.computeIfAbsent(table.toLowerCase(), k -> new HashSet<>()).add(col.toLowerCase());
                }
            }
        } catch (SQLException e) {
            log.warn("无法查询生成列（MySQL 版本可能过低）: {}", e.getMessage());
        }
        log.info("数据库 [{}] 查询到 {} 个表含有生成列: {}", dbName, result.size(), result);
        return result;
    }

    /**
     * 兜底方案：通过 SHOW FULL COLUMNS 识别生成列（兼容某些 INFORMATION_SCHEMA 返回异常的场景）。
     */
    private Set<String> getGeneratedColumnNamesByShow(Connection conn, String tableName) {
        Set<String> generated = new HashSet<>();
        String sql = "SHOW FULL COLUMNS FROM `" + tableName + "`";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String field = rs.getString("Field");
                String extra = rs.getString("Extra");
                if (field != null && extra != null && extra.toUpperCase().contains("GENERATED")) {
                    generated.add(field.toLowerCase());
                }
            }
        } catch (SQLException e) {
            log.debug("SHOW FULL COLUMNS 检测生成列失败, table={}, msg={}", tableName, e.getMessage());
        }
        return generated;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private List<String> getAllTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = conn.createStatement().executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private String getCreateTableSQL(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return "";
    }

    /**
     * 写入表数据（使用已传入的生成列集合，日期时间格式化为 MySQL 字面量）。
     */
    private void writeTableData(BufferedWriter writer, Connection conn, String tableName, Set<String> generatedCols)
            throws SQLException, IOException {
        List<String> columns = new ArrayList<>();
        Statement colStmt = conn.createStatement();
        try {
            ResultSet colRs = colStmt.executeQuery("SELECT * FROM `" + tableName + "` WHERE 1=0");
            ResultSetMetaData metaData = colRs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String col = metaData.getColumnName(i);
                if (!generatedCols.contains(col.toLowerCase())) {
                    columns.add(col);
                }
            }
            colRs.close();
        } finally {
            colStmt.close();
        }

            if (columns.isEmpty()) {
                log.warn("表 {} 所有列都是生成列，跳过数据写入", tableName);
                writer.write("-- No columns (all generated?), skipping\n");
                return;
            }

            log.debug("表 {} 跳过生成列: {}", tableName, generatedCols);
            String selectList = columns.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
            StringBuilder sb = new StringBuilder();
        int rowCount = 0;

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + selectList + " FROM `" + tableName + "`");
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                rowCount++;
                sb.setLength(0);
                sb.append("INSERT INTO `").append(tableName).append("` (");
                for (int i = 0; i < columns.size(); i++) {
                    sb.append("`").append(columns.get(i)).append("`");
                    if (i < columns.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(") VALUES (");
                for (int i = 0; i < columns.size(); i++) {
                    appendSqlLiteral(sb, rs.getObject(i + 1));
                    if (i < columns.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(");\n");
                writer.write(sb.toString());
            }
        }

        if (rowCount == 0) {
            writer.write("-- Empty table: DROP+CREATE already handled above\n");
        }
    }

    private void appendSqlLiteral(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("NULL");
            return;
        }
        if (value instanceof Number) {
            sb.append(value);
            return;
        }
        if (value instanceof byte[]) {
            sb.append("0x").append(bytesToHex((byte[]) value));
            return;
        }
        if (value instanceof java.sql.Timestamp) {
            sb.append("'").append(((java.sql.Timestamp) value).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("'");
            return;
        }
        if (value instanceof java.sql.Date) {
            sb.append("'").append(((java.sql.Date) value).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("'");
            return;
        }
        if (value instanceof java.sql.Time) {
            sb.append("'").append(value.toString()).append("'");
            return;
        }
        if (value instanceof java.time.LocalDateTime) {
            sb.append("'").append(((java.time.LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("'");
            return;
        }
        if (value instanceof java.time.LocalDate) {
            sb.append("'").append(((java.time.LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("'");
            return;
        }
        if (value instanceof java.time.LocalTime) {
            sb.append("'").append(((java.time.LocalTime) value).format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("'");
            return;
        }
        if (value instanceof java.util.Date && !(value instanceof java.sql.Date) && !(value instanceof java.sql.Timestamp)) {
            sb.append("'").append(new java.sql.Timestamp(((java.util.Date) value).getTime()).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("'");
            return;
        }
        if (value instanceof Boolean) {
            sb.append(((Boolean) value) ? "1" : "0");
            return;
        }
        sb.append("'").append(escapeSQLString(value.toString())).append("'");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String escapeSQLString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }

    /**
     * SQL 解析器：按语句分割（忽略字符串和引号内的分号）
     */
    private static class SqlParser {
        private final StringBuilder buf = new StringBuilder();
        private boolean inSingleQuote = false;
        private boolean inDoubleQuote = false;
        private boolean inBacktick = false;
        private boolean inEscape = false;

        void append(char c) {
            buf.append(c);
        }

        void append(String s) {
            buf.append(s);
        }

        /**
         * 如果当前积累的内容是一条完整的 SQL 语句（遇到不在引号内的分号），返回该语句并清空缓冲区。
         * 返回 null 表示还不是完整语句。
         */
        String checkComplete() {
            for (int i = 0; i < buf.length(); i++) {
                char c = buf.charAt(i);

                if (inEscape) {
                    inEscape = false;
                    continue;
                }

                if (c == '\\') {
                    inEscape = true;
                    continue;
                }

                if (inSingleQuote) {
                    if (c == '\'') {
                        inSingleQuote = false;
                    }
                    continue;
                }
                if (inDoubleQuote) {
                    if (c == '"') {
                        inDoubleQuote = false;
                    }
                    continue;
                }
                if (inBacktick) {
                    if (c == '`') {
                        inBacktick = false;
                    }
                    continue;
                }

                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '`') {
                    inBacktick = true;
                } else if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                    String stmt = buf.substring(0, i);
                    buf.delete(0, i + 1);
                    inEscape = false;
                    return stmt.trim();
                }
            }
            return null;
        }

        String remainder() {
            inEscape = false;
            inSingleQuote = false;
            inDoubleQuote = false;
            inBacktick = false;
            return buf.toString().trim();
        }
    }

    @Override
    public void restore(String backupFilePath) throws Exception {
        Path path = Paths.get(backupFilePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("备份文件不存在: " + backupFilePath);
        }

        log.info("开始恢复数据库: {}", backupFilePath);
        long startTime = System.currentTimeMillis();
        int executedCount = 0;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(300);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {

                    SqlParser parser = new SqlParser();
                    int lineNumber = 0;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        lineNumber++;
                        String trimmed = line.trim();

                        if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                            continue;
                        }

                        parser.append(trimmed);
                        parser.append(" ");

                        String sql;
                        while ((sql = parser.checkComplete()) != null) {
                            executeStatement(stmt, sql, lineNumber);
                            executedCount++;
                        }
                    }

                    String remainder = parser.remainder();
                    if (!remainder.isEmpty()) {
                        executeStatement(stmt, remainder, lineNumber);
                        executedCount++;
                    }
                }

                conn.commit();
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("恢复完成: 执行 {} 条语句, 耗时 {}ms", executedCount, elapsed);

            } catch (SQLException e) {
                log.error("恢复执行失败: {}", e.getMessage());
                try { conn.rollback(); } catch (Exception ignored) {}
                throw new Exception("执行 SQL 失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 执行单条 SQL 语句。
     * - "doesn't exist" / "Unknown table"：来自 DROP IF EXISTS，静默跳过
     * - 其他所有错误：全部上报，不静默吞掉
     */
    private void executeStatement(Statement stmt, String sql, int lineNumber) throws SQLException {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null) {
                String upper = msg.toUpperCase();
                // DROP IF EXISTS 可能的后续报错：表不存在，直接跳过
                if (upper.contains("DON'T EXIST") || upper.contains("UNKNOWN TABLE")) {
                    return;
                }
            }
            // 所有其他错误（包括含生成列的旧备份）全部上报
            String sqlPreview = sql.length() > 200 ? sql.substring(0, 200) + "..." : sql;
            throw new SQLException("行 ~" + lineNumber + ": " + (msg != null ? msg : e) + " | SQL: " + sqlPreview, e);
        }
    }

    private String extractDbName(String url) {
        int idx = url.lastIndexOf('/');
        if (idx < 0) {
            return "lab_reservation";
        }
        String after = url.substring(idx + 1);
        int q = after.indexOf('?');
        return q > 0 ? after.substring(0, q) : after;
    }

    @Override
    public List<File> listBackupFiles() {
        List<File> list = new ArrayList<>();
        Path dir = Paths.get(BACKUP_DIR);
        if (!Files.exists(dir)) return list;
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".sql"))
                    .map(Path::toFile)
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .forEach(list::add);
        } catch (IOException e) {
            log.warn("列出备份文件失败: {}", e.getMessage());
        }
        return list;
    }
}
