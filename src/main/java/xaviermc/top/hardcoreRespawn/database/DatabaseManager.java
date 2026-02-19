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
                    created_at LONG DEFAULT (strftime('%s', 'now')),
                    is_new_player BOOLEAN DEFAULT TRUE
                )
            """;

            Statement stmt = connection.createStatement();
            stmt.execute(createTableSQL);
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
                (uuid, player_name, respawn_count, death_timestamp, is_waiting, wait_duration, last_login, is_new_player)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getPlayerName());
            stmt.setInt(3, data.getRespawnCount());
            stmt.setLong(4, data.getDeathTimestamp());
            stmt.setBoolean(5, data.isWaiting());
            stmt.setLong(6, data.getWaitDuration());
            stmt.setLong(7, data.getLastLogin());
            stmt.setBoolean(8, data.isNewPlayer());

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