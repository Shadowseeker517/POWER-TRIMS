package MCplugin.powerTrims.commands;

import MCplugin.powerTrims.Logic.Messaging;
import MCplugin.powerTrims.Logic.PlayerSettingsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ToggleOffhandCommand implements CommandExecutor {

    private final PlayerSettingsManager playerSettingsManager;

    public ToggleOffhandCommand(PlayerSettingsManager playerSettingsManager) {
        this.playerSettingsManager = playerSettingsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messaging.sendError(sender, "This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        boolean currentSetting = playerSettingsManager.isOffhandActivationEnabled(uuid);
        boolean newSetting = !currentSetting;
        playerSettingsManager.setOffhandActivation(uuid, newSetting);

        if (newSetting) {
            Messaging.sendPluginMessage(player, "Off-hand ability activation has been " + ChatColor.GREEN + "ENABLED" + ChatColor.GRAY + ".");
        } else {
            Messaging.sendPluginMessage(player, "Off-hand ability activation has been " + ChatColor.RED + "DISABLED" + ChatColor.GRAY + ".");
        }

        return true;
    }
}
