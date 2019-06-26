package io.izzel.kbxhl;

import lombok.extern.java.Log;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        if (args.length > 1) {
            switch (args[0]) {
                case "start":
                    if (player.hasPermission("kbxhl.command.start")) {
                        if (instance.getConfig().getPlayers().contains(id)) {
                            player.sendMessage("你已经开始游戏了");
                        } else {
                            Bukkit.getPluginManager().callEvent(new KBXHLEvent.Start(player));
                        }
                    }
                    break;
                case "stop":
                    if (player.hasPermission("kbxhl.command.stop")) {
                        if (instance.getConfig().getPlayers().contains(id)) {
                            Bukkit.getPluginManager().callEvent(new KBXHLEvent.Stop(player));
                        } else {
                            player.sendMessage("你没有在一场游戏中");
                        }
                    }
                    break;
                case "top":
                    break;
            }
        }
        return true;
    }
}
