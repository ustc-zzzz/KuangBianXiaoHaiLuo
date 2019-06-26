package io.izzel.kbxhl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public class KBXHLScoreManager {

    private static final double MAX = 651.0D;

    private KBXHLBukkit instance;

    @Getter private final Map<Player, Integer> scores = new WeakHashMap<>();
    @Getter private final Map<Player, BossBar> bossBar = new WeakHashMap<>();
    @Getter private final Map<Player, Integer> toAdd = new WeakHashMap<>();

    void init(KBXHLBukkit instance) {
        this.instance = instance;
        instance.getServer().getScheduler().runTaskTimer(instance, this::updateBar, 20, 20);
        instance.getServer().getPluginManager().registerEvents(new Listener(), instance);
    }

    private void updateBar() {
        val iterator = toAdd.entrySet().iterator();
        while (iterator.hasNext()) {
            val entry = iterator.next();
            val player = entry.getKey();
            val value = entry.getValue();
            val bar = this.bossBar.get(player);
            val score = scores.getOrDefault(player, 0) + value;
            scores.put(player, score);
            val percent = Math.min(score / MAX, 1.0D);
            bar.setProgress(percent);
            iterator.remove();
        }
    }

    public void add(Player player, int score) {
        toAdd.compute(player, (k, v) -> Optional.ofNullable(v).map(it -> it + score).orElse(score));
    }

    public int score(Player player) {
        return scores.getOrDefault(player, 0);
    }

    public Duration complete(Player player) {
        try {
            val start = Instant.ofEpochMilli(player.getMetadata("kbxhl_start").get(0).asLong());
            player.removeMetadata("kbxhl_start", instance);
            scores.remove(player);
            Optional.ofNullable(bossBar.remove(player)).ifPresent(it -> it.removePlayer(player));
            toAdd.remove(player);
            return Duration.between(start, Instant.now());
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler
        public void on(KBXHLEvent.Start event) {
            val player = event.getPlayer();
            player.setMetadata("kbxhl_start", new FixedMetadataValue(instance, System.currentTimeMillis()));
            val bar = instance.getServer().createBossBar("§6§l狂扁小海螺", BarColor.PURPLE, BarStyle.SOLID);
            bar.addPlayer(player);
            bossBar.put(player, bar);
            instance.getServer().getPluginManager().registerEvents(new GameListener(player.getUniqueId()), instance);
        }

        @EventHandler
        public void on(KBXHLEvent.Stop event) {
            val player = event.getPlayer();
            val score = score(player);
            val duration = complete(player);
            if (score < MAX) player.sendMessage("未完成游戏");
            else {
                KBXHLBukkit.instance().getConfig().updateRank(player, duration);
                int seconds = Math.toIntExact(duration.getSeconds()), milliseconds = duration.getNano() / 1_000_000;
                player.sendMessage("=> " + String.format("%d.%03d", seconds, milliseconds) + " seconds");
            }
        }

    }

    @RequiredArgsConstructor
    private class GameListener implements org.bukkit.event.Listener {
        private final UUID player;

        @EventHandler
        public void on(KBXHLEvent.Stop event) {

        }

        @EventHandler
        public void on(PlayerQuitEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                instance.getServer().getPluginManager().callEvent(new KBXHLEvent.Stop(event.getPlayer()));
                HandlerList.unregisterAll(this);
            }
        }

    }


}
