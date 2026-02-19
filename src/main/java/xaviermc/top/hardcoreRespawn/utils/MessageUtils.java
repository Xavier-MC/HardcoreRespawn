package xaviermc.top.hardcoreRespawn.utils;

import net.md_5.bungee.api.ChatColor;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;

public class MessageUtils {
    public static String getColoredMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void loadMessages() {
        HardcoreRespawn plugin = HardcoreRespawn.getInstance();

        // 如果messages.yml不存在，则创建默认配置
        if (!plugin.getDataFolder().toPath().resolve("messages.yml").toFile().exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }
}