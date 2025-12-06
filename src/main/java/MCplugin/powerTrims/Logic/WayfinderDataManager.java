package MCplugin.powerTrims.Logic;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class WayfinderDataManager {

    private final JavaPlugin plugin;
    private FileConfiguration dataConfig;
    private final File configFile;

    public WayfinderDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "wayfinder-data.yml");
        if (!configFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create wayfinder-data.yml file.");
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void setMarkedLocation(UUID playerUUID, Location location) {
        dataConfig.set(playerUUID.toString(), location);
        saveData();
    }

    public Location getMarkedLocation(UUID playerUUID) {
        return dataConfig.getLocation(playerUUID.toString());
    }

    public void removeMarkedLocation(UUID playerUUID) {
        dataConfig.set(playerUUID.toString(), null);
        saveData();
    }

    public boolean hasMarkedLocation(UUID playerUUID) {
        return dataConfig.isSet(playerUUID.toString());
    }

    private void saveData() {
        try {
            dataConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save Wayfinder data to " + configFile);
        }
    }
}
