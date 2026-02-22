package xaviermc.top.hardcoreRespawn.utils;

import net.md_5.bungee.api.ChatColor;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

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

    public static String getLogMessage(String key, Object... args) {
        HardcoreRespawn plugin = HardcoreRespawn.getInstance();
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 获取消息，如果不存在则返回默认值
        String message = messagesConfig.getString("messages." + key, key);

        // 替换参数
        if (args.length > 0 && args.length % 2 == 0) {
            // 假设参数是键值对：key1, value1, key2, value2, ...
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    String placeholder = "{" + args[i] + "}";
                    message = message.replace(placeholder, String.valueOf(args[i + 1]));
                }
            }
        }

        return message;
    }
}