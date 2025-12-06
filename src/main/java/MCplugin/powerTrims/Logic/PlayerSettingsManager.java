package MCplugin.powerTrims.Logic;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSettingsManager implements Listener {

    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Boolean> offhandToggleCache = new ConcurrentHashMap<>();

    public PlayerSettingsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "player-settings.yml");
        setup();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setup() {
        if (!configFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player-settings.yml file.");
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            dataConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player settings to " + configFile);
        }
    }

    public boolean isOffhandActivationEnabled(UUID uuid) {
        return offhandToggleCache.getOrDefault(uuid, true);
    }

    public void setOffhandActivation(UUID uuid, boolean enabled) {
        offhandToggleCache.put(uuid, enabled);
        dataConfig.set(uuid.toString() + ".offhand-activation", enabled);
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        boolean setting = dataConfig.getBoolean(uuid.toString() + ".offhand-activation", true);
        offhandToggleCache.put(uuid, setting);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        offhandToggleCache.remove(event.getPlayer().getUniqueId());
    }
}
