package com.github.ustc_zzzz.kbxhl;

import com.flowpowered.math.vector.Vector3i;
import com.github.ustc_zzzz.kbxhl.event.KBXHLEvent;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
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
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.living.player.Player;
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
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeConfiguration
{
    private final Logger logger;
    private final KBXHLSponge plugin;
    private final TypeToken<Config> typeToken = TypeToken.of(Config.class);

    private Config config = new Config();

    KBXHLSpongeConfiguration(KBXHLSponge plugin)
    {
        this.plugin = plugin;
        this.logger = plugin.logger;
    }

    void init()
    {
        this.config = this.getConfig();
        EventManager eventManager = Sponge.getEventManager();
        eventManager.registerListeners(this.plugin, this.new EventListener());
    }

    private Config getConfig()
    {
        try
        {
            CommentedConfigurationNode node = this.plugin.loader.load();
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
            CommentedConfigurationNode node = this.plugin.loader.createEmptyNode();
            this.plugin.loader.save(node.setValue(this.typeToken, config));
        }
        catch (ObjectMappingException | IOException e)
        {
            this.logger.error(e.getMessage(), e);
        }
    }

    public class EventListener
    {
        private final Base64.Decoder decoder = Base64.getUrlDecoder();
        private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        private final ItemStack disabledItem = ItemStack.of(ItemTypes.BARRIER, 1);
        private final ItemStack handheldItem = ItemStack.builder().itemType(ItemTypes.STONE_AXE).quantity(1)
                .add(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, TextStyles.BOLD, "点击左键狂扁小海螺")).build();

        @Listener
        public void on(AffectSlotEvent event, @First Player player)
        {
            if (config.playerInventories.containsKey(player.getUniqueId()))
            {
                event.setCancelled(true);
            }
        }

        @Listener
        public void on(MoveEntityEvent event)
        {
            if (config.playerInventories.containsKey(event.getTargetEntity().getUniqueId()))
            {
                event.setToTransform(event.getToTransform().setLocation(event.getFromTransform().getLocation()));
            }
        }

        @Listener
        public void on(InteractBlockEvent event, @First Player player)
        {
            if (config.playerInventories.containsKey(player.getUniqueId()))
            {
                event.setCancelled(true);
                Vector3i offset = event.getTargetBlock().getPosition().sub(player.getPosition().toInt());
                Task.builder().execute(task -> plugin.structure.repairFor(player, offset)).submit(plugin);
            }
        }

        @Listener
        public void on(AttackEntityEvent event, @First EntityDamageSource source)
        {
            Entity entity = event.getTargetEntity();
            if (entity instanceof Shulker && source.getSource() instanceof Player)
            {
                if (entity.getCreator().filter(source.getSource().getUniqueId()::equals).isPresent())
                {
                    ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.FIREWORKS_SPARK).build();
                    entity.getWorld().spawnParticles(effect, entity.getLocation().getPosition());
                    entity.offer(Keys.INVISIBLE, Boolean.TRUE);
                }
            }
        }

        @Listener
        public void on(KBXHLEvent.Start event, @Root Player player)
        {
            UUID uuid = player.getUniqueId();
            if (!config.playerInventories.containsKey(uuid))
            {
                PlayerInventory inventory = (PlayerInventory) player.getInventory();
                DataContainer data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
                for (Slot slot : inventory.<Slot>slots())
                {
                    String key = slot.getProperties(SlotIndex.class).iterator().next().toString();
                    slot.poll().ifPresent(item -> data.set(DataQuery.of(key), item));
                }
                try (ByteArrayOutputStream output = new ByteArrayOutputStream())
                {
                    DataFormats.NBT.writeTo(output, data);

                    Hotbar hotbar = inventory.getHotbar();
                    hotbar.set(SlotIndex.of(0), disabledItem);
                    hotbar.set(SlotIndex.of(1), disabledItem);
                    hotbar.set(SlotIndex.of(2), disabledItem);
                    hotbar.set(SlotIndex.of(3), disabledItem);
                    hotbar.set(SlotIndex.of(4), handheldItem);
                    hotbar.set(SlotIndex.of(5), disabledItem);
                    hotbar.set(SlotIndex.of(6), disabledItem);
                    hotbar.set(SlotIndex.of(7), disabledItem);
                    hotbar.set(SlotIndex.of(8), disabledItem);
                    hotbar.setSelectedSlotIndex(1 + 1 + 1 + 1);

                    Stack<Vector3i> stack = new Stack<>();
                    plugin.structure.constructFor(player, stack);
                    config.playerInventories.put(uuid, encoder.encodeToString(output.toByteArray()));

                    Consumer<Task> consumer = task ->
                    {
                        if (config.playerInventories.containsKey(uuid))
                        {
                            plugin.structure.summonFor(player, stack);
                        }
                        else
                        {
                            task.cancel();
                        }
                    };
                    Task.builder().intervalTicks(18).execute(consumer).submit(plugin);
                }
                catch (IOException e)
                {
                    logger.error(e.getMessage(), e);
                }
                setConfig(config);
            }
        }

        @Listener
        public void on(KBXHLEvent.Stop event, @Root Player player)
        {
            String value = config.playerInventories.remove(player.getUniqueId());

            plugin.structure.destructFor(player);

            if (Objects.nonNull(value))
            {
                DataContainer data;
                PlayerInventory inventory = (PlayerInventory) player.getInventory();
                try (ByteArrayInputStream input = new ByteArrayInputStream(decoder.decode(value)))
                {
                    data = DataFormats.NBT.readFrom(input);
                }
                catch (IOException e)
                {
                    logger.error(e.getMessage(), e);
                    data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
                }
                for (Slot slot : inventory.<Slot>slots())
                {
                    String key = slot.getProperties(SlotIndex.class).iterator().next().toString();
                    slot.set(data.getSerializable(DataQuery.of(key), ItemStack.class).orElse(ItemStack.empty()));
                }
                setConfig(config);
            }
        }

        @Listener
        public void on(ClientConnectionEvent.Join event)
        {
            Player player = event.getTargetEntity();
            String value = config.playerInventories.remove(player.getUniqueId());
            if (Objects.nonNull(value))
            {
                DataContainer data;
                PlayerInventory inventory = (PlayerInventory) player.getInventory();
                try (ByteArrayInputStream input = new ByteArrayInputStream(decoder.decode(value)))
                {
                    data = DataFormats.NBT.readFrom(input);
                }
                catch (IOException e)
                {
                    logger.error(e.getMessage(), e);
                    data = DataContainer.createNew(DataView.SafetyMode.NO_DATA_CLONED);
                }
                for (Slot slot : inventory.<Slot>slots())
                {
                    String key = slot.getProperties(SlotIndex.class).iterator().next().toString();
                    slot.set(data.getSerializable(DataQuery.of(key), ItemStack.class).orElse(ItemStack.empty()));
                }
                setConfig(config);
            }
        }
    }

    @ConfigSerializable
    public static class Config
    {
        @Setting(value = "player-inventories")
        private Map<UUID, String> playerInventories = new TreeMap<>();
    }
}
