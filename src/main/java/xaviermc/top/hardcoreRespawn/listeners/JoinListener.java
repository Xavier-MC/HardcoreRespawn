package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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

        // 检查玩家是否在等待期
        if (plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            player.teleport(spawnLocation);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);

            plugin.getPlayerDataManager().resumeWaitingPeriod(player);
        } else {
            // 如果是新玩家，给予初始复活次数
            plugin.getPlayerDataManager().initializeNewPlayer(player);
        }
    }
}
