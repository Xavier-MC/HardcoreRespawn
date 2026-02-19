package xaviermc.top.hardcoreRespawn.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

public class RespawnCommand implements CommandExecutor {
    private final HardcoreRespawn plugin;

    public RespawnCommand(HardcoreRespawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender);
        }

        switch (args[0].toLowerCase()) {
            case "skip":
                return handleSkip(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "admin":
                return handleAdmin(sender, args);
            case "reload":
                return handleReload(sender, args);
            default:
                return showHelp(sender);
        }
    }

    private boolean showHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(MessageUtils.getColoredMessage("&a--- 硬核复活插件 ---"));
            player.sendMessage(MessageUtils.getColoredMessage("&e/respawn info &7- 查看复活信息"));
            player.sendMessage(MessageUtils.getColoredMessage("&e/respawn skip &7- 跳过等待时间"));
        } else {
            sender.sendMessage(MessageUtils.getColoredMessage("&a--- 硬核复活插件 ---"));
            sender.sendMessage(MessageUtils.getColoredMessage("&e/respawn admin add <玩家> <数量> &7- 添加复活次数"));
            sender.sendMessage(MessageUtils.getColoredMessage("&e/respawn admin set <玩家> <数量> &7- 设置复活次数"));
            sender.sendMessage(MessageUtils.getColoredMessage("&e/respawn admin reset <玩家> &7- 重置玩家状态"));
            sender.sendMessage(MessageUtils.getColoredMessage("&e/respawn reload &7- 重载配置"));
        }
        return true;
    }

    private boolean handleSkip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c此命令只能由玩家执行！"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hardcorerespawn.skip")) {
            player.sendMessage(MessageUtils.getColoredMessage("&c你没有权限执行此命令！"));
            return true;
        }

        return plugin.getPlayerDataManager().attemptSkip(player);
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c此命令只能由玩家执行！"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hardcorerespawn.info")) {
            player.sendMessage(MessageUtils.getColoredMessage("&c你没有权限执行此命令！"));
            return true;
        }

        plugin.getPlayerDataManager().showInfo(player);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hardcorerespawn.admin")) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c你没有权限执行此命令！"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c用法: /respawn admin <add|set|reset> <玩家> [数量]"));
            return true;
        }

        String subCommand = args[1].toLowerCase();
        String targetPlayerName = args[2];

        switch (subCommand) {
            case "add":
                if (args.length != 4) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&c用法: /respawn admin add <玩家> <数量>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[3]);
                    plugin.getPlayerDataManager().adminAdd(targetPlayerName, amount, sender);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&c数量必须是数字！"));
                }
                break;
            case "set":
                if (args.length != 4) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&c用法: /respawn admin set <玩家> <数量>"));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[3]);
                    plugin.getPlayerDataManager().adminSet(targetPlayerName, amount, sender);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&c数量必须是数字！"));
                }
                break;
            case "reset":
                plugin.getPlayerDataManager().adminReset(targetPlayerName, sender);
                break;
            default:
                sender.sendMessage(MessageUtils.getColoredMessage("&c无效的子命令！"));
                return true;
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hardcorerespawn.admin")) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c你没有权限执行此命令！"));
            return true;
        }

        plugin.reloadConfig();
        MessageUtils.loadMessages();
        sender.sendMessage(MessageUtils.getColoredMessage("&a配置已重载！"));
        return true;
    }
}