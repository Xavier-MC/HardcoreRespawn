package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class DeathListener implements Listener {
    private final HardcoreRespawn plugin;

    public DeathListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 获取玩家当前复活次数
        int respawnCount = plugin.getPlayerDataManager().getPlayerRespawnCount(player);

        if (respawnCount > 0) {
            // 有复活次数，消耗一次并正常复活
            plugin.getPlayerDataManager().useRespawnCount(player);
            player.sendMessage(plugin.getPlayerDataManager().getMessage("respawn_used")
                    .replace("{count}", String.valueOf(respawnCount - 1)));

            // 设置为生存模式，让玩家复活
            player.setGameMode(GameMode.SURVIVAL);
        } else {
            // 没有复活次数，开始等待期
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);

            plugin.getPlayerDataManager().startWaitingPeriod(player);

            player.sendMessage(plugin.getPlayerDataManager().getMessage("death_penalty_started"));
        }

        // 防止掉落物品
        event.getDrops().clear();
        event.setKeepInventory(true);
    }
}