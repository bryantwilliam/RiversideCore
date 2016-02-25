package com.gmail.gogobebe2.riversidecore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    private static Economy economy = null;
    private Map<UUID, Long> cooldowns = new HashMap<>(); //Player uuid, Time in milliseconds when the player ran cooldown starts
    private boolean chatMuted = false;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting up " + this.getName() + ". If you need me to update this plugin, email at gogobebe2@gmail.com");
        setupEconomy();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling " + this.getName() + ". If you need me to update this plugin, email at gogobebe2@gmail.com");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (label.equalsIgnoreCase("fixhand") && player.hasPermission("riverside.fixhand")) {
                final double cost = 5000;
                EconomyResponse economyResponse = economy.withdrawPlayer(player, cost);
                ItemStack item = player.getItemInHand();

                if (!economyResponse.transactionSuccess()) {
                    player.sendMessage(ChatColor.RED + "Error! " + economyResponse.errorMessage);
                } else if (!repair(item)) {
                    player.sendMessage(ChatColor.RED + "Error! This item can not be fixed!");
                } else {
                    player.sendMessage(ChatColor.GREEN + "$" + cost + " has been taken out of your account and your item has been fixed!");
                }
                return true;
            } else if (label.equalsIgnoreCase("fixall") && player.hasPermission("riverside.fixall")) {
                final int COOLDOWN_TIME = 60 * 60; // 1 hour
                UUID uuid = player.getUniqueId();
                long currentMillis = System.currentTimeMillis();

                if (cooldowns.containsKey(uuid)) {
                    int timePassed = (int) (cooldowns.get(uuid) - System.currentTimeMillis() * 1000);
                    int timeLeft = COOLDOWN_TIME - timePassed;

                    if (timeLeft > 0) {
                        //the player is still cooling down.
                        player.sendMessage(ChatColor.RED + "Error! This command is on cool down. You can use it again in " + timeLeft + " minutes.");
                        return true;
                    }
                    cooldowns.remove(uuid);
                }

                boolean atleast1RepairableItemPresent = false;
                for (ItemStack item : player.getInventory().getArmorContents())
                    if (repair(item)) atleast1RepairableItemPresent = true;
                for (ItemStack item : player.getInventory().getContents())
                    if (repair(item)) atleast1RepairableItemPresent = true;

                if (atleast1RepairableItemPresent) {
                    player.sendMessage(ChatColor.GREEN + "All items fixed!");
                    cooldowns.put(uuid, currentMillis);
                } else player.sendMessage(ChatColor.RED + "Error! You don't have any items that can be fixed!");

                return true;
            } else if (label.equalsIgnoreCase("clearchat") && player.hasPermission("riverside.clearchat")) {
                for (int i = 0; i < 100; i++) player.sendMessage("");
                player.sendMessage(ChatColor.GREEN + "Chat cleared!");
                return true;
            } else if (label.equalsIgnoreCase("mutechat") && player.hasPermission("riverside.mutechat")) {
                chatMuted = !chatMuted;
                player.sendMessage(ChatColor.GREEN + "Chat muted!");
                return true;
            } else if (label.equalsIgnoreCase("report") && player.hasPermission("riverside.report")) {
                String incorrectUsageMessage = ChatColor.RED + "Incorrect usage, use /report <player> <reason>";

                if (args.length != 2) {
                    player.sendMessage(incorrectUsageMessage);
                    return true;
                }

                String playername = args[0];
                String message = args[1];

                boolean atleast1StaffMemberOnline = false;

                for (Player possibleStaff : Bukkit.getOnlinePlayers()) {
                    if (possibleStaff.hasPermission("riverside.report.view")) {
                        possibleStaff.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Report against "
                                + ChatColor.AQUA + playername + ChatColor.BOLD + " for: " + ChatColor.RESET + message);
                        atleast1StaffMemberOnline = true;
                    }
                }

                if (atleast1StaffMemberOnline) player.sendMessage(ChatColor.GREEN + "Report sent to all staff members online.");
                else player.sendMessage(ChatColor.RED + "There are no staff members online a this time so they can't see your report!");

                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the repair was succesful, false otherwise.
     */
    private boolean repair(ItemStack item) {
        if (item instanceof Repairable) {
            item.setDurability((short) 0);
            return true;
        }
        return false;
    }

    @EventHandler
    private void playerChatEvent(AsyncPlayerChatEvent event) {
        if (chatMuted) {
            event.getPlayer().sendMessage(ChatColor.RED + "Chat is muted!");
            event.setCancelled(true);
        }
    }
}
