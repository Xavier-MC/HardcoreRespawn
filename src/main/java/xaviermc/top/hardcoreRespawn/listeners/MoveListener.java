package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class MoveListener implements Listener {
    private final HardcoreRespawn plugin;

    public MoveListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            double spawnRadius = plugin.getConfig().getDouble("settings.spawn_radius", 5.0);

            // 计算距离出生点的距离
            double distance = player.getLocation().distance(spawnLocation);

            if (distance > spawnRadius) {
                // 传送回出生点
                player.teleport(spawnLocation);
                player.sendMessage(plugin.getPlayerDataManager().getMessage("movement_restricted"));
            }
        }
    }
}