package com.github.ustc_zzzz.kbxhl;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
@Plugin(id = "kbxhl", name = "KuangBianXiaoHaiLuo", version = "@version@", description = "KuangBianXiaoHaiLuo")
public class KBXHLSponge
{
    private static final Pattern CHAT_PATTERN_1 = Pattern.compile("\u6d77\u87ba[\uff01\u0021]\u005c\u005c\u0073\u002a");
    private static final Pattern CHAT_PATTERN_2 = Pattern.compile("\u571f\u7403[\uff01\u0021]\u005c\u005c\u0073\u002a");

    final Logger logger;
    final Path configPath;

    final KBXHLConfig config;
    final KBXHLCommand command;
    final KBXHLGameStructure structure;
    final KBXHLScoreManager scoreManager;

    @Inject
    public KBXHLSponge(Logger logger, @DefaultConfig(sharedRoot = true) Path configPath)
    {
        this.logger = logger;
        this.configPath = configPath;

        this.config = new KBXHLConfig(this);
        this.command = new KBXHLCommand(this);
        this.structure = new KBXHLGameStructure(this);
        this.scoreManager = new KBXHLScoreManager(this);
    }

    @Listener
    public void on(GameStartingServerEvent event)
    {
        this.config.init();
        this.command.init();
        this.structure.init();
        this.scoreManager.init();
    }

    @Listener(order = Order.LATE, beforeModifications = true)
    public void on(MessageChannelEvent.Chat event, @Root Player player)
    {
        String oldMessage = event.getRawMessage().toPlain();
        if (CHAT_PATTERN_1.matcher(oldMessage).matches())
        {
            event.setMessage(Text.of("<<<<<<< " + oldMessage));
            String newMessage = ">>>>>>> " + oldMessage.replace("\u6d77\u87ba", "\u571f\u7403");
            Task.builder().execute(task -> player.sendMessage(ChatTypes.CHAT, Text.of(newMessage))).submit(this);
        }
        if (CHAT_PATTERN_2.matcher(oldMessage).matches())
        {
            event.setMessage(Text.of("<<<<<<< " + oldMessage));
            String newMessage = ">>>>>>> " + oldMessage.replace("\u571f\u7403", "\u6d77\u87ba");
            Task.builder().execute(task -> player.sendMessage(ChatTypes.CHAT, Text.of(newMessage))).submit(this);
        }
    }
}
