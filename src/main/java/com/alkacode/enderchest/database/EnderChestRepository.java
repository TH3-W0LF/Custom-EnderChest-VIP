package com.alkacode.enderchest.database;

import com.alkacode.core.api.DatabaseProvider;
import com.alkacode.core.database.AbstractRepository;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unico ponto de acesso ao banco do AlkaEnderChest, sobre o {@link DatabaseProvider}
 * do AlkaCore (HikariCP) - substitui o antigo DatabaseManager (conta/senha, SQLite
 * proprio) e o EnderChestStorage (itens por pagina, YAML/SQLite/MySQL com JDBC
 * direto). Uma linha por jogador em vez de uma linha por (jogador, pagina): todas as
 * paginas de um jogador sao serializadas juntas na coluna `pages`.
 *
 * O cache local (Map por pagina) evita releitura/reescrita do blob inteiro a cada
 * pagina - so a pagina alterada muda no cache, mas o blob completo e reserializado no
 * save (mesmo racional do antigo EnderChestStorage: pagina vazia = sem entrada no
 * mapa, nunca "compacta" as demais paginas).
 */
public final class EnderChestRepository extends AbstractRepository {

    public static final int PAGE_SIZE = 45;

    private final Logger logger;
    private final Map<UUID, Map<Integer, ItemStack[]>> pageCache = new ConcurrentHashMap<>();

    public EnderChestRepository(DatabaseProvider db, Logger logger) {
        super(db);
        this.logger = logger;
        createTable();
    }

    private void createTable() {
        try (Connection conn = db.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS alka_ec_data (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        pages TEXT,
                        current_page INT DEFAULT 0,
                        password_hash VARCHAR(255),
                        tier INT DEFAULT 0,
                        max_tier INT DEFAULT 0,
                        failed_attempts INT DEFAULT 0,
                        punishment_level INT DEFAULT 0,
                        lockout_until BIGINT DEFAULT 0
                    )
                    """);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar tabela alka_ec_data", e);
        }
    }

    private boolean rowExists(UUID uuid) {
        String sql = "SELECT 1 FROM alka_ec_data WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao verificar linha de " + uuid, e);
            return false;
        }
    }

    private void ensureRow(UUID uuid) {
        if (rowExists(uuid)) {
            return;
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO alka_ec_data (player_uuid) VALUES (?)")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao criar linha de " + uuid, e);
        }
    }

    private void updateColumn(UUID uuid, String column, Object value) {
        ensureRow(uuid);
        String sql = "UPDATE alka_ec_data SET " + column + " = ? WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar " + column + " de " + uuid, e);
        }
    }

    private Object readColumn(UUID uuid, String column) {
        String sql = "SELECT " + column + " FROM alka_ec_data WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(column);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao ler " + column + " de " + uuid, e);
        }
        return null;
    }

    // --------------------------------------------------------------- conta/senha

    public String getPasswordHash(UUID uuid) {
        Object value = readColumn(uuid, "password_hash");
        return value == null ? null : value.toString();
    }

    public void setPassword(UUID uuid, String passwordHash) {
        updateColumn(uuid, "password_hash", passwordHash);
    }

    public void removePassword(UUID uuid) {
        ensureRow(uuid);
        String sql = "UPDATE alka_ec_data SET password_hash = NULL WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao remover senha de " + uuid, e);
        }
    }

    public long getLockoutUntil(UUID uuid) {
        Object value = readColumn(uuid, "lockout_until");
        return value == null ? 0L : ((Number) value).longValue();
    }

    public int getPunishmentLevel(UUID uuid) {
        Object value = readColumn(uuid, "punishment_level");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int incrementFailedAttempts(UUID uuid) {
        ensureRow(uuid);
        String sql = "UPDATE alka_ec_data SET failed_attempts = failed_attempts + 1 WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao incrementar tentativas de " + uuid, e);
        }
        Object value = readColumn(uuid, "failed_attempts");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public void resetFailedAttempts(UUID uuid) {
        ensureRow(uuid);
        String sql = "UPDATE alka_ec_data SET failed_attempts = 0, punishment_level = 0, lockout_until = 0 WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao resetar tentativas de " + uuid, e);
        }
    }

    public void applyLockout(UUID uuid, long endTime, int newLevel) {
        ensureRow(uuid);
        String sql = "UPDATE alka_ec_data SET lockout_until = ?, punishment_level = ?, failed_attempts = 0 WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, endTime);
            ps.setInt(2, newLevel);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao aplicar bloqueio em " + uuid, e);
        }
    }

    // --------------------------------------------------------------------- tier

    public int getTier(UUID uuid) {
        Object value = readColumn(uuid, "tier");
        return value == null ? 0 : ((Number) value).intValue();
    }

    /** `max_tier` nunca diminui - registra o maior tier ja desbloqueado, mesmo que `tier` volte a cair no futuro. */
    public void setTier(UUID uuid, int tier) {
        ensureRow(uuid);
        int currentMax = readMaxTier(uuid);
        String sql = "UPDATE alka_ec_data SET tier = ?, max_tier = ? WHERE player_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tier);
            ps.setInt(2, Math.max(currentMax, tier));
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao definir tier de " + uuid, e);
        }
    }

    private int readMaxTier(UUID uuid) {
        Object value = readColumn(uuid, "max_tier");
        return value == null ? 0 : ((Number) value).intValue();
    }

    // ------------------------------------------------------------------- paginas

    public ItemStack[] loadPage(UUID uuid, int page) {
        Map<Integer, ItemStack[]> pages = pagesOf(uuid);
        ItemStack[] items = pages.get(page);
        return items != null ? items.clone() : new ItemStack[PAGE_SIZE];
    }

    public void savePage(UUID uuid, int page, ItemStack[] items) {
        ItemStack[] fixed = new ItemStack[PAGE_SIZE];
        System.arraycopy(items, 0, fixed, 0, Math.min(items.length, PAGE_SIZE));

        Map<Integer, ItemStack[]> pages = pagesOf(uuid);
        pages.put(page, fixed.clone());

        String sql = upsert("alka_ec_data",
                new String[]{"player_uuid", "pages", "current_page"},
                new String[]{"player_uuid"});
        try {
            execute(sql, ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, serializePages(pages));
                ps.setInt(3, page);
            });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao salvar paginas de " + uuid, e);
        }
    }

    /** Descarta o cache do jogador (chamar no quit) para nao acumular memoria indefinidamente. */
    public void clearCache(UUID uuid) {
        pageCache.remove(uuid);
    }

    private Map<Integer, ItemStack[]> pagesOf(UUID uuid) {
        return pageCache.computeIfAbsent(uuid, this::loadPagesFromDb);
    }

    private Map<Integer, ItemStack[]> loadPagesFromDb(UUID uuid) {
        Object value = readColumn(uuid, "pages");
        if (value == null || value.toString().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return deserializePages(value.toString());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar paginas de " + uuid, e);
            return new HashMap<>();
        }
    }

    // ---------------------------------------------------------------- serial

    private String serializePages(Map<Integer, ItemStack[]> pages) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            // TreeMap para ordem deterministica - facilita depurar o blob manualmente se precisar.
            Map<Integer, ItemStack[]> ordered = new TreeMap<>(pages);
            dataOutput.writeInt(ordered.size());
            for (Map.Entry<Integer, ItemStack[]> entry : ordered.entrySet()) {
                dataOutput.writeInt(entry.getKey());
                for (ItemStack item : entry.getValue()) {
                    dataOutput.writeObject(item);
                }
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar paginas do EnderChest.", e);
        }
    }

    private Map<Integer, ItemStack[]> deserializePages(String data) throws IOException {
        Map<Integer, ItemStack[]> pages = new HashMap<>();
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int pageCount = dataInput.readInt();
            for (int i = 0; i < pageCount; i++) {
                int pageIndex = dataInput.readInt();
                ItemStack[] items = new ItemStack[PAGE_SIZE];
                for (int slot = 0; slot < PAGE_SIZE; slot++) {
                    items[slot] = (ItemStack) dataInput.readObject();
                }
                pages.put(pageIndex, items);
            }
            dataInput.close();
            return pages;
        } catch (ClassNotFoundException e) {
            throw new IOException("Classe nao encontrada ao desserializar paginas.", e);
        }
    }
}
