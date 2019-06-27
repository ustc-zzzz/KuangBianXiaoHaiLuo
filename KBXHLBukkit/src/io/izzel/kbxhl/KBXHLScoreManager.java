package io.izzel.kbxhl;

import lombok.Getter;
import lombok.val;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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
            if (score >= MAX) instance.getServer().getPluginManager().callEvent(new KBXHLEvent.Stop(player));
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
            val start = player.getMetadata("kbxhl_start").get(0).asLong();
            player.removeMetadata("kbxhl_start", instance);
            scores.remove(player);
            Optional.ofNullable(bossBar.remove(player)).ifPresent(it -> it.removePlayer(player));
            toAdd.remove(player);
            if (start < System.currentTimeMillis())
                return Duration.between(Instant.ofEpochMilli(start), Instant.now());
            else return Duration.ZERO;
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(KBXHLEvent.Start event) {
            val player = event.getPlayer();
            player.setMetadata("kbxhl_start", new FixedMetadataValue(instance, System.currentTimeMillis() + 3000));
            val bar = instance.getServer().createBossBar("§6§l狂扁小海螺", BarColor.PURPLE, BarStyle.SOLID);
            bar.setProgress(0.0d);
            bar.addPlayer(player);
            bossBar.put(player, bar);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(KBXHLEvent.Stop event) {
            val player = event.getPlayer();
            val score = score(player);
            val duration = complete(player);
            if (score < MAX) player.sendMessage("未完成游戏");
            else {
                KBXHLBukkit.instance().getConf().updateRank(player, duration);
                int seconds = Math.toIntExact(duration.getSeconds()), milliseconds = duration.getNano() / 1_000_000;
                player.sendMessage("=> " + String.format("%d.%03d", seconds, milliseconds) + " seconds");
            }
        }

    }


}
