package com.github.ustc_zzzz.kbxhl;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeStructure
{
    private final Random random = new Random();

    private final KBXHLSponge plugin;
    private final List<Vector3i> positionForEndBricks;
    private final List<Vector3i> positionForPurpurBlock;
    private final List<Vector3i> positionForPurpleGlass;

    private BlockState endBricks = BlockSnapshot.NONE.getState();
    private BlockState purpurBlock = BlockSnapshot.NONE.getState();
    private BlockState purpleGlass = BlockSnapshot.NONE.getState();

    private EnumMap<Direction, Byte> directionMap = new EnumMap<>(Direction.class);

    KBXHLSpongeStructure(KBXHLSponge plugin)
    {
        ImmutableList.Builder<Vector3i> builderForEndBricks = ImmutableList.builder();
        ImmutableList.Builder<Vector3i> builderForPurpurBlock = ImmutableList.builder();
        ImmutableList.Builder<Vector3i> builderForPurpleGlass = ImmutableList.builder();
        for (int i = -4; i <= 4; ++i)
        {
            for (int j = -4; j <= 4; ++j)
            {
                int maxSquared = Math.max(i * i, j * j);
                int minSquared = Math.min(i * i, j * j);
                if (minSquared < 4 * 4)
                {
                    builderForEndBricks.add(new Vector3i(i, -1, j));
                    builderForPurpleGlass.add(new Vector3i(i, 3, j));
                    if (maxSquared > 2 * 2)
                    {
                        if (minSquared < 2 * 2 && maxSquared < 4 * 4)
                        {
                            builderForPurpurBlock.add(new Vector3i(i, 0, j));
                            builderForPurpurBlock.add(new Vector3i(i, 1, j));
                            builderForPurpurBlock.add(new Vector3i(i, 2, j));
                        }
                        else
                        {
                            builderForEndBricks.add(new Vector3i(i, 0, j));
                            builderForEndBricks.add(new Vector3i(i, 1, j));
                            builderForEndBricks.add(new Vector3i(i, 2, j));
                        }
                    }
                }
            }
        }
        this.plugin = plugin;
        this.positionForEndBricks = builderForEndBricks.build();
        this.positionForPurpurBlock = builderForPurpurBlock.build();
        this.positionForPurpleGlass = builderForPurpleGlass.build();
    }

    void init()
    {
        this.endBricks = BlockTypes.END_BRICKS.getDefaultState();
        this.purpurBlock = BlockTypes.PURPUR_BLOCK.getDefaultState();
        this.purpleGlass = BlockTypes.STAINED_GLASS.getDefaultState().with(Keys.DYE_COLOR, DyeColors.PURPLE).get();
    }

    public int scoreFor(Player player, Shulker entity)
    {
        if (entity.getCreator().filter(player.getUniqueId()::equals).isPresent())
        {
            if (!entity.get(Keys.INVISIBLE).orElse(Boolean.TRUE))
            {
                ParticleEffect effect = ParticleEffect.builder().type(ParticleTypes.FIREWORKS_SPARK).build();
                entity.getWorld().spawnParticles(effect, entity.getLocation().getPosition());
                entity.offer(Keys.INVISIBLE, Boolean.TRUE);
                return ShulkerIterator.getScore(entity);
            }
        }
        return 0;
    }

    public void repairFor(Player player, Vector3i offset)
    {
        Vector3i baseVector = player.getPosition().toInt();

        if (this.positionForEndBricks.contains(offset))
        {
            player.sendBlockChange(baseVector.add(offset), this.endBricks);
        }
        if (this.positionForPurpleGlass.contains(offset))
        {
            player.sendBlockChange(baseVector.add(offset), this.purpleGlass);
        }
    }

    public void destructFor(Player player)
    {
        Vector3i baseVector = player.getPosition().toInt();

        for (Vector3i offset : this.positionForEndBricks)
        {
            player.resetBlockChange(baseVector.add(offset));
        }
        for (Vector3i offset : this.positionForPurpurBlock)
        {
            player.resetBlockChange(baseVector.add(offset));
        }
        for (Vector3i offset : this.positionForPurpleGlass)
        {
            player.resetBlockChange(baseVector.add(offset));
        }

        for (Entity entity : player.getWorld().getNearbyEntities(baseVector.toDouble(), 5))
        {
            if (entity instanceof Shulker && entity.getCreator().filter(player.getUniqueId()::equals).isPresent())
            {
                entity.remove();
            }
        }
    }

    public ShulkerIterator constructFor(Player player)
    {
        Location<World> location = player.getLocation();
        Vector3i baseVector = location.getBlockPosition();
        ShulkerIterator iterator = new ShulkerIterator(location, this.positionForPurpurBlock);

        for (Vector3i offset : this.positionForEndBricks)
        {
            player.sendBlockChange(baseVector.add(offset), this.endBricks);
        }
        for (Vector3i offset : this.positionForPurpurBlock)
        {
            player.sendBlockChange(baseVector.add(offset), this.purpurBlock);
        }
        for (Vector3i offset : this.positionForPurpleGlass)
        {
            player.sendBlockChange(baseVector.add(offset), this.purpleGlass);
        }

        player.setLocation(baseVector.toDouble().add(0.5, 0, 0.5), player.getWorld().getUniqueId());
        player.setRotation(Vector3d.ZERO);
        return iterator;
    }

    public void summonFor(Player player, ShulkerIterator iterator)
    {
        Optional<Shulker> shulkerOptional = iterator.next();
        if (shulkerOptional.isPresent())
        {
            Shulker shulker = shulkerOptional.get();
            int duringTicks = ShulkerIterator.getDuringTicks(shulker);

            Consumer<Task> executor = task ->
            {
                if (shulker.isRemoved())
                {
                    player.resetBlockChange(shulker.getLocation().getBlockPosition());
                }
                else
                {
                    shulker.remove();
                    player.sendBlockChange(shulker.getLocation().getBlockPosition(), this.purpurBlock);
                }
            };
            Task.builder().delayTicks(duringTicks).execute(executor).submit(this.plugin);

            player.resetBlockChange(shulker.getLocation().getBlockPosition());
            shulker.setCreator(player.getUniqueId());
        }
    }

    public static class ShulkerIterator implements Iterator<Optional<Shulker>>
    {
        private static final Text SHULKER_NAME_SSR = Text.of(TextColors.GOLD, "特级稀有海螺");
        private static final Text SHULKER_NAME_SR = Text.of(TextColors.GOLD, "超级稀有海螺");
        private static final Text SHULKER_NAME_R = Text.of(TextColors.GOLD, "稀有海螺");
        private static final Text SHULKER_NAME_N = Text.of(TextColors.GOLD, "普通海螺");

        private static final EnumMap<Direction, Byte> DIRECTION_MAP = new EnumMap<>(Direction.class);
        private static final String SHUKLER_LEVEL_SEQ = "NDCDBDCDNDCDADCDNDCDBDCD";
        private static final Random RANDOM = new Random();

        static
        {
            DIRECTION_MAP.put(Direction.SOUTH, (byte) 2);
            DIRECTION_MAP.put(Direction.NORTH, (byte) 3);
            DIRECTION_MAP.put(Direction.EAST, (byte) 4);
            DIRECTION_MAP.put(Direction.WEST, (byte) 5);
        }

        private ArrayList<Vector3i> offsets;
        private final Location<World> base;
        private int pointer = -1;

        private ShulkerIterator(Location<World> base, Collection<? extends Vector3i> offsets)
        {
            this.base = base;
            this.offsets = new ArrayList<>(offsets);
            Collections.shuffle(this.offsets, RANDOM);
        }

        @Override
        public boolean hasNext()
        {
            return true;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Optional<Shulker> next()
        {
            int maxPointer = SHUKLER_LEVEL_SEQ.length();
            if (++this.pointer >= maxPointer)
            {
                int size = this.offsets.size(), beforeSize = size - maxPointer;

                List<Vector3i> after = this.offsets.subList(beforeSize, size);
                List<Vector3i> before = this.offsets.subList(0, beforeSize);

                this.offsets = new ArrayList<>(after);
                Collections.shuffle(this.offsets);

                this.offsets.addAll(before);
                this.pointer %= maxPointer;
            }

            Vector3d offset = this.offsets.get(this.pointer).toDouble();
            Vector3d position = this.base.getPosition().add(offset);
            World world = this.base.getExtent();

            DataContainer data = world.createEntity(EntityTypes.SHULKER, position).createSnapshot().toContainer();
            Direction direction = Direction.getClosestHorizontal(offset, Direction.Division.CARDINAL);

            data.set(DataQuery.of("UnsafeData", "AttachFace"), DIRECTION_MAP.get(direction));
            data.set(DataQuery.of("UnsafeData", "NoAI"), (byte) 1);

            switch (SHUKLER_LEVEL_SEQ.charAt(this.pointer))
            {
            case 'D':
                String customNameD = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_N);
                data.set(DataQuery.of("UnsafeData", "Color"), (byte) 10); // purple (N)
                data.set(DataQuery.of("UnsafeData", "CustomName"), customNameD);
                break;
            case 'C':
                String customNameC = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_R);
                data.set(DataQuery.of("UnsafeData", "Color"), (byte) 6); // pink (R)
                data.set(DataQuery.of("UnsafeData", "CustomName"), customNameC);
                break;
            case 'B':
                String customNameB = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_SR);
                data.set(DataQuery.of("UnsafeData", "Color"), (byte) 1); // orange (SR)
                data.set(DataQuery.of("UnsafeData", "CustomName"), customNameB);
                break;
            case 'A':
                String customNameA = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_SSR);
                data.set(DataQuery.of("UnsafeData", "Color"), (byte) 4); // yellow (SSR)
                data.set(DataQuery.of("UnsafeData", "CustomName"), customNameA);
                break;
            default:
                return Optional.empty();
            }

            return EntitySnapshot.builder().build(data).flatMap(EntitySnapshot::restore).map(Shulker.class::cast);
        }

        static int getDuringTicks(Shulker shulker)
        {
            Text text = shulker.get(Keys.DISPLAY_NAME).orElse(Text.of());
            if (text.equals(SHULKER_NAME_N))
            {
                return 54;
            }
            if (text.equals(SHULKER_NAME_R))
            {
                return 90;
            }
            if (text.equals(SHULKER_NAME_SR))
            {
                return 102;
            }
            if (text.equals(SHULKER_NAME_SSR))
            {
                return 108;
            }
            return 0;
        }

        private static int getScore(Shulker shulker)
        {
            Text text = shulker.get(Keys.DISPLAY_NAME).orElse(Text.of());
            if (text.equals(SHULKER_NAME_N))
            {
                return 1;
            }
            if (text.equals(SHULKER_NAME_R))
            {
                return 5;
            }
            if (text.equals(SHULKER_NAME_SR))
            {
                return 25;
            }
            if (text.equals(SHULKER_NAME_SSR))
            {
                return 125;
            }
            return 0;
        }
    }
}
