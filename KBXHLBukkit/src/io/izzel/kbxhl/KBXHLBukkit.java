package io.izzel.kbxhl;

import lombok.Getter;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public final class KBXHLBukkit extends JavaPlugin implements Listener {

    private static final Pattern CHAT_PATTERN_1 = Pattern.compile("\u6d77\u87ba[\uff01\u0021]\u005c\u005c\u0073\u002a");
    private static final Pattern CHAT_PATTERN_2 = Pattern.compile("\u571f\u7403[\uff01\u0021]\u005c\u005c\u0073\u002a");
    private static KBXHLBukkit INSTANCE;

    public static KBXHLBukkit instance() {
        return INSTANCE;
    }

    @Getter private KBXHLConfig conf = new KBXHLConfig();
    @Getter private KBXHLCommand command = new KBXHLCommand();
    @Getter private KBXHLScoreManager scoreManager = new KBXHLScoreManager();
    @Getter private KBXHLGame game = new KBXHLGame();

    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        conf.init(this);
        command.init(this);
        scoreManager.init(this);
        game.init(this);
    }

    @Override
    public void onDisable() {
        conf.save();
    }

    @EventHandler
    public void on(AsyncPlayerChatEvent event) {
        val old = event.getMessage();
        if (CHAT_PATTERN_1.matcher(old).matches()) {
            event.setMessage("<<<<<<< " + old);
            val rep = ">>>>>>> " + old.replace("\u6d77\u87ba", "\u571f\u7403");
            getServer().getScheduler().runTask(this, () -> event.getPlayer().sendMessage(rep));
        }
        if (CHAT_PATTERN_2.matcher(old).matches()) {
            event.setMessage("<<<<<<< " + old);
            val rep = ">>>>>>> " + old.replace("\u571f\u7403", "\u6d77\u87ba");
            getServer().getScheduler().runTask(this, () -> event.getPlayer().sendMessage(rep));
        }
    }

}
