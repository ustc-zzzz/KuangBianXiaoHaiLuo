package com.github.ustc_zzzz.kbxhl;

import com.github.ustc_zzzz.kbxhl.event.KBXHLEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeCommand implements Supplier<CommandCallable>
{
    private final KBXHLSponge plugin;
    private final Set<UUID> players = new LinkedHashSet<>();

    KBXHLSpongeCommand(KBXHLSponge plugin)
    {
        this.plugin = plugin;
    }

    void init()
    {
        Sponge.getCommandManager().register(this.plugin, this.get(), "kbxhl");
        Sponge.getEventManager().registerListener(this.plugin, GameStoppingServerEvent.class, this::on);
        Sponge.getEventManager().registerListener(this.plugin, ClientConnectionEvent.Disconnect.class, this::on);
    }

    private CommandResult fallback(CommandSource src, CommandContext args)
    {
        // TODO
        src.sendMessage(Text.of("KuangBianXiaoHaiLuo Help"));
        return CommandResult.success();
    }

    private CommandResult start(CommandSource src, CommandContext args)
    {
        if (src instanceof Player)
        {
            if (this.start((Player) src))
            {
                return CommandResult.success();
            }
        }
        return this.fallback(src, args);
    }

    private CommandResult stop(CommandSource src, CommandContext args)
    {
        if (src instanceof Player)
        {
            if (this.stop((Player) src))
            {
                return CommandResult.success();
            }
        }
        return this.fallback(src, args);
    }

    public boolean start(Player player)
    {
        if (this.players.add(player.getUniqueId()))
        {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame())
            {
                Cause currentCause = frame.pushCause(player).getCurrentCause();
                KBXHLEvent.Start e = () -> currentCause;
                Sponge.getEventManager().post(e);
                return true;
            }
        }
        return false;
    }

    public boolean stop(Player player)
    {
        if (this.players.remove(player.getUniqueId()))
        {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame())
            {
                Cause currentCause = frame.pushCause(player).getCurrentCause();
                KBXHLEvent.Stop e = () -> currentCause;
                Sponge.getEventManager().post(e);
                return true;
            }
        }
        return false;
    }

    public boolean has(Player player)
    {
        return this.players.contains(player.getUniqueId());
    }

    private CommandResult top(CommandSource src, CommandContext args)
    {
        src.sendMessage(this.plugin.configuration.toTopRankListText());
        return CommandResult.success();
    }

    private void on(GameStoppingServerEvent event)
    {
        Sponge.getServer().getOnlinePlayers().forEach(KBXHLSpongeCommand.this::stop);
    }

    private void on(ClientConnectionEvent.Disconnect event)
    {
        KBXHLSpongeCommand.this.stop(event.getTargetEntity());
    }

    @Override
    public CommandCallable get()
    {
        this.plugin.logger.info("Register command for KuangBianXiaoHaiLuo");
        return CommandSpec.builder()
                .child(CommandSpec.builder().permission("kbxhl.command.start").executor(this::start).build(), "start")
                .child(CommandSpec.builder().permission("kbxhl.command.stop").executor(this::stop).build(), "stop")
                .child(CommandSpec.builder().permission("kbxhl.command.top").executor(this::top).build(), "top")
                .childArgumentParseExceptionFallback(true).executor(this::fallback).build();
    }
}
