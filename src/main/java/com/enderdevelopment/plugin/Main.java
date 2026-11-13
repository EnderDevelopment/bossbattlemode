package com.enderdevelopment.plugin;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private Wither boss;
    private BossBar bossBar;
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("BossBarBoss enabled. Use /boss spawn to summon the boss.");
    }

    @Override
    public void onDisable() {
        removeBoss(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("boss")) {
            return false;
        }
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /boss <spawn|remove>", NamedTextColor.YELLOW));
            return true;
        }
        if (args[0].equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only a player can summon the boss.", NamedTextColor.RED));
                return true;
            }
            if (boss != null && boss.isValid() && !boss.isDead()) {
                sender.sendMessage(Component.text("The boss is already alive.", NamedTextColor.RED));
                return true;
            }
            boss = player.getWorld().spawn(player.getLocation(), Wither.class, wither -> {
                wither.customName(Component.text("The Abyssal Boss", NamedTextColor.DARK_PURPLE));
                wither.setCustomNameVisible(true);
                wither.setRemoveWhenFarAway(false);
            });
            bossBar = Bukkit.createBossBar("The Abyssal Boss", BarColor.PURPLE, BarStyle.SEGMENTED_10);
            bossBar.setProgress(1.0);
            for (Player online : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(online);
            }
            updateTask = Bukkit.getScheduler().runTaskTimer(this, this::updateBossBar, 1L, 5L);
            Bukkit.broadcast(Component.text("The Abyssal Boss has appeared!", NamedTextColor.DARK_PURPLE));
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            if (boss == null || !boss.isValid() || boss.isDead()) {
                sender.sendMessage(Component.text("There is no active boss.", NamedTextColor.RED));
                removeBoss(false);
                return true;
            }
            removeBoss(true);
            Bukkit.broadcast(Component.text("The Abyssal Boss was removed.", NamedTextColor.GRAY));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /boss <spawn|remove>", NamedTextColor.YELLOW));
        return true;
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (boss != null && event.getEntity().getUniqueId().equals(boss.getUniqueId())) {
            removeBoss(false);
            Bukkit.broadcast(Component.text("The Abyssal Boss has been defeated!", NamedTextColor.GREEN));
        }
    }

    private void updateBossBar() {
        if (boss == null || !boss.isValid() || boss.isDead()) {
            removeBoss(false);
            return;
        }
        double maxHealth = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, boss.getHealth() / maxHealth)));
        bossBar.setTitle("The Abyssal Boss - " + (int) Math.ceil(boss.getHealth()) + " HP");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(boss.getWorld())) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private void removeBoss(boolean removeEntity) {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (removeEntity && boss != null && boss.isValid() && !boss.isDead()) {
            Entity entity = boss;
            entity.remove();
        }
        boss = null;
    }
}
