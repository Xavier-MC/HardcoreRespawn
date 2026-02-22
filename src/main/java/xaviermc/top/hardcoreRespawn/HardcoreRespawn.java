package xaviermc.top.hardcoreRespawn;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xaviermc.top.hardcoreRespawn.commands.RespawnCommand;
import xaviermc.top.hardcoreRespawn.listeners.*;
import xaviermc.top.hardcoreRespawn.managers.PlayerDataManager;
import xaviermc.top.hardcoreRespawn.database.DatabaseManager;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

import java.util.List;

public class HardcoreRespawn extends JavaPlugin {
    private static HardcoreRespawn instance;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置文件
        checkConfigVersion();
        MessageUtils.loadMessages();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        // 初始化数据管理器
        playerDataManager = new PlayerDataManager(this);
        
        // 启动在线时间检查任务
        playerDataManager.startOnlineTimeCheckTask();

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

        getLogger().info(MessageUtils.getLogMessage("log_plugin_enabled"));
        getLogger().info(MessageUtils.getLogMessage("log_one_heart_mode", "status", getConfig().getBoolean("settings.one_heart.enabled", true)));

        // 解析等待期模式
        int waitTimeMode = getConfig().getInt("settings.wait_time_mode", 3);
        String modeName = MessageUtils.getLogMessage("log_observer_mode");
        if (waitTimeMode == 0) modeName = MessageUtils.getLogMessage("log_survival_mode");
        else if (waitTimeMode == 2) modeName = MessageUtils.getLogMessage("log_adventure_mode");
        // 检查指令白名单状态
        List<String> commandWhitelist = getConfig().getStringList("settings.command_whitelist");
        String whitelistStatus = commandWhitelist.isEmpty() ? MessageUtils.getLogMessage("log_whitelist_disabled") : MessageUtils.getLogMessage("log_whitelist_enabled");
        
        getLogger().info(MessageUtils.getLogMessage("log_wait_time_mode", "mode", modeName, "whitelist_status", whitelistStatus));

        getLogger().info(MessageUtils.getLogMessage("log_authme_supported", "status", getConfig().getBoolean("settings.authme_supported", false)));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info(MessageUtils.getLogMessage("log_plugin_disabled"));
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

    /**
     * 检测玩家是否处于未登录状态
     * @param player 玩家对象
     * @return 如果玩家未登录返回true，否则返回false
     */
    public static boolean isPlayerLoggedOut(Player player) {
        // 检查配置是否启用了AuthMe支持
        if (!getInstance().getConfig().getBoolean("settings.authme_supported", false)) {
            return false;
        }

        // 软依赖检测：检查服务端是否加载了AuthMe插件
        try {
            // 尝试获取AuthMe插件实例
            org.bukkit.plugin.Plugin authMePlugin = getInstance().getServer().getPluginManager().getPlugin("AuthMe");
            if (authMePlugin != null && authMePlugin.isEnabled()) {
                // 使用反射调用AuthMe API，避免硬依赖
                Class<?> authMeApiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
                Object apiInstance = authMeApiClass.getMethod("getInstance").invoke(null);
                boolean isAuthenticated = (boolean) authMeApiClass.getMethod("isAuthenticated", Player.class).invoke(apiInstance, player);
                return !isAuthenticated;
            }
        } catch (Exception e) {
            // 如果AuthMe插件不存在或API调用失败，默认返回false
            getInstance().getLogger().fine(MessageUtils.getLogMessage("log_authme_not_found"));
        }

        return false;
    }

    /**
     * 检查配置文件版本，如果版本不匹配则替换为默认配置
     */
    private void checkConfigVersion() {
        // 保存默认配置文件到插件目录（如果不存在）
        saveDefaultConfig();
        
        // 获取当前配置文件的版本
        String currentVersion = getConfig().getString("version", "0.0");
        
        // 获取默认配置文件的版本
        String defaultVersion = "0.0";
        try {
            // 获取默认配置文件资源
            java.io.InputStream resourceStream = getResource("config.yml");
            if (resourceStream != null) {
                org.bukkit.configuration.file.FileConfiguration defaultConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(resourceStream)
                );
                defaultVersion = defaultConfig.getString("version", "0.0");
                resourceStream.close();
            } else {
                getLogger().severe(MessageUtils.getLogMessage("log_config_resource_error"));
                return;
            }
        } catch (Exception e) {
            getLogger().severe(MessageUtils.getLogMessage("log_config_load_error", "error", e.getMessage()));
            e.printStackTrace();
            return;
        }
        
        // 比较版本
        if (!currentVersion.equals(defaultVersion)) {
            getLogger().warning(MessageUtils.getLogMessage("log_config_version_mismatch", "current", currentVersion, "default", defaultVersion));
            getLogger().warning(MessageUtils.getLogMessage("log_config_replaced", "current", currentVersion, "default", defaultVersion));
            
            // 重命名旧配置文件为config-old.yml
            java.io.File oldConfigFile = new java.io.File(getDataFolder(), "config.yml");
            java.io.File backupConfigFile = new java.io.File(getDataFolder(), "config-old.yml");
            
            // 如果备份文件已存在，先删除
            if (backupConfigFile.exists()) {
                backupConfigFile.delete();
            }
            
            if (oldConfigFile.exists()) {
                if (!oldConfigFile.renameTo(backupConfigFile)) {
                getLogger().severe(MessageUtils.getLogMessage("log_config_rename_error"));
                return;
            }
            getLogger().info(MessageUtils.getLogMessage("log_config_backup"));
            }
            
            // 保存新的默认配置文件
            saveDefaultConfig();
            reloadConfig();
            
            getLogger().info(MessageUtils.getLogMessage("log_config_updated", "version", defaultVersion));
        }
    }
}