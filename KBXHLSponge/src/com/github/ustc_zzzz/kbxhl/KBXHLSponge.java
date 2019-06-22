package com.github.ustc_zzzz.kbxhl;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.regex.Pattern;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
@Plugin(id = "kbxhl", name = "KuangBianXiaoHaiLuo", version = "1.0.0-SNAPSHOT", description = "KuangBianXiaoHaiLuo")
public class KBXHLSponge
{
    private final Pattern CHAT_PATTERN_1 = Pattern.compile("\u6d77\u87ba[\uff01\u0021]\u005c\u005c\u0073\u002a");
    private final Pattern CHAT_PATTERN_2 = Pattern.compile("\u571f\u7403[\uff01\u0021]\u005c\u005c\u0073\u002a");

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
