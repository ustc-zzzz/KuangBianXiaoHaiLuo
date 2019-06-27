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
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLCommand implements Supplier<CommandCallable>
{
    private final KBXHLSponge plugin;
    private final Map<UUID, Tristate> players = new LinkedHashMap<>();

    KBXHLCommand(KBXHLSponge plugin)
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
        UUID uuid = player.getUniqueId();
        if (!this.players.containsKey(uuid))
        {
            this.players.put(uuid, Tristate.UNDEFINED);
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame())
            {
                Cause currentCause = frame.pushCause(player).getCurrentCause();
                KBXHLEvent.Start e = () -> currentCause;
                Sponge.getEventManager().post(e);
                this.players.put(uuid, Tristate.TRUE);
                return true;
            }
        }
        return false;
    }

    public boolean stop(Player player)
    {
        UUID uuid = player.getUniqueId();
        if (this.players.getOrDefault(uuid, Tristate.FALSE).asBoolean())
        {
            this.players.put(uuid, Tristate.UNDEFINED);
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame())
            {
                Cause currentCause = frame.pushCause(player).getCurrentCause();
                KBXHLEvent.Stop e = () -> currentCause;
                Sponge.getEventManager().post(e);
                this.players.remove(uuid);
                return true;
            }
        }
        return false;
    }

    public Tristate has(Player player)
    {
        return this.players.getOrDefault(player.getUniqueId(), Tristate.FALSE);
    }

    private CommandResult top(CommandSource src, CommandContext args)
    {
        src.sendMessage(this.plugin.config.toTopRankListText());
        return CommandResult.success();
    }

    private void on(GameStoppingServerEvent event)
    {
        Sponge.getServer().getOnlinePlayers().forEach(KBXHLCommand.this::stop);
    }

    private void on(ClientConnectionEvent.Disconnect event)
    {
        KBXHLCommand.this.stop(event.getTargetEntity());
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
