package xaviermc.top.hardcoreRespawn.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;
import xaviermc.top.hardcoreRespawn.models.PlayerData;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;
import xaviermc.top.hardcoreRespawn.utils.TimeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {
    private final HardcoreRespawn plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public PlayerDataManager(HardcoreRespawn plugin) {
        this.plugin = plugin;
        startPeriodicSaveTask();
    }

    public void loadPlayerData(Player player) {
        CompletableFuture.supplyAsync(() -> {
            return plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        }).thenAccept(data -> {
            if (data != null) {
                // 更新最后登录时间
                data.setLastLogin(System.currentTimeMillis());
                // 检查离线期间是否需要恢复复活次数
                checkOfflineRespawnRecovery(player, data);
                playerDataMap.put(player.getUniqueId(), data);
                // 应用保存的生命值上限
                applySavedMaxHealth(player, data);
                // 确保当前生命值为最大生命值
                if (player.getHealth() != data.getMaxHealth()) {
                    player.setHealth(data.getMaxHealth());
                }
                // 保存玩家数据（包括可能从旧数据库更新的生命值上限）
                plugin.getDatabaseManager().savePlayerData(data);
            } else {
                // 创建新玩家数据
                PlayerData newData = new PlayerData(player.getUniqueId(), player.getName());
                // 设置默认生命值上限
                double defaultMaxHealth = plugin.getConfig().getDouble("settings.default_max_health", 1.0);
                newData.setMaxHealth(defaultMaxHealth);
                playerDataMap.put(player.getUniqueId(), newData);
                // 应用默认生命值上限
                applySavedMaxHealth(player, newData);
                // 确保当前生命值为最大生命值
                player.setHealth(newData.getMaxHealth());
                // 保存玩家数据
                plugin.getDatabaseManager().savePlayerData(newData);
            }
        });
    }

    public void initializeNewPlayer(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.isNewPlayer()) {
            int defaultRespawnCount = plugin.getConfig().getInt("settings.default_respawn_count", 3);
            data.setRespawnCount(defaultRespawnCount); // 给予初始复活机会
            
            // 设置默认生命值上限
            double defaultMaxHealth = plugin.getConfig().getDouble("settings.default_max_health", 2.0);
            data.setMaxHealth(defaultMaxHealth);
            
            data.setNewPlayer(false);
            plugin.getDatabaseManager().savePlayerData(data);
            
            // 新玩家首次加入时应用一滴血模式
            if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
                applyOneHeartMode(player);
            } else {
                // 应用默认生命值上限
                applySavedMaxHealth(player, data);
            }
        }
    }

    public int getPlayerRespawnCount(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        return data != null ? data.getRespawnCount() : 0;
    }

    public void addRespawnCount(Player player, int count) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setRespawnCount(data.getRespawnCount() + count);
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }

    public void useRespawnCount(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.getRespawnCount() > 0) {
            data.setRespawnCount(data.getRespawnCount() - 1);
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }

    public boolean isInWaitingPeriod(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        return data != null && data.isWaiting();
    }

    public void startWaitingPeriod(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            long waitTimeHours = plugin.getConfig().getLong("settings.wait_time.hours", 24);
            long waitTimeMinutes = plugin.getConfig().getLong("settings.wait_time.minutes", 0);
            long waitTimeMillis = (waitTimeHours * 60 + waitTimeMinutes) * 60 * 1000; // 转换为毫秒

            data.setDeathTimestamp(System.currentTimeMillis());
            data.setWaiting(true);
            data.setWaitDuration(waitTimeMillis);
            plugin.getDatabaseManager().savePlayerData(data);

            createBossBar(player);
        }
    }

    public void resumeWaitingPeriod(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.isWaiting()) {
            createBossBar(player);
        }
    }

    private void createBossBar(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null || !data.isWaiting()) return;

        // 移除现有的BossBar（如果存在）
        if (activeBossBars.containsKey(player.getUniqueId())) {
            activeBossBars.get(player.getUniqueId()).removeAll();
        }

        BarColor barColor = BarColor.valueOf(plugin.getConfig().getString("settings.bossbar.color", "RED"));
        BarStyle barStyle = BarStyle.valueOf(plugin.getConfig().getString("settings.bossbar.style", "SOLID"));

        BossBar bossBar = Bukkit.createBossBar("", barColor, barStyle);
        bossBar.addPlayer(player);
        activeBossBars.put(player.getUniqueId(), bossBar);

        // 启动倒计时任务
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData currentData = playerDataMap.get(player.getUniqueId());
                if (currentData == null || !currentData.isWaiting()) {
                    // 停止倒计时
                    this.cancel();
                    if (activeBossBars.containsKey(player.getUniqueId())) {
                        activeBossBars.get(player.getUniqueId()).removeAll();
                        activeBossBars.remove(player.getUniqueId());
                    }
                    return;
                }

                long currentTime = System.currentTimeMillis();
                long timeLeft = currentData.getTimeUntilRelease(currentTime);

                if (timeLeft <= 0) {
                    // 时间到了，结束等待期
                    endWaitingPeriod(player);
                    this.cancel();
                    if (activeBossBars.containsKey(player.getUniqueId())) {
                        activeBossBars.get(player.getUniqueId()).removeAll();
                        activeBossBars.remove(player.getUniqueId());
                    }
                    return;
                }

                // 更新BossBar
                String title = MessageUtils.getColoredMessage(
                        plugin.getPlayerDataManager().getMessage("bossbar_title")
                                .replace("{time}", TimeUtils.formatTime(timeLeft))
                );
                float progress = (float) (1.0 - (double) timeLeft / currentData.getWaitDuration());

                bossBar.setTitle(title);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // 每秒更新一次
    }


    public void showInfo(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null) {
            player.sendMessage(getMessage("data_not_loaded"));
            return;
        }

        player.sendMessage(MessageUtils.getColoredMessage("&a=== 复活信息 ==="));
        player.sendMessage(getMessage("info_respawn_count")
                .replace("{count}", String.valueOf(data.getRespawnCount())));
        // 显示生命值上限信息
        player.sendMessage(MessageUtils.getColoredMessage("&a生命值上限: " + data.getMaxHealth() + " (" + (data.getMaxHealth() / 2) + " 颗心)"));

        // 显示在线时间信息
        if (plugin.getConfig().getBoolean("settings.online_time_reward.enabled", true)) {
            long requiredHours = plugin.getConfig().getLong("settings.online_time_reward.required_time.hours", 24);
            long requiredMinutes = plugin.getConfig().getLong("settings.online_time_reward.required_time.minutes", 0);
            long requiredMillis = (requiredHours * 60 + requiredMinutes) * 60 * 1000;
            
            long totalMillis = data.getTotalOnlineTime();
            long totalHours = totalMillis / (60 * 60 * 1000);
            long totalMinutes = (totalMillis % (60 * 60 * 1000)) / (60 * 1000);
            
            long remainingMillis = requiredMillis - (totalMillis % requiredMillis);
            long remainingHours = remainingMillis / (60 * 60 * 1000);
            long remainingMinutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000);
            
            player.sendMessage(MessageUtils.getColoredMessage("&a累计在线时间: " + totalHours + " 小时 " + totalMinutes + " 分钟"));
            player.sendMessage(MessageUtils.getColoredMessage("&a距离下次获得复活机会: " + remainingHours + " 小时 " + remainingMinutes + " 分钟"));
        }

        if (data.isWaiting()) {
            long timeLeft = data.getTimeUntilRelease(System.currentTimeMillis());
            player.sendMessage(getMessage("info_waiting_time_left")
                    .replace("{time}", TimeUtils.formatTime(timeLeft)));
        } else {
            player.sendMessage(getMessage("info_not_waiting"));
        }
    }

    public void adminAdd(String playerName, int amount, org.bukkit.command.CommandSender sender) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            PlayerData data = playerDataMap.get(targetPlayer.getUniqueId());
            if (data != null) {
                data.setRespawnCount(data.getRespawnCount() + amount);
                plugin.getDatabaseManager().savePlayerData(data);

                targetPlayer.sendMessage(getMessage("admin_respawn_count_added")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{total}", String.valueOf(data.getRespawnCount())));

                sender.sendMessage(getMessage("admin_respawn_count_added_console")
                        .replace("{player}", playerName)
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{total}", String.valueOf(data.getRespawnCount())));
            }
        } else {
            // 玩家不在线，直接从数据库更新
            CompletableFuture.supplyAsync(() -> {
                PlayerData data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
                if (data != null) {
                    data.setRespawnCount(data.getRespawnCount() + amount);
                    plugin.getDatabaseManager().savePlayerData(data);
                    return data;
                }
                return null;
            }).thenAccept(data -> {
                if (data != null) {
                    sender.sendMessage(getMessage("admin_respawn_count_added_console")
                            .replace("{player}", playerName)
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{total}", String.valueOf(data.getRespawnCount())));
                } else {
                    sender.sendMessage(getMessage("player_not_found"));
                }
            });
        }
    }

    public void adminSet(String playerName, int amount, org.bukkit.command.CommandSender sender) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            PlayerData data = playerDataMap.get(targetPlayer.getUniqueId());
            if (data != null) {
                data.setRespawnCount(amount);
                plugin.getDatabaseManager().savePlayerData(data);

                targetPlayer.sendMessage(getMessage("admin_respawn_count_set")
                        .replace("{amount}", String.valueOf(amount)));

                sender.sendMessage(getMessage("admin_respawn_count_set_console")
                        .replace("{player}", playerName)
                        .replace("{amount}", String.valueOf(amount)));
            }
        } else {
            // 玩家不在线，直接从数据库更新
            CompletableFuture.supplyAsync(() -> {
                PlayerData data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
                if (data != null) {
                    data.setRespawnCount(amount);
                    plugin.getDatabaseManager().savePlayerData(data);
                    return data;
                }
                return null;
            }).thenAccept(data -> {
                if (data != null) {
                    sender.sendMessage(getMessage("admin_respawn_count_set_console")
                            .replace("{player}", playerName)
                            .replace("{amount}", String.valueOf(amount)));
                } else {
                    sender.sendMessage(getMessage("player_not_found"));
                }
            });
        }
    }

    public void adminReset(String playerName, org.bukkit.command.CommandSender sender) {
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer != null) {
            PlayerData data = playerDataMap.get(targetPlayer.getUniqueId());
            if (data != null) {
                data.setWaiting(false);
                data.setDeathTimestamp(0);
                plugin.getDatabaseManager().savePlayerData(data);

                // 移除BossBar
                if (activeBossBars.containsKey(targetPlayer.getUniqueId())) {
                    activeBossBars.get(targetPlayer.getUniqueId()).removeAll();
                    activeBossBars.remove(targetPlayer.getUniqueId());
                }

                // 恢复玩家状态
                targetPlayer.setGameMode(GameMode.SURVIVAL);

                targetPlayer.sendMessage(getMessage("admin_reset_player"));
                sender.sendMessage(getMessage("admin_reset_player_console")
                        .replace("{player}", playerName));
            }
        } else {
            // 玩家不在线，直接从数据库更新
            CompletableFuture.supplyAsync(() -> {
                PlayerData data = plugin.getDatabaseManager().getPlayerDataByName(playerName);
                if (data != null) {
                    data.setWaiting(false);
                    data.setDeathTimestamp(0);
                    plugin.getDatabaseManager().savePlayerData(data);
                    return data;
                }
                return null;
            }).thenAccept(data -> {
                if (data != null) {
                    sender.sendMessage(getMessage("admin_reset_player_console")
                            .replace("{player}", playerName));
                } else {
                    sender.sendMessage(getMessage("player_not_found"));
                }
            });
        }
    }

    public String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }

    private void startPeriodicSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerData data : playerDataMap.values()) {
                    plugin.getDatabaseManager().savePlayerData(data);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60 * 5, 20 * 60 * 5); // 每5分钟保存一次
    }

    /**
     * 启动在线时间检查任务
     */
    public void startOnlineTimeCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 遍历所有在线玩家
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    PlayerData data = playerDataMap.get(player.getUniqueId());
                    if (data != null) {
                        // 更新在线时间
                        updateOnlineTime(player);
                        // 检查是否应该奖励复活次数
                        checkOnlineTimeReward(player, data);
                        // 检查是否需要恢复复活次数（在线时也检查）
                        checkOfflineRespawnRecovery(player, data);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60, 20 * 60); // 每分钟检查一次
    }

    /**
     * 检查离线期间是否需要恢复复活次数
     */
    public void checkOfflineRespawnRecovery(org.bukkit.entity.Player player, PlayerData data) {
        long currentTime = System.currentTimeMillis();
        long lastRecovery = data.getLastRespawnRecovery();
        long timeElapsed = currentTime - lastRecovery;
        
        // 24小时的毫秒数
        long recoveryInterval = 24 * 60 * 60 * 1000;
        
        // 计算应该恢复的次数
        int recoverCount = (int) (timeElapsed / recoveryInterval);
        
        if (recoverCount > 0) {
            // 获取最大复活次数
            int maxRespawnCount = 3;
            
            // 计算新的复活次数
            int newRespawnCount = Math.min(data.getRespawnCount() + recoverCount, maxRespawnCount);
            
            if (newRespawnCount > data.getRespawnCount()) {
                int actuallyRecovered = newRespawnCount - data.getRespawnCount();
                data.setRespawnCount(newRespawnCount);
                // 更新最后恢复时间
                data.setLastRespawnRecovery(lastRecovery + (long) actuallyRecovered * recoveryInterval);
                plugin.getDatabaseManager().savePlayerData(data);
                
                // 通知玩家
                player.sendMessage(org.bukkit.ChatColor.GREEN + "你离线期间恢复了 " + actuallyRecovered + " 次复活机会！当前剩余: " + newRespawnCount + " 次");
            } else if (data.getRespawnCount() >= maxRespawnCount) {
                // 如果已经达到最大次数，更新最后恢复时间
                data.setLastRespawnRecovery(currentTime);
                plugin.getDatabaseManager().savePlayerData(data);
            }
        }
    }

    /**
     * 更新玩家的在线时间
     */
    public void updateOnlineTime(org.bukkit.entity.Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - data.getLastLogin();
            
            // 只更新正数的时间差
            if (timeElapsed > 0) {
                data.setTotalOnlineTime(data.getTotalOnlineTime() + timeElapsed);
                data.setLastLogin(currentTime);
                plugin.getDatabaseManager().savePlayerData(data);
            }
        }
    }

    /**
     * 检查并奖励在线时间对应的复活次数
     */
    public void checkOnlineTimeReward(org.bukkit.entity.Player player, PlayerData data) {
        // 检查是否启用了在线时间奖励
        if (!plugin.getConfig().getBoolean("settings.online_time_reward.enabled", true)) {
            return;
        }

        // 获取配置的参数
        long requiredHours = plugin.getConfig().getLong("settings.online_time_reward.required_time.hours", 24);
        long requiredMinutes = plugin.getConfig().getLong("settings.online_time_reward.required_time.minutes", 0);
        int maxStacks = plugin.getConfig().getInt("settings.online_time_reward.max_stacks", 3);

        // 计算所需的毫秒数
        long requiredMillis = (requiredHours * 60 + requiredMinutes) * 60 * 1000;

        // 获取当前时间和上次获得奖励的时间
        long currentTime = System.currentTimeMillis();
        long lastRewardTime = data.getLastOnlineReward();
        long timeElapsed = currentTime - lastRewardTime;

        // 检查是否已经达到最大叠加次数
        if (data.getRespawnCount() >= maxStacks) {
            // 如果已经达到最大值，更新上次奖励时间以避免重复检查
            data.setLastOnlineReward(currentTime);
            plugin.getDatabaseManager().savePlayerData(data);
            return;
        }

        // 检查是否达到了获得奖励的时间
        if (timeElapsed >= requiredMillis) {
            // 计算可以获得的奖励次数
            int rewardCount = 1;
            
            // 计算新的复活次数，确保不超过最大值
            int newRespawnCount = Math.min(data.getRespawnCount() + rewardCount, maxStacks);
            
            if (newRespawnCount > data.getRespawnCount()) {
                // 更新复活次数
                data.setRespawnCount(newRespawnCount);
                // 更新上次获得奖励的时间
                data.setLastOnlineReward(currentTime);
                plugin.getDatabaseManager().savePlayerData(data);
                
                // 通知玩家获得了复活次数
                player.sendMessage(org.bukkit.ChatColor.GREEN + "你累计在线 " + requiredHours + " 小时 " + requiredMinutes + " 分钟 ，获得了 " + rewardCount + " 次复活机会！当前剩余: " + newRespawnCount + " 次");
            }
        }
    }

    /**
     * 玩家退出时调用，更新在线时间
     */
    public void onPlayerQuit(org.bukkit.entity.Player player) {
        updateOnlineTime(player);
    }

    /**
     * 应用一滴血模式 - 将玩家最大生命值设置为1
     * @param player 目标玩家
     */
    public void applyOneHeartMode(Player player) {
        if (!plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            return;
        }

        // 设置最大生命值为1
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(1.0);

        // 确保当前生命值不超过最大值
        if (player.getHealth() > 1.0) {
            player.setHealth(1.0);
        }

        // 应用速度降低效果（如果启用）
        if (plugin.getConfig().getBoolean("settings.one_heart.speed_effect_enabled", false)) {
            double speedReduction = plugin.getConfig().getDouble("settings.one_heart.speed_reduction", 0.2);
            player.setWalkSpeed((float) (0.2 * (1 - speedReduction)));
        }
    }

    /**
     * 恢复玩家正常最大生命值（20点，10颗心）
     * @param player 目标玩家
     */
    public void restoreNormalHealth(Player player) {
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
        player.setHealth(20.0);
        player.setWalkSpeed(0.2f);
    }

    /**
     * 为指定玩家应用保存的生命值上限
     * @param player 目标玩家
     */
    public void applySavedMaxHealth(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            applySavedMaxHealth(player, data);
        }
    }

    /**
     * 应用保存的生命值上限
     * @param player 目标玩家
     * @param data 玩家数据
     */
    public void applySavedMaxHealth(Player player, PlayerData data) {
        if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            // 如果一滴血模式启用，应用一滴血模式
            applyOneHeartMode(player);
        } else {
            // 否则应用保存的生命值上限
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(data.getMaxHealth());
            // 确保当前生命值为最大生命值
            player.setHealth(data.getMaxHealth());
        }
    }

    /**
     * 设置玩家的生命值上限
     * @param player 目标玩家
     * @param maxHealth 新的生命值上限
     */
    public void setMaxHealth(Player player, double maxHealth) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setMaxHealth(maxHealth);
            plugin.getDatabaseManager().savePlayerData(data);
            // 应用新的生命值上限
            if (!plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
                // 确保当前生命值为最大生命值
                player.setHealth(maxHealth);
            }
        }
    }

    /**
     * 获取玩家的生命值上限
     * @param player 目标玩家
     * @return 生命值上限
     */
    public double getMaxHealth(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        return data != null ? data.getMaxHealth() : 20.0;
    }

    /**
     * 结束等待期时调用
     */
    public void endWaitingPeriod(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setWaiting(false);
            data.setDeathTimestamp(0);
            plugin.getDatabaseManager().savePlayerData(data);

            // 恢复玩家状态 - 设置为生存模式
            player.setGameMode(GameMode.SURVIVAL);

            // 如果一滴血模式已启用，保持最大生命值为1
            if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
                applyOneHeartMode(player);
            } else {
                // 应用保存的生命值上限
                applySavedMaxHealth(player, data);
            }

            player.sendMessage(getMessage("waiting_period_ended"));
        }
    }

    /**
     * 使用跳过命令时
     */
    public boolean attemptSkip(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null || !data.isWaiting()) {
            player.sendMessage(getMessage("not_in_waiting_period"));
            return true;
        }

        if (data.getRespawnCount() <= 0) {
            player.sendMessage(getMessage("no_respawn_count"));
            return true;
        }

        // 消耗一次复活次数
        data.setRespawnCount(data.getRespawnCount() - 1);
        data.setWaiting(false);
        data.setDeathTimestamp(0);
        plugin.getDatabaseManager().savePlayerData(data);

        // 移除 BossBar
        if (activeBossBars.containsKey(player.getUniqueId())) {
            activeBossBars.get(player.getUniqueId()).removeAll();
            activeBossBars.remove(player.getUniqueId());
        }

        // 恢复玩家状态 - 设置为生存模式
        player.setGameMode(GameMode.SURVIVAL);

        // 如果一滴血模式已启用，保持最大生命值为1
        if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            applyOneHeartMode(player);
        } else {
            // 应用保存的生命值上限
            applySavedMaxHealth(player, data);
        }

        player.sendMessage(getMessage("skip_success")
                .replace("{count}", String.valueOf(data.getRespawnCount())));

        return true;
    }

    /**
     * 获取剩余等待时间（格式化字符串）
     */
    public String getRemainingTimeFormatted(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data == null || !data.isWaiting()) {
            return "0 秒";
        }

        long timeLeft = data.getTimeUntilRelease(System.currentTimeMillis());
        return TimeUtils.formatTime(timeLeft);
    }
}