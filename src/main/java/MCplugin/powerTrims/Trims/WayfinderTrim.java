package MCplugin.powerTrims.Trims;

import MCplugin.powerTrims.Logic.*;
import MCplugin.powerTrims.config.ConfigManager;
import MCplugin.powerTrims.integrations.WorldGuardIntegration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WayfinderTrim implements Listener {
    private final JavaPlugin plugin;
    private final TrimCooldownManager cooldownManager;
    private final ConfigManager configManager;
    private final AbilityManager abilityManager;
    private final WayfinderDataManager wayfinderDataManager;
    private final PlayerSettingsManager playerSettingsManager;
    private final long TELEPORT_COOLDOWN;
    private final Set<UUID> debouncedPlayers = new HashSet<>();

    private static NamespacedKey PHASED_KEY;

    public WayfinderTrim(JavaPlugin plugin, TrimCooldownManager cooldownManager, ConfigManager configManager, AbilityManager abilityManager, WayfinderDataManager wayfinderDataManager, PlayerSettingsManager playerSettingsManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.configManager = configManager;
        this.abilityManager = abilityManager;
        this.wayfinderDataManager = wayfinderDataManager;
        this.playerSettingsManager = playerSettingsManager;
        this.TELEPORT_COOLDOWN = configManager.getLong("wayfinder.primary.cooldown");

        PHASED_KEY = new NamespacedKey(plugin, "wayfinder_phased_player");
        abilityManager.registerPrimaryAbility(TrimPattern.WAYFINDER, this::WayfinderPrimary);
    }

    public void WayfinderPrimary(Player player) {
        if (debouncedPlayers.contains(player.getUniqueId())) {
            return;
        }
        if (!configManager.isTrimEnabled("wayfinder")) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null && !WorldGuardIntegration.canUseAbilities(player)) {
            Messaging.sendError(player, "You cannot use this ability in the current region.");
            return;
        }
        if (!ArmourChecking.hasFullTrimmedArmor(player, TrimPattern.WAYFINDER)) {
            return;
        }
        if (cooldownManager.isOnCooldown(player, TrimPattern.WAYFINDER)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (wayfinderDataManager.hasMarkedLocation(uuid)) {
            Location mark = wayfinderDataManager.getMarkedLocation(uuid);
            wayfinderDataManager.removeMarkedLocation(uuid);

            playDepartureAnimation(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    player.teleport(mark);
                    playArrivalAnimation(player, mark);
                    Messaging.sendTrimMessage(player, "Wayfinder", ChatColor.AQUA, "You have returned to your marked location!");
                }
            }.runTaskLater(plugin, 15L);

            cooldownManager.setCooldown(player, TrimPattern.WAYFINDER, TELEPORT_COOLDOWN);
        } else {
            Location mark = player.getLocation();
            wayfinderDataManager.setMarkedLocation(uuid, mark);
            playMarkAnimation(mark);
            Messaging.sendTrimMessage(player, "Wayfinder", ChatColor.DARK_AQUA, "You have marked this location!");
            player.playSound(mark, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        }
        debouncedPlayers.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                debouncedPlayers.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 2L);
    }

    private void playMarkAnimation(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        final int durationTicks = 40;
        final double radius = 1.5;
        final Particle particle = Particle.DUST;
        final Color startColor = Color.WHITE;
        final Color endColor = Color.BLUE;

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > durationTicks) {
                    world.spawnParticle(Particle.WITCH, location.clone().add(0, 1, 0), 100, 0.5, 1, 0.5, 0.1);
                    world.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
                    this.cancel();
                    return;
                }

                double progress = (double) ticks / durationTicks;
                double currentRadius = radius * (1 - progress);
                double yOffset = progress * 2.5;

                int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * progress);
                int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * progress);
                int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * progress);

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                Color currentColor = Color.fromRGB(r, g, b);
                Particle.DustOptions dustOptions = new Particle.DustOptions(currentColor, 1.0f);

                for (int i = 0; i < 4; i++) {
                    double angle = (ticks * 0.2) + (i * Math.PI / 2);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    world.spawnParticle(particle, location.clone().add(x, yOffset, z), 1, 0, 0, 0, dustOptions);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playDepartureAnimation(Player player) {
        player.getPersistentDataContainer().set(PHASED_KEY, PersistentDataType.BYTE, (byte) 1);


        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.2f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 20;

            @Override
            public void run() {
                if (ticks++ > duration || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1, 0);
                for (int i = 0; i < 15; i++) {
                    double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.2;
                    double y = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
                    double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.2;

                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.AQUA, 1.0f);
                    world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, dustOptions);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playArrivalAnimation(Player player, Location arrivalLocation) {
        World world = arrivalLocation.getWorld();
        if (world == null) {
            player.getPersistentDataContainer().remove(PHASED_KEY);
            return;
        }

        world.playSound(arrivalLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        try {
            world.spawnParticle(Particle.FLASH, arrivalLocation.clone().add(0, 1, 0), 1, 0, 0, 0, null);
        } catch (Exception e) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, arrivalLocation.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 20;

            @Override
            public void run() {
                if (ticks++ > duration) {
                    player.getPersistentDataContainer().remove(PHASED_KEY);
                    this.cancel();
                    return;
                }

                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE, 1.2f);

                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI / 10) * i + (ticks * 0.5);
                    double radius = 1.5 * (1.0 - ((double) ticks / duration));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    world.spawnParticle(Particle.DUST, arrivalLocation.clone().add(x, 1, z), 1, 0, 0, 0, dustOptions);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onOffhandPress(PlayerSwapHandItemsEvent event) {
        if (!configManager.isTrimEnabled("wayfinder")) {
            return;
        }
        if (!playerSettingsManager.isOffhandActivationEnabled(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            abilityManager.activatePrimaryAbility(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(PHASED_KEY, PersistentDataType.BYTE)) {
            player.getPersistentDataContainer().remove(PHASED_KEY);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(PHASED_KEY, PersistentDataType.BYTE)) {
            player.getPersistentDataContainer().remove(PHASED_KEY);
        }
    }
}
