package io.izzel.kbxhl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.val;
import lombok.var;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class KBXHLGame {

    @Getter private final Set<UUID> players = new HashSet<>();
    private KBXHLBukkit instance;

    private List<Vector> positionForEndBricks;
    private List<Vector> positionForPurpurBlock;
    private List<Vector> positionForPurpleGlass;

    void init(KBXHLBukkit instance) {
        this.instance = instance;
        instance.getServer().getPluginManager().registerEvents(new Listener(), instance);
        val builderForEndBricks = ImmutableList.<Vector>builder();
        val builderForPurpurBlock = ImmutableList.<Vector>builder();
        val builderForPurpleGlass = ImmutableList.<Vector>builder();
        for (int i = -4; i <= 4; ++i) {
            for (int j = -4; j <= 4; ++j) {
                int maxSquared = Math.max(i * i, j * j);
                int minSquared = Math.min(i * i, j * j);
                if (minSquared < 4 * 4) {
                    builderForEndBricks.add(new Vector(i, -1, j));
                    builderForPurpleGlass.add(new Vector(i, 3, j));
                    if (maxSquared > 2 * 2) {
                        if (minSquared < 2 * 2 && maxSquared < 4 * 4) {
                            builderForPurpurBlock.add(new Vector(i, 0, j));
                            builderForPurpurBlock.add(new Vector(i, 1, j));
                            builderForPurpurBlock.add(new Vector(i, 2, j));
                        } else {
                            builderForEndBricks.add(new Vector(i, 0, j));
                            builderForEndBricks.add(new Vector(i, 1, j));
                            builderForEndBricks.add(new Vector(i, 2, j));
                        }
                    }
                }
            }
        }
        this.positionForEndBricks = builderForEndBricks.build();
        this.positionForPurpurBlock = builderForPurpurBlock.build();
        this.positionForPurpleGlass = builderForPurpleGlass.build();
    }

    private void fix(Player player, Location base, Vector offset) {
        if (positionForEndBricks.contains(offset)) {
            player.sendBlockChange(base.clone().add(offset), Material.END_BRICKS, (byte) 0);
        }
        if (positionForPurpurBlock.contains(offset)) {
            player.sendBlockChange(base.clone().add(offset), Material.PURPUR_BLOCK, (byte) 0);
        }
        if (positionForPurpleGlass.contains(offset)) {
            player.sendBlockChange(base.clone().add(offset), Material.STAINED_GLASS, (byte) 10);
        }
    }

    private void construct(Player player) {
        val location = player.getLocation();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        for (Vector vector : positionForEndBricks) {
            player.sendBlockChange(location.clone().add(vector), Material.END_BRICKS, (byte) 0);
        }
        for (Vector vector : positionForPurpurBlock) {
            player.sendBlockChange(location.clone().add(vector), Material.PURPUR_BLOCK, (byte) 0);
        }
        for (Vector vector : positionForPurpleGlass) {
            player.sendBlockChange(location.clone().add(vector), Material.STAINED_GLASS, (byte) 10);
        }
        player.teleport(location.clone().add(0.5, 0, 0.5).setDirection(new Vector()));
    }

    private void destruct(Player player) {
        val location = player.getLocation();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        for (Vector vector : positionForEndBricks) {
            val loc = location.clone().add(vector);
            player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
        }
        for (Vector vector : positionForPurpurBlock) {
            val loc = location.clone().add(vector);
            player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
        }
        for (Vector vector : positionForPurpleGlass) {
            val loc = location.clone().add(vector);
            player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
        }
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity.hasMetadata("kbxhl_creator") &&
                    entity.getMetadata("kbxhl_creator").stream().anyMatch(it -> it.asInt() == player.getEntityId())) {
                entity.remove();
            }
        }
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler
        public void on(KBXHLEvent.Start event) {
            players.add(event.getPlayer().getUniqueId());
            construct(event.getPlayer());
            instance.getServer().getPluginManager().registerEvents(new GameListener(event.getPlayer().getUniqueId()), instance);
        }

        @EventHandler
        public void on(KBXHLEvent.Stop event) {
            players.remove(event.getPlayer().getUniqueId());
            destruct(event.getPlayer());
        }

    }

    private class GameListener implements org.bukkit.event.Listener {
        private final UUID player;

        private GameListener(UUID player) {
            this.player = player;
            new ShulkerSpawnTask().runTaskTimer(instance, 60, 9);
        }

        @EventHandler
        public void on(EntityDamageByEntityEvent event) {
            if (event.getDamager().getUniqueId().equals(player)) {
                val player = ((Player) event.getDamager());
                val entity = event.getEntity();
                if (entity instanceof Shulker && entity.hasMetadata("kbxhl_creator")) {
                    val name = entity.getCustomName();
                    val score = Statics.SCORE.get(name);
                    instance.getScoreManager().add(player, score);
                    event.setCancelled(true);
                    entity.remove();
                }
            }
        }

        @EventHandler
        public void on(PlayerInteractEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    val base = event.getPlayer().getLocation();
                    base.setX(base.getBlockX());
                    base.setY(base.getBlockY());
                    base.setZ(base.getBlockZ());
                    val offset = event.getClickedBlock().getLocation().subtract(base).toVector();
                    fix(event.getPlayer(), base, offset);
                }
            }
        }

        // 为什么水桶没有 AffectSlotEvent，真草

        @EventHandler
        public void on(InventoryMoveItemEvent event) {
            if (Optional.ofNullable(event.getSource().getHolder()).filter(Player.class::isInstance).map(Player.class::cast)
                    .filter(it -> it.getUniqueId().equals(player)).isPresent()) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void on(PlayerSwapHandItemsEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void on(PlayerDropItemEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void on(InventoryOpenEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void on(PlayerItemHeldEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void on(PlayerMoveEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(KBXHLEvent.Stop event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                HandlerList.unregisterAll(this);
            }
        }

        @EventHandler
        public void on(PlayerQuitEvent event) {
            if (event.getPlayer().getUniqueId().equals(player)) {
                HandlerList.unregisterAll(this);
            }
        }

        private class ShulkerSpawnTask extends BukkitRunnable {

            private final Random random = new Random();
            private List<Vector> seq = new ArrayList<>(positionForPurpurBlock);
            private int pointer = seq.size();

            @Override
            public void run() {
                val player = instance.getServer().getPlayer(GameListener.this.player);
                if (player == null || !players.contains(GameListener.this.player)) {
                    cancel();
                    return;
                }
                var amount = random.nextInt(4);
                while (amount-- != 0) spawn();
            }

            private void spawn() {
                if (pointer == seq.size()) {
                    Collections.shuffle(seq, random);
                    pointer = 0;
                }
                val player = instance.getServer().getPlayer(GameListener.this.player);
                val offset = seq.get(pointer++);
                val base = player.getLocation();
                base.setX(base.getBlockX());
                base.setY(base.getBlockY());
                base.setZ(base.getBlockZ());
                val loc = base.clone().add(offset);
                val angle = player.getEyeLocation().getDirection().angle(loc.clone().subtract(base).toVector());
                if ((angle / Math.PI) < random.nextDouble()) { // 游戏嘛就是要难一点
                    player.sendBlockChange(loc, Material.AIR, ((byte) 0));
                    val shulker = player.getWorld().spawn(loc, Shulker.class);
                    shulker.setAI(false);
                    shulker.setGravity(false);
                    val name = name();
                    shulker.setCustomName(name);
                    shulker.setCustomNameVisible(true);
                    shulker.setColor(Statics.COLOR.get(name));
                    shulker.setMetadata("kbxhl_creator", new FixedMetadataValue(instance, player.getEntityId()));
                    val tick = Statics.TICK.get(name);
                    instance.getServer().getScheduler().runTaskLater(instance, () -> {
                        if (shulker.isValid()) {
                            shulker.remove();
                        }
                        if (players.contains(player.getUniqueId())) {
                            fix(player, base, offset);
                        }
                    }, tick);
                }
            }

            private String name() {
                val num = random.nextInt(23);
                if (num < 16) return Statics.SHULKER_NAME_N;
                if (num < 20) return Statics.SHULKER_NAME_R;
                if (num < 22) return Statics.SHULKER_NAME_SR;
                return Statics.SHULKER_NAME_SSR;
            }

        }

    }

    public static class Statics {
        public static final String SHULKER_NAME_SSR = "§6特级稀有海螺";
        public static final String SHULKER_NAME_SR = "§6超级稀有海螺";
        public static final String SHULKER_NAME_R = "§6稀有海螺";
        public static final String SHULKER_NAME_N = "§6普通海螺";
        public static final Map<String, Integer> SCORE = ImmutableMap.of(
                SHULKER_NAME_N, 1,
                SHULKER_NAME_R, 5,
                SHULKER_NAME_SR, 25,
                SHULKER_NAME_SSR, 125
        );
        public static final Map<String, Integer> TICK = ImmutableMap.of(
                SHULKER_NAME_N, 54,
                SHULKER_NAME_R, 51,
                SHULKER_NAME_SR, 45,
                SHULKER_NAME_SSR, 27
        );
        public static final Map<String, DyeColor> COLOR = ImmutableMap.of(
                SHULKER_NAME_N, DyeColor.PURPLE,
                SHULKER_NAME_R, DyeColor.PINK,
                SHULKER_NAME_SR, DyeColor.ORANGE,
                SHULKER_NAME_SSR, DyeColor.YELLOW
        );
    }

}
