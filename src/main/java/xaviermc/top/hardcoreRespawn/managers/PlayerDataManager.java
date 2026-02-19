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
                playerDataMap.put(player.getUniqueId(), data);
            } else {
                // 创建新玩家数据
                PlayerData newData = new PlayerData(player.getUniqueId(), player.getName());
                playerDataMap.put(player.getUniqueId(), newData);
                plugin.getDatabaseManager().savePlayerData(newData);
            }
        });
    }

    public void initializeNewPlayer(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.isNewPlayer()) {
            data.setRespawnCount(3); // 给予初始3次复活机会
            data.setNewPlayer(false);
            plugin.getDatabaseManager().savePlayerData(data);
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
            long waitTimeHours = plugin.getConfig().getLong("settings.wait_time_hours", 24);
            long waitTimeMillis = waitTimeHours * 60 * 60 * 1000; // 转换为毫秒

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
     * 应用一滴血模式 - 将玩家最大生命值设置为2（1颗心）
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

            // 如果一滴血模式已启用，保持最大生命值为2
            if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
                applyOneHeartMode(player);
            } else {
                restoreNormalHealth(player);
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

        // 如果一滴血模式已启用，保持最大生命值为2
        if (plugin.getConfig().getBoolean("settings.one_heart.enabled", true)) {
            applyOneHeartMode(player);
        } else {
            restoreNormalHealth(player);
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