package xaviermc.top.hardcoreRespawn.database;

import org.bukkit.plugin.Plugin;
import xaviermc.top.hardcoreRespawn.models.PlayerData;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final Plugin plugin;
    private Connection connection;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = new File(plugin.getDataFolder(), "players.db").getPath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // 创建表
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    respawn_count INT DEFAULT 3,
                    death_timestamp LONG DEFAULT 0,
                    is_waiting BOOLEAN DEFAULT FALSE,
                    wait_duration LONG DEFAULT 86400000, -- 24小时默认
                    last_login LONG DEFAULT 0,
                    total_online_time LONG DEFAULT 0, -- 总在线时间（毫秒）
                    last_respawn_recovery LONG DEFAULT 0, -- 上次恢复复活次数的时间戳
                    max_health DOUBLE DEFAULT 20.0, -- 生命值上限
                    created_at LONG DEFAULT (strftime('%s', 'now')),
                    is_new_player BOOLEAN DEFAULT TRUE
                )
            """;

            Statement stmt = connection.createStatement();
            stmt.execute(createTableSQL);
            
            // 检查并添加缺失的字段
            // 检查total_online_time字段是否存在
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_data)");
            boolean hasTotalOnlineTime = false;
            boolean hasLastRespawnRecovery = false;
            boolean hasMaxHealth = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("total_online_time".equals(columnName)) {
                    hasTotalOnlineTime = true;
                } else if ("last_respawn_recovery".equals(columnName)) {
                    hasLastRespawnRecovery = true;
                } else if ("max_health".equals(columnName)) {
                    hasMaxHealth = true;
                }
            }
            rs.close();

            // 如果不存在total_online_time字段，添加它
            if (!hasTotalOnlineTime) {
                stmt.execute("ALTER TABLE player_data ADD COLUMN total_online_time LONG DEFAULT 0");
                plugin.getLogger().info("已添加total_online_time字段到数据库表");
            }

            // 如果不存在last_respawn_recovery字段，添加它
            if (!hasLastRespawnRecovery) {
                stmt.execute("ALTER TABLE player_data ADD COLUMN last_respawn_recovery LONG DEFAULT 0");
                plugin.getLogger().info("已添加last_respawn_recovery字段到数据库表");
            }

            // 如果不存在max_health字段，添加它
            if (!hasMaxHealth) {
                stmt.execute("ALTER TABLE player_data ADD COLUMN max_health DOUBLE DEFAULT 20.0");
                plugin.getLogger().info("已添加max_health字段到数据库表");
            }

            stmt.close();

            plugin.getLogger().info("数据库初始化成功！");
        } catch (Exception e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(java.util.UUID uuid) {
        try {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                PlayerData data = new PlayerData(uuid, rs.getString("player_name"));
                data.setRespawnCount(rs.getInt("respawn_count"));
                data.setDeathTimestamp(rs.getLong("death_timestamp"));
                data.setWaiting(rs.getBoolean("is_waiting"));
                data.setWaitDuration(rs.getLong("wait_duration"));
                data.setLastLogin(rs.getLong("last_login"));
                // 兼容旧数据库，检查是否存在 total_online_time 字段
                try {
                    data.setTotalOnlineTime(rs.getLong("total_online_time"));
                } catch (SQLException e) {
                    data.setTotalOnlineTime(0);
                }
                // 兼容旧数据库，检查是否存在 last_respawn_recovery 字段
                try {
                    data.setLastRespawnRecovery(rs.getLong("last_respawn_recovery"));
                } catch (SQLException e) {
                    data.setLastRespawnRecovery(System.currentTimeMillis());
                }
                // 兼容旧数据库，检查是否存在 max_health 字段
                try {
                    data.setMaxHealth(rs.getDouble("max_health"));
                } catch (SQLException e) {
                    // 使用配置文件中的默认值
                    double defaultMaxHealth = plugin.getConfig().getDouble("settings.default_max_health", 1.0);
                    data.setMaxHealth(defaultMaxHealth);
                }
                data.setNewPlayer(rs.getBoolean("is_new_player"));
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public PlayerData getPlayerDataByName(String name) {
        try {
            String sql = "SELECT * FROM player_data WHERE player_name = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                java.util.UUID uuid = java.util.UUID.fromString(rs.getString("uuid"));
                PlayerData data = new PlayerData(uuid, name);
                data.setRespawnCount(rs.getInt("respawn_count"));
                data.setDeathTimestamp(rs.getLong("death_timestamp"));
                data.setWaiting(rs.getBoolean("is_waiting"));
                data.setWaitDuration(rs.getLong("wait_duration"));
                data.setLastLogin(rs.getLong("last_login"));
                // 兼容旧数据库，检查是否存在 total_online_time 字段
                try {
                    data.setTotalOnlineTime(rs.getLong("total_online_time"));
                } catch (SQLException e) {
                    data.setTotalOnlineTime(0);
                }
                // 兼容旧数据库，检查是否存在 last_respawn_recovery 字段
                try {
                    data.setLastRespawnRecovery(rs.getLong("last_respawn_recovery"));
                } catch (SQLException e) {
                    data.setLastRespawnRecovery(System.currentTimeMillis());
                }
                // 兼容旧数据库，检查是否存在 max_health 字段
                try {
                    data.setMaxHealth(rs.getDouble("max_health"));
                } catch (SQLException e) {
                    // 使用配置文件中的默认值
                    double defaultMaxHealth = plugin.getConfig().getDouble("settings.default_max_health", 1.0);
                    data.setMaxHealth(defaultMaxHealth);
                }
                data.setNewPlayer(rs.getBoolean("is_new_player"));
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void savePlayerData(PlayerData data) {
        try {
            String sql = """
                INSERT OR REPLACE INTO player_data 
                (uuid, player_name, respawn_count, death_timestamp, is_waiting, wait_duration, last_login, total_online_time, last_respawn_recovery, max_health, is_new_player)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getPlayerName());
            stmt.setInt(3, data.getRespawnCount());
            stmt.setLong(4, data.getDeathTimestamp());
            stmt.setBoolean(5, data.isWaiting());
            stmt.setLong(6, data.getWaitDuration());
            stmt.setLong(7, data.getLastLogin());
            stmt.setLong(8, data.getTotalOnlineTime());
            stmt.setLong(9, data.getLastRespawnRecovery());
            stmt.setDouble(10, data.getMaxHealth());
            stmt.setBoolean(11, data.isNewPlayer());

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}