package com.zsj.meetingagent.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * 数据库轻量迁移器。
 * 负责补齐阶段演进中新增但旧本地库可能缺失的字段，避免用户已有 MySQL 表结构导致启动或上传失败。
 */
@Component
public class DatabaseSchemaMigrationRunner implements ApplicationRunner {

    private static final String TABLE_UPLOADED_FILE = "uploaded_file";
    private static final String COLUMN_FILE_BYTES = "file_bytes";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigrationRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!hasColumn(TABLE_UPLOADED_FILE, COLUMN_FILE_BYTES)) {
            addUploadedFileBytesColumn();
        }
    }

    private boolean hasColumn(String tableName, String columnName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void addUploadedFileBytesColumn() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (databaseName.contains("mysql")) {
                jdbcTemplate.execute("""
                        ALTER TABLE uploaded_file
                        ADD COLUMN file_bytes LONGBLOB COMMENT '原始文件二进制内容，用于前端简历预览'
                        AFTER document_type
                        """);
                return;
            }
            jdbcTemplate.execute("ALTER TABLE uploaded_file ADD COLUMN file_bytes BLOB");
        }
    }
}
