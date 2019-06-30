package com.github.ustc_zzzz.kbxhl;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.github.ustc_zzzz.kbxhl.event.KBXHLEvent;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.AffectSlotEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLConfig
{
    private static final int MAX_RANK_SIZE = 10;

    private final Logger logger;
    private final KBXHLSponge plugin;
    private final HoconConfigurationLoader loader;
    private final TypeToken<Config> typeToken = TypeToken.of(Config.class);

    private final Base64.Decoder decoder = Base64.getUrlDecoder();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    private Config config = new Config();

    KBXHLConfig(KBXHLSponge plugin)
    {
        this.plugin = plugin;
        this.logger = plugin.logger;
        this.loader = HoconConfigurationLoader.builder().setPath(plugin.configPath).build();
    }

    void init()
    {
        this.config = this.getConfig();
        EventManager eventManager = Sponge.getEventManager();
        eventManager.registerListeners(this.plugin, this.new EventListener());
    }


    public Text toTopRankListText()
    {
        UserStorageService service = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
        int size = Math.min(MAX_RANK_SIZE, config.playerBest.size());
        List<Text> topRankList = new ArrayList<>();
        for (int rank = 0; rank < size; ++rank)
        {
            UUID uuid = config.playerBest.get(rank);
            String order = rank <= 1 ? "1st" : rank <= 2 ? "2nd" : rank <= 3 ? "3rd" : rank + "th";
            String name = Strings.padEnd(service.get(uuid).map(User::getName).orElse("-"), 20, ' ');
            String time = config.playerFinishTime.getOrDefault(uuid, "         -         ");
            String score = config.playerDurations.containsKey(uuid) ? String.valueOf(config.playerDurations.get(uuid) / 1e3) : "-";
            topRankList.add(Text.of(order, "：", name, "于", time, "刷新，共耗时", score, "s"));
        }
        return Text.joinWith(Text.NEW_LINE, topRankList);
    }

    private Config getConfig()
    {
        try
        {
            CommentedConfigurationNode node = this.loader.load();
            return node.<Config>getValue(this.typeToken, Config::new);
        }
        catch (IOException | ObjectMappingException e)
        {
            this.logger.error(e.getMessage(), e);
            return new Config();
        }
    }

    private void setConfig(Config config)
    {
        try
        {
            CommentedConfigurationNode node = this.loader.createEmptyNode();
            this.loader.save(node.setValue(this.typeToken, config));
        }
        catch (ObjectMappingException | IOException e)
        {
            this.logger.error(e.getMessage(), e);
        }
    }

    private void insertInventory(Player player, Inventory inventory)
    {
        UUID uuid = player.getUniqueId();
        if (!config.playerInventories.containsKey(uuid))
        {
            DataContainer data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
            for (Slot slot : inventory.<Slot>slots())
            {
                String key = slot.getProperties(SlotIndex.class).iterator().next().toString();
                slot.poll().ifPresent(item -> data.set(DataQuery.of(key), item));
            }
            player.get(Keys.IS_FLYING).ifPresent(isFlying -> data.set(Keys.IS_FLYING, isFlying));
            Vector3d position = player.getPosition();
            data.set(DataQuery.of("Pos"), position);
            try (ByteArrayOutputStream o = new ByteArrayOutputStream(); OutputStream output = new GZIPOutputStream(o))
            {
                DataFormats.NBT.writeTo(output, data);
                config.playerInventories.put(uuid, encoder.encodeToString(o.toByteArray()));
            }
            catch (IOException e)
            {
                logger.error(e.getMessage(), e);
            }
            player.setLocation(new Location<>(player.getWorld(), position.getX(), 252, position.getZ()));
            player.offer(Keys.IS_FLYING, Boolean.TRUE);
            setConfig(config);
        }
    }

    private void removeInventory(Player player)
    {
        String value = config.playerInventories.remove(player.getUniqueId());

        if (Objects.nonNull(value))
        {
            DataContainer data;
            PlayerInventory inventory = (PlayerInventory) player.getInventory();
            try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(decoder.decode(value))))
            {
                data = DataFormats.NBT.readFrom(input);
            }
            catch (IOException e)
            {
                logger.error(e.getMessage(), e);
                data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
            }
            Optional<Vector3d> positionOptional = data.getObject(DataQuery.of("Pos"), Vector3d.class);
            Optional<Boolean> isFlyingOptional = data.getBoolean(Keys.IS_FLYING.getQuery());
            for (Slot slot : inventory.<Slot>slots())
            {
                String key = slot.getProperties(SlotIndex.class).iterator().next().toString();
                slot.set(data.getSerializable(DataQuery.of(key), ItemStack.class).orElse(ItemStack.empty()));
            }
            positionOptional.ifPresent(p -> player.setLocation(new Location<>(player.getWorld(), p)));
            isFlyingOptional.ifPresent(isFlyingBool -> player.offer(Keys.IS_FLYING, isFlyingBool));
            setConfig(config);
        }
    }

    public class EventListener
    {
        private final ItemStack disabledItem = ItemStack.of(ItemTypes.BARRIER, 1);
        private final ItemStack handheldItem = ItemStack.builder().itemType(ItemTypes.STONE_AXE).quantity(1)
                .add(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, TextStyles.BOLD, "点击左键狂扁小海螺")).build();

        @Listener
        public void on(AffectSlotEvent event, @First Player player)
        {
            if (plugin.command.has(player).asBoolean())
            {
                event.setCancelled(true);
            }
        }

        @Listener
        public void on(MoveEntityEvent event)
        {
            Entity entity = event.getTargetEntity();
            if (entity instanceof Player && plugin.command.has((Player) entity).asBoolean())
            {
                event.setToTransform(event.getToTransform().setLocation(event.getFromTransform().getLocation()));
            }
        }

        @Listener
        public void on(InteractBlockEvent event, @First Player player)
        {
            if (!Tristate.FALSE.equals(plugin.command.has(player)))
            {
                event.setCancelled(true);
                Vector3i offset = event.getTargetBlock().getPosition().sub(player.getPosition().toInt());
                Task.builder().execute(task -> plugin.structure.repairFor(player, offset)).submit(plugin);
            }
        }

        @Listener
        public void on(AttackEntityEvent event, @First EntityDamageSource source)
        {
            Entity sourceEntity = source.getSource();
            Entity targetEntity = event.getTargetEntity();
            if (targetEntity instanceof Shulker && sourceEntity instanceof Player)
            {
                Player player = (Player) sourceEntity;
                int score = plugin.structure.scoreFor(player, ((Shulker) targetEntity));
                if (plugin.scoreManager.add(player, score) >= KBXHLScoreManager.MAX_SCORE)
                {
                    plugin.command.stop(player);
                }
            }
        }

        @Listener
        public void on(KBXHLEvent.Start event, @Root Player player)
        {
            Inventory inventory = player.getInventory();
            Hotbar hotbar = ((PlayerInventory) inventory).getHotbar();

            insertInventory(player, inventory);

            plugin.scoreManager.add(player, 0);

            hotbar.setSelectedSlotIndex(4);
            hotbar.set(SlotIndex.of(0), disabledItem);
            hotbar.set(SlotIndex.of(1), disabledItem);
            hotbar.set(SlotIndex.of(2), disabledItem);
            hotbar.set(SlotIndex.of(3), disabledItem);
            hotbar.set(SlotIndex.of(4), handheldItem);
            hotbar.set(SlotIndex.of(5), disabledItem);
            hotbar.set(SlotIndex.of(6), disabledItem);
            hotbar.set(SlotIndex.of(7), disabledItem);
            hotbar.set(SlotIndex.of(8), disabledItem);

            KBXHLGameStructure.ShulkerIterator iterator = plugin.structure.constructFor(player);

            Consumer<Task> consumer = task ->
            {
                if (plugin.command.has(player).asBoolean())
                {
                    plugin.structure.summonFor(player, iterator);
                }
                else
                {
                    task.cancel();
                }
            };
            Task.builder().intervalTicks(18).execute(consumer).submit(plugin);
        }

        @Listener
        public void on(KBXHLEvent.Stop event, @Root Player player)
        {
            plugin.structure.destructFor(player);
            boolean succeed = plugin.scoreManager.add(player, 0) >= KBXHLScoreManager.MAX_SCORE;

            Duration d = plugin.scoreManager.remove(player);
            if (succeed)
            {
                UUID uuid = player.getUniqueId();
                int newDuration = Math.toIntExact(d.toMillis());
                int oldDuration = config.playerDurations.getOrDefault(uuid, Integer.MAX_VALUE);
                if (newDuration < oldDuration)
                {
                    player.sendMessage(Text.of("狂扁共用时：", newDuration / 1e3, "s", "（个人新记录！）"));

                    config.playerFinishTime.put(uuid, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
                    config.playerDurations.put(uuid, newDuration);
                    config.playerBest.remove(uuid);
                    config.playerBest.add(uuid);

                    ToIntFunction<UUID> function = k -> config.playerDurations.getOrDefault(k, Integer.MAX_VALUE);
                    config.playerBest.sort(Comparator.comparingInt(function));

                    int rank = config.playerBest.lastIndexOf(uuid);
                    if (rank < MAX_RANK_SIZE)
                    {
                        config.playerBest.add(rank, uuid);
                        MessageChannel broadcastChannel = Sponge.getServer().getBroadcastChannel();
                        String order = rank <= 1 ? "1st" : rank <= 2 ? "2nd" : rank <= 3 ? "3rd" : rank + "th";
                        broadcastChannel.send(Text.of("来自", player.getName(), "的狂扁小海螺新记录（", order, "）：", newDuration / 1e3, "s"));
                    }

                    if (config.playerBest.size() > MAX_RANK_SIZE)
                    {
                        config.playerBest.subList(MAX_RANK_SIZE, config.playerBest.size()).clear();
                    }

                    setConfig(config);
                }
                else
                {
                    player.sendMessage(Text.of("狂扁共用时：", newDuration / 1e3, "s"));
                }
            }

            removeInventory(player);
        }

        @Listener
        public void on(ClientConnectionEvent.Join event)
        {
            Player player = event.getTargetEntity();
            removeInventory(player);
        }
    }

    @ConfigSerializable
    public static class Config
    {
        @Setting(value = "player-inventories")
        private Map<UUID, String> playerInventories = new TreeMap<>();

        @Setting(value = "player-finish-time")
        private Map<UUID, String> playerFinishTime = new TreeMap<>();

        @Setting(value = "player-durations")
        private Map<UUID, Integer> playerDurations = new TreeMap<>();

        @Setting(value = "player-best")
        private List<UUID> playerBest = new ArrayList<>();
    }
}
