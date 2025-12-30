package com.telegramnotifications;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("telegramnotifications.reload")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        plugin.reloadConfig();
        plugin.loadConfigValues();
        sender.sendMessage("§a✓ Configuración de TelegramNotifications recargada.");
        return true;
    }
}
