package com.github.ustc_zzzz.kbxhl;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.golem.Shulker;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeStructure
{
    static final Text SHULKER_NAME = Text.of(TextColors.GOLD, "海螺");

    private final Random random = new Random();

    private final KBXHLSponge plugin;
    private final List<Vector3i> positionForEndBricks;
    private final List<Vector3i> positionForPurpurBlock;
    private final List<Vector3i> positionForPurpleGlass;

    private BlockState endBricks = BlockSnapshot.NONE.getState();
    private BlockState purpurBlock = BlockSnapshot.NONE.getState();
    private BlockState purpleGlass = BlockSnapshot.NONE.getState();

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

    public void summonFor(Player player, int duringTicks, Stack<Vector3i> stack)
    {
        if (!stack.empty())
        {
            Collections.shuffle(stack, this.random);

            World world = player.getWorld();
            Vector3i offset = stack.pop();
            Vector3d position = player.getPosition().add(offset.toDouble());
            Shulker shulker = (Shulker) world.createEntity(EntityTypes.SHULKER, position);

            Vector3i positionInt = position.toInt();
            shulker.offer(Keys.AI_ENABLED, Boolean.FALSE);
            shulker.offer(Keys.DISPLAY_NAME, SHULKER_NAME);
            shulker.offer(Keys.DIRECTION, Direction.getClosest(position, Direction.Division.CARDINAL).getOpposite());

            Consumer<Task> executor = task ->
            {
                shulker.remove();
                stack.push(offset);
                player.sendBlockChange(positionInt, BlockTypes.PURPUR_BLOCK.getDefaultState());
            };

            world.spawnEntity(shulker);
            player.resetBlockChange(positionInt);
            Task.builder().delayTicks(duringTicks).execute(executor).submit(this.plugin);
        }
    }
}
