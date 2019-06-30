package io.izzel.kbxhl;

import lombok.extern.java.Log;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@Log
public class KBXHLCommand implements CommandExecutor {

    private KBXHLBukkit instance;

    void init(KBXHLBukkit instance) {
        this.instance = instance;
        log.info("注册指令");
        instance.getCommand("kbxhl").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        val player = ((Player) sender);
        val id = player.getUniqueId();
        if (args.length >= 1) {
            switch (args[0]) {
                case "start":
                    if (player.hasPermission("kbxhl.command.start")) {
                        if (instance.getGame().getPlayers().contains(id)) {
                            player.sendMessage("你已经开始游戏了");
                        } else {
                            Bukkit.getPluginManager().callEvent(new KBXHLEvent.Start(player));
                        }
                        return true;
                    }
                    break;
                case "stop":
                    if (player.hasPermission("kbxhl.command.stop")) {
                        if (instance.getGame().getPlayers().contains(id)) {
                            Bukkit.getPluginManager().callEvent(new KBXHLEvent.Stop(player));
                        } else {
                            player.sendMessage("你没有在一场游戏中");
                        }
                        return true;
                    }
                    break;
                case "top":
                    if (player.hasPermission("kbxhl.command.top")) {
                        instance.getConf().getRank().entrySet()
                                .stream()
                                .filter(it -> it.getKey().length() == 36)
                                .sorted(Comparator.comparing(Map.Entry::getValue))
                                .limit(10)
                                .map(entry -> {
                                    val name = instance.getServer().getOfflinePlayer(UUID.fromString(entry.getKey())).getName();
                                    val time = entry.getValue();
                                    val mili = time % 1000;
                                    val sec = time / 1000;
                                    return String.format("%s 用时 %d.%03d s", name, sec, mili);
                                })
                                .forEach(player::sendMessage);
                        return true;
                    }
                    break;
            }
        }
        player.sendMessage("KuangBianXiaoHaiLuo v" + instance.getDescription().getVersion());
        return true;
    }
}
