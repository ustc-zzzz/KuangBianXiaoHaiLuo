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
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeStructure
{
    private static final Text SHULKER_NAME_N = Text.of(TextColors.GOLD, "普通海螺");
    private static final Text SHULKER_NAME_R = Text.of(TextColors.GOLD, "稀有海螺");
    private static final Text SHULKER_NAME_SR = Text.of(TextColors.GOLD, "超级稀有海螺");
    private static final Text SHULKER_NAME_SSR = Text.of(TextColors.GOLD, "特级稀有海螺");

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
        this.directionMap.put(Direction.WEST, (byte) 5);
        this.directionMap.put(Direction.EAST, (byte) 4);
        this.directionMap.put(Direction.NORTH, (byte) 3);
        this.directionMap.put(Direction.SOUTH, (byte) 2);
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

    public void constructFor(Player player, Stack<Vector3i> stack)
    {
        Vector3i baseVector = player.getPosition().toInt();
        stack.addAll(this.positionForPurpurBlock);

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
    }

    @SuppressWarnings("deprecation")
    public void summonFor(Player player, Stack<Vector3i> stack)
    {
        if (!stack.empty())
        {
            Collections.shuffle(stack, this.random);

            World world = player.getWorld();
            Vector3i offsetInt = stack.pop();
            Vector3d offset = offsetInt.toDouble();
            Vector3d position = player.getPosition().add(offset);

            Vector3i positionInt = position.toInt();
            Direction direction = Direction.getClosest(offset, Direction.Division.CARDINAL);
            DataContainer data = world.createEntity(EntityTypes.SHULKER, position).createSnapshot().toContainer();

            data.set(DataQuery.of("UnsafeData", "NoAI"), (byte) 1);
            data.set(DataQuery.of("UnsafeData", "AttachFace"), this.directionMap.get(direction));

            int duringTicks = 0;

            for (int i = this.random.nextInt(64); i >= 0; i = this.random.nextInt(64))
            {
                i -= 36;
                if (i < 0)
                {
                    String customName = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_N);
                    data.set(DataQuery.of("UnsafeData", "Color"), (byte) 10); // purple (N)
                    data.set(DataQuery.of("UnsafeData", "CustomName"), customName);
                    duringTicks = 54;
                    break;
                }
                i -= 18;
                if (i < 0)
                {
                    String customName = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_R);
                    data.set(DataQuery.of("UnsafeData", "Color"), (byte) 6); // pink (R)
                    data.set(DataQuery.of("UnsafeData", "CustomName"), customName);
                    duringTicks = 90;
                    break;
                }
                i -= 6;
                if (i < 0)
                {
                    String customName = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_SR);
                    data.set(DataQuery.of("UnsafeData", "Color"), (byte) 1); // orange (SR)
                    data.set(DataQuery.of("UnsafeData", "CustomName"), customName);
                    duringTicks = 102;
                    break;
                }
                i -= 3;
                if (i < 0)
                {
                    String customName = TextSerializers.LEGACY_FORMATTING_CODE.serialize(SHULKER_NAME_SSR);
                    data.set(DataQuery.of("UnsafeData", "Color"), (byte) 4); // yellow (SSR)
                    data.set(DataQuery.of("UnsafeData", "CustomName"), customName);
                    duringTicks = 108;
                    break;
                }
            }

            Entity shulker = EntitySnapshot.builder().build(data).flatMap(EntitySnapshot::restore).get();

            Consumer<Task> executor = task ->
            {
                if (shulker.isRemoved())
                {
                    stack.push(offsetInt);
                    player.resetBlockChange(positionInt);
                }
                else
                {
                    shulker.remove();
                    stack.push(offsetInt);
                    player.sendBlockChange(positionInt, this.purpurBlock);
                }
            };

            player.resetBlockChange(positionInt);
            shulker.setCreator(player.getUniqueId());
            Task.builder().delayTicks(duringTicks).execute(executor).submit(this.plugin);
        }
    }
}
