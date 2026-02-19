package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

import java.util.Random;

public class EntityKillListener implements Listener {
    private final HardcoreRespawn plugin;
    private final Random random = new Random();

    public EntityKillListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        // 检查奖励系统是否启用
        if (!plugin.getConfig().getBoolean("rewards.enabled", false)) {
            return;
        }

        // 检查击杀者是否为玩家
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player killer = (Player) event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();

        // 检查是否为可奖励的实体类型
        if (plugin.getConfig().contains("rewards.entities." + entityType.name())) {
            double chance = plugin.getConfig().getDouble("rewards.entities." + entityType.name() + ".chance", 0.0);
            int count = plugin.getConfig().getInt("rewards.entities." + entityType.name() + ".count", 0);

            if (random.nextDouble() <= chance) {
                plugin.getPlayerDataManager().addRespawnCount(killer, count);
                killer.sendMessage(plugin.getPlayerDataManager().getMessage("reward_received")
                        .replace("{count}", String.valueOf(count))
                        .replace("{entity}", entityType.name()));
            }
        }
    }
}