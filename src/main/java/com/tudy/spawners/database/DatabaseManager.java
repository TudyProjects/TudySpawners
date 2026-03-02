package com.tudy.spawners.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public DatabaseManager(String type, String host, int port, String database, String user, String password, File dataFolder) {
        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("sqlite")) {
            File dbFile = new File(dataFolder, "spawners.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setPoolName("TudySpawners-SQLite");
            config.setMaximumPoolSize(1);
        } else {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(10);
        }

        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        initTables();
    }

    private void initTables() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS player_spawners (
                location_id VARCHAR(100) PRIMARY KEY,
                entity_type VARCHAR(32) NOT NULL,
                stack_size INT NOT NULL DEFAULT 1,
                storage_data TEXT,
                stored_xp INT DEFAULT 0
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);

            // Eski veritabanı dosyasına sahip olanlar için XP sütununu otomatik ekleme (ALTER TABLE)
            try {
                stmt.execute("ALTER TABLE player_spawners ADD COLUMN stored_xp INT DEFAULT 0");
            } catch (SQLException ignored) {
                // Sütun zaten varsa hata fırlatır, görmezden gelir.
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }
}