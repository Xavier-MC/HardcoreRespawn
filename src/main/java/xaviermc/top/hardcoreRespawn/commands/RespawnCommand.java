package xaviermc.top.hardcoreRespawn.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xaviermc.top.hardcoreRespawn.HardcoreRespawn;
import xaviermc.top.hardcoreRespawn.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RespawnCommand implements CommandExecutor, TabCompleter {
        private final HardcoreRespawn plugin;

        // 主指令子命令列表
        private static final List<String> MAIN_SUBCOMMANDS = Arrays.asList("skip", "info", "admin", "reload");

        // admin 子命令列表
        private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList("add", "set", "reset");

        public RespawnCommand(HardcoreRespawn plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
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

        @Override
        @Nullable
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                          @NotNull String label, @NotNull String[] args) {
            // args[0] = 第一个参数，args[1] = 第二个参数，以此类推
            if (args.length == 1) {
                // 补全主指令：skip, info, admin, reload
                return getCompletions(args[0], MAIN_SUBCOMMANDS, sender);
            } else if (args.length == 2) {
                // 根据第一个参数决定第二个参数的补全
                switch (args[0].toLowerCase()) {
                    case "admin":
                        // admin 命令需要权限
                        if (sender.hasPermission("hardcorerespawn.admin")) {
                            return getCompletions(args[1], ADMIN_SUBCOMMANDS, sender);
                        }
                        break;
                    default:
                        break;
                }
            } else if (args.length == 3) {
                // admin add/set 需要玩家名
                if (args[0].equalsIgnoreCase("admin") &&
                        (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("set"))) {
                    if (sender.hasPermission("hardcorerespawn.admin")) {
                        // 返回所有在线玩家名
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            } else if (args.length == 4) {
                // admin add/set 需要数字
                if (args[0].equalsIgnoreCase("admin") &&
                        (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("set"))) {
                    if (sender.hasPermission("hardcorerespawn.admin")) {
                        // 数字不需要补全，返回空列表
                        return new ArrayList<>();
                    }
                }
            }

            return new ArrayList<>();
        }

        /**
         * 获取匹配的补全建议
         */
        private List<String> getCompletions(String currentInput, List<String> allOptions, CommandSender sender) {
            return allOptions.stream()
                    .filter(option -> option.toLowerCase().startsWith(currentInput.toLowerCase()))
                    .collect(Collectors.toList());
        }

    private boolean showHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_header")));
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_info")));
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_skip")));
        } else {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_header")));
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_admin_add")));
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_admin_set")));
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_admin_reset")));
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_help_reload")));
        }
        return true;
    }

    private boolean handleSkip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_only_player")));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hardcorerespawn.skip")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_no_permission")));
            return true;
        }

        return plugin.getPlayerDataManager().attemptSkip(player);
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_only_player")));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hardcorerespawn.info")) {
            player.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_no_permission")));
            return true;
        }

        plugin.getPlayerDataManager().showInfo(player);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hardcorerespawn.admin")) {
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_no_permission")));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.getColoredMessage("&c用法：/respawn admin <add|set|reset> <玩家> [数量]"));
            return true;
        }

        String subCommand = args[1].toLowerCase();
        String targetPlayerName = args[2];

        switch (subCommand) {
            case "add":
                if (args.length != 4) {
                    sender.sendMessage(MessageUtils.getColoredMessage("&c用法：/respawn admin add <玩家> <数量>"));
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
                    sender.sendMessage(MessageUtils.getColoredMessage("&c用法：/respawn admin set <玩家> <数量>"));
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
            sender.sendMessage(MessageUtils.getColoredMessage(plugin.getPlayerDataManager().getMessage("command_no_permission")));
            return true;
        }

        plugin.reloadConfig();
        MessageUtils.loadMessages();
        sender.sendMessage(MessageUtils.getColoredMessage("&a配置已重载！"));
        return true;
    }
}