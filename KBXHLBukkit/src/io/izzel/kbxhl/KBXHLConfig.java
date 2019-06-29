package io.izzel.kbxhl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.val;
import lombok.var;
import org.bukkit.Material;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Log
public class KBXHLConfig {

    private Path invFile, rankFile;
    private FileConfiguration inv, rank;

    private transient long top;

    @SneakyThrows
    void init(KBXHLBukkit instance) {
        log.info("加载配置");
        val folder = instance.getDataFolder().toPath();
        if (!Files.isDirectory(folder)) Files.createDirectories(folder);
        invFile = folder.resolve("inv.yml");
        if (!Files.exists(invFile)) Files.createFile(invFile);
        inv = YamlConfiguration.loadConfiguration(Files.newBufferedReader(invFile));
        rankFile = folder.resolve("rank.yml");
        if (!Files.exists(rankFile)) Files.createFile(rankFile);
        rank = YamlConfiguration.loadConfiguration(Files.newBufferedReader(rankFile));
        top = rank.getLong("top", Long.MAX_VALUE);
        instance.getServer().getPluginManager().registerEvents(new Listener(), instance);
        log.info("配置加载完成");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Long> getRank() {
        return (Map<String, Long>) (Object) rank.getValues(false);
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

        private final ItemStack disabled = new ItemStack(Material.BARRIER, 1);
        private final ItemStack attack = new ItemStack(Material.STONE_AXE, 1) {{
            val meta = getItemMeta();
            meta.setDisplayName("§6§l点击左键狂扁小海螺");
            setItemMeta(meta);
        }};

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(KBXHLEvent.Start event) {
            storeInv(event.getPlayer());
            event.getPlayer().getInventory().clear();
            for (var i = 0; i < 9; i++) {
                event.getPlayer().getInventory().setItem(i, i == 4 ? attack : disabled);
            }
            event.getPlayer().getInventory().setHeldItemSlot(4);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(KBXHLEvent.Stop event) {
            loadInv(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(PlayerJoinEvent event) {
            val uid = event.getPlayer().getUniqueId().toString();
            if (inv.contains(uid)) loadInv(event.getPlayer());
        }

    }

}
