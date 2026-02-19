// src/main/java/xaviermc/top/hardcoreRespawn/listeners/CommandListener.java
package xaviermc.top.hardcoreRespawn.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

import java.util.List;

public class CommandListener implements Listener {
    private final HardcoreRespawn plugin;

    public CommandListener(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否在等待期
        if (!plugin.getPlayerDataManager().isInWaitingPeriod(player)) {
            return;
        }

        // 检查玩家是否有绕过权限（OP 默认拥有）
        if (player.hasPermission("hardcorerespawn.bypass.commandlimit")) {
            return; // 有权限，不限制
        }

        String message = event.getMessage();
        String command = message.split(" ")[0].toLowerCase().replace("/", "");

        // 获取白名单指令列表
        List<String> whitelist = plugin.getConfig().getStringList("settings.command_whitelist");

        // 如果白名单为空，不限制任何指令（功能关闭）
        if (whitelist == null || whitelist.isEmpty()) {
            return;
        }

        // 检查指令是否在白名单中
        boolean isAllowed = whitelist.stream()
                .anyMatch(allowed -> command.equalsIgnoreCase(allowed) ||
                        command.startsWith(allowed.toLowerCase() + ":"));

        if (!isAllowed) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.getColoredMessage(
                    plugin.getPlayerDataManager().getMessage("command_blocked")
            ));
        }
    }
}