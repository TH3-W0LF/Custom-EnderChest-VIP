package org.dark.customenderchest.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.utils.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final CustomEnderChest plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(CustomEnderChest plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setPoolName("EnderChestPool");
        
        // Otimizações para SQLite
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        createTables();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS ec_players (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "password_hash TEXT" +
                            ");")) {
                ps.execute();
            }
            
            try (Statement stmt = conn.createStatement()) {
                try { stmt.execute("ALTER TABLE ec_players ADD COLUMN failed_attempts INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE ec_players ADD COLUMN lockout_end BIGINT DEFAULT 0"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE ec_players ADD COLUMN punishment_level INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE ec_players ADD COLUMN purchased_tier INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            }
            
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS ec_pages (" +
                            "uuid VARCHAR(36), " +
                            "page_index INT, " +
                            "items_blob TEXT, " +
                            "PRIMARY KEY(uuid, page_index)" +
                            ");")) {
                ps.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar tabelas do banco de dados!", e);
        }
    }

    // ... métodos savePage e loadPage ...
    public void savePage(UUID uuid, int pageIndex, ItemStack[] items) {
        String base64 = ItemSerializer.toBase64(items);
        String query = "INSERT OR REPLACE INTO ec_pages (uuid, page_index, items_blob) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, pageIndex);
            ps.setString(3, base64);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao salvar página " + pageIndex + " para " + uuid, e);
        }
    }

    public ItemStack[] loadPage(UUID uuid, int pageIndex) {
        String query = "SELECT items_blob FROM ec_pages WHERE uuid = ? AND page_index = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, pageIndex);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String base64 = rs.getString("items_blob");
                    try { return ItemSerializer.fromBase64(base64); } catch (Exception e) { return new ItemStack[0]; }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar página " + pageIndex + " para " + uuid, e);
        }
        return new ItemStack[0];
    }

    public void setPassword(UUID uuid, String passwordHash) {
        String query = "INSERT INTO ec_players (uuid, password_hash) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET password_hash=excluded.password_hash";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao definir senha para " + uuid, e);
        }
    }
    
    public String getPasswordHash(UUID uuid) {
        String query = "SELECT password_hash FROM ec_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getString("password_hash"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
    
    public void removePassword(UUID uuid) {
        String query = "UPDATE ec_players SET password_hash = NULL WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    // Métodos de Bloqueio
    public long getLockoutEnd(UUID uuid) {
        String query = "SELECT lockout_end FROM ec_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong("lockout_end"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getPunishmentLevel(UUID uuid) {
        String query = "SELECT punishment_level FROM ec_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("punishment_level"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public int incrementFailedAttempts(UUID uuid) {
        String query = "UPDATE ec_players SET failed_attempts = failed_attempts + 1 WHERE uuid = ?";
        // Garante que a linha existe
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO ec_players (uuid) VALUES (?)")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT failed_attempts FROM ec_players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("failed_attempts"); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public void resetFailedAttempts(UUID uuid) {
        String query = "UPDATE ec_players SET failed_attempts = 0, punishment_level = 0, lockout_end = 0 WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public void applyLockout(UUID uuid, long endTime, int newLevel) {
        String query = "UPDATE ec_players SET lockout_end = ?, punishment_level = ?, failed_attempts = 0 WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, endTime);
            ps.setInt(2, newLevel);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // Métodos de Tier
    public int getPurchasedTier(UUID uuid) {
        String query = "SELECT purchased_tier FROM ec_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("purchased_tier"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void setPurchasedTier(UUID uuid, int tier) {
        String query = "INSERT INTO ec_players (uuid, purchased_tier) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET purchased_tier=excluded.purchased_tier";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, tier);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
