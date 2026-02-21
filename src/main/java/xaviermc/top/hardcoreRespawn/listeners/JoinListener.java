// src/main/java/xaviermc/top/hardcoreRespawn/listeners/JoinListener.java
package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class JoinListener implements Listener {
    private final HardcoreRespawn plugin;

    public JoinListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 加载玩家数据
        plugin.getPlayerDataManager().loadPlayerData(player);

        // 检查玩家是否未登录，如果未登录则跳过所有限制
        if (HardcoreRespawn.isPlayerLoggedOut(player)) {
            return;
        }

        // 应用生命值设置
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 加载玩家数据后应用生命值设置
            plugin.getPlayerDataManager().applySavedMaxHealth(player);
        }, 10L);

        // 检查玩家是否在等待期
        if (plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);

            // 设置为旁观者模式
            player.setGameMode(GameMode.SPECTATOR);

            plugin.getPlayerDataManager().resumeWaitingPeriod(player);

            player.sendMessage(plugin.getPlayerDataManager().getMessage("still_in_waiting_period")
                    .replace("{time}", plugin.getPlayerDataManager().getRemainingTimeFormatted(player)));
        } else {
            // 如果是新玩家，给予初始复活次数
            plugin.getPlayerDataManager().initializeNewPlayer(player);
        }
    }

    // 监听世界切换事件，确保切换世界后仍保持一滴血模式
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否未登录，如果未登录则跳过所有限制
        if (HardcoreRespawn.isPlayerLoggedOut(player)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 应用生命值设置
            plugin.getPlayerDataManager().applySavedMaxHealth(player);

            // 如果在等待期，保持配置的等待游戏模式
            if (plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
                int waitTimeMode = plugin.getConfig().getInt("settings.wait_time_mode", 3);
                switch (waitTimeMode) {
                    case 0:
                        player.setGameMode(GameMode.SURVIVAL);
                        break;
                    case 2:
                        player.setGameMode(GameMode.ADVENTURE);
                        break;
                    case 3:
                    default:
                        player.setGameMode(GameMode.SPECTATOR);
                        break;
                }
            }
        }, 20L);
    }

    // 监听玩家退出事件，更新在线时间
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().onPlayerQuit(player);
    }
}