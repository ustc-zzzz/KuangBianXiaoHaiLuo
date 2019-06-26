package io.izzel.kbxhl;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.val;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Log
public class KBXHLConfig {

    private Path invFile, rankFile;
    private FileConfiguration inv, rank;
    @Getter private final Set<UUID> players = new HashSet<>();

    private transient long top;

    @SneakyThrows
    void init(KBXHLBukkit instance) {
        log.info("加载配置");
        invFile = instance.getDataFolder().toPath().resolve("inv.yml");
        if (!Files.exists(invFile)) Files.createFile(invFile);
        inv = YamlConfiguration.loadConfiguration(Files.newBufferedReader(invFile));
        rankFile = instance.getDataFolder().toPath().resolve("rank.yml");
        if (!Files.exists(rankFile)) Files.createFile(rankFile);
        rank = YamlConfiguration.loadConfiguration(Files.newBufferedReader(rankFile));
        top = rank.getLong("top", Long.MAX_VALUE);
        instance.getServer().getPluginManager().registerEvents(new Listener(), instance);
        log.info("配置加载完成");
    }

    public void updateRank(Player player, Duration duration) {
        val time = duration.toMillis();
        val uid = player.getUniqueId().toString();
        if (rank.contains(uid)) {
            val last = rank.getLong(uid);
            if (time < last) {
                rank.set(uid, time);
                player.sendMessage("打破个人记录");
                if (time < top) {
                    player.sendMessage("打破服务器记录");
                    rank.set("top", time);
                    rank.set("top_player", uid);
                    top = time;
                }
            }
        } else {
            rank.set(uid, time);
        }
    }

    @SneakyThrows
    private void storeInv(Player player) {
        val id = player.getUniqueId().toString();
        if (inv.contains(id)) inv.set(id, null);
        val sec = inv.createSection(id);
        val contents = player.getInventory().getContents();
        sec.set("inv", Arrays.asList(contents));
    }

    @SuppressWarnings("unchecked")
    private void loadInv(Player player) {
        val id = player.getUniqueId().toString();
        if (inv.contains(id)) {
            val contents = ((List<ItemStack>) inv.getConfigurationSection(id).getList("inv")).toArray(new ItemStack[0]);
            player.getInventory().setContents(contents);
            inv.set(id, null);
        }
    }

    @SneakyThrows
    void save() {
        inv.save(invFile.toFile());
        rank.save(rankFile.toFile());
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(KBXHLEvent.Start event) {
            players.add(event.getPlayer().getUniqueId());
            storeInv(event.getPlayer());
            event.getPlayer().getInventory().clear();
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(KBXHLEvent.Stop event) {
            players.remove(event.getPlayer().getUniqueId());
            loadInv(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(PlayerJoinEvent event) {
            val uid = event.getPlayer().getUniqueId().toString();
            if (inv.contains(uid)) loadInv(event.getPlayer());
        }

    }

}
