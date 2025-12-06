package MCplugin.powerTrims.Logic;

import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AbilityManager {

    private final Map<TrimPattern, Consumer<Player>> primaryAbilities = new HashMap<>();

    public void registerPrimaryAbility(TrimPattern trim, Consumer<Player> ability) {
        primaryAbilities.put(trim, ability);
    }

    public void activatePrimaryAbility(Player player) {
        TrimPattern pattern = ArmourChecking.getEquippedTrim(player); // You already have this logic
        if (pattern != null) {
            // Call the same method the /ability command uses
            activatePrimaryAbility(player, pattern);
        } else {
            Messaging.sendError(player, "You do not have a full set of trimmed armor.");
        }
    }

    public void activatePrimaryAbility(Player player, TrimPattern pattern) {
        if (primaryAbilities.containsKey(pattern)) {
            primaryAbilities.get(pattern).accept(player);
        } else {
            Messaging.sendError(player, "That trim does not have a registered ability.");
        }
    }

}

