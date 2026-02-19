// src/main/java/xaviermc/top/hardcoreRespawn/HardcoreRespawn.java
package xaviermc.top.hardcoreRespawn;

import org.bukkit.plugin.java.JavaPlugin;
import xaviermc.top.hardcoreRespawn.commands.RespawnCommand;
import xaviermc.top.hardcoreRespawn.listeners.*;
import xaviermc.top.hardcoreRespawn.managers.PlayerDataManager;
import xaviermc.top.hardcoreRespawn.database.DatabaseManager;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

public class HardcoreRespawn extends JavaPlugin {
    private static HardcoreRespawn instance;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置文件
        saveDefaultConfig();
        MessageUtils.loadMessages();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        // 初始化数据管理器
        playerDataManager = new PlayerDataManager(this);

        // 注册命令和 TabCompleter
        RespawnCommand respawnCommand = new RespawnCommand(this);
        getCommand("respawn").setExecutor(respawnCommand);
        getCommand("respawn").setTabCompleter(respawnCommand);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MoveListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityKillListener(this), this);
        getServer().getPluginManager().registerEvents(new LowHealthListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this); // ← 新增

        getLogger().info("HardcoreRespawn 插件已启用！");
        getLogger().info("一滴血模式：" + getConfig().getBoolean("settings.one_heart.enabled", true));
        getLogger().info("等待期模式：旁观者 + 指令白名单");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("HardcoreRespawn 插件已禁用！");
    }

    public static HardcoreRespawn getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}