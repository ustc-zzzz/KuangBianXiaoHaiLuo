package com.github.ustc_zzzz.kbxhl;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.function.Supplier;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public class KBXHLSpongeCommand implements Supplier<CommandCallable>
{
    private final KBXHLSponge plugin;

    KBXHLSpongeCommand(KBXHLSponge plugin)
    {
        this.plugin = plugin;
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
            // TODO
            return CommandResult.success();
        }
        return this.fallback(src, args);
    }

    private CommandResult stop(CommandSource src, CommandContext args)
    {
        if (src instanceof Player)
        {
            // TODO
            return CommandResult.success();
        }
        return this.fallback(src, args);
    }

    private CommandResult top(CommandSource src, CommandContext args)
    {
        // TODO
        return CommandResult.success();
    }

    @Override
    public CommandCallable get()
    {
        return CommandSpec.builder()
                .child(CommandSpec.builder().permission("kbxhl.command.start").executor(this::start).build(), "start")
                .child(CommandSpec.builder().permission("kbxhl.command.stop").executor(this::stop).build(), "stop")
                .child(CommandSpec.builder().permission("kbxhl.command.top").executor(this::top).build(), "top")
                .childArgumentParseExceptionFallback(true).executor(this::fallback).build();
    }
}
