package MCplugin.powerTrims.commands;

import MCplugin.powerTrims.Logic.AbilityManager;
import MCplugin.powerTrims.Logic.ArmourChecking;
import MCplugin.powerTrims.Logic.Messaging;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AbilityCommand implements CommandExecutor, TabCompleter {

    private final AbilityManager abilityManager;
    private final List<String> trimNames;

    public AbilityCommand(AbilityManager abilityManager) {
        this.abilityManager = abilityManager;
        // Cache the trim pattern names from the registry on startup
        this.trimNames = Registry.TRIM_PATTERN.stream()
                .map(pattern -> pattern.getKey().getKey())
                .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messaging.sendError(sender, "This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            TrimPattern trimOnPlayer = ArmourChecking.getEquippedTrim(player);
            if (trimOnPlayer != null) {
                abilityManager.activatePrimaryAbility(player, trimOnPlayer);
            } else {
                Messaging.sendError(player, "You do not have a full set of trimmed armor.");
            }
            return true;
        }

        String trimName = args[0].toLowerCase();
        // Look up the pattern in the registry using a NamespacedKey
        TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(trimName));

        if (pattern == null) {
            Messaging.sendError(player, "Invalid trim ability name.");
            return true;
        }

        abilityManager.activatePrimaryAbility(player, pattern);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Filter the cached list of names for tab completion
            return trimNames.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
