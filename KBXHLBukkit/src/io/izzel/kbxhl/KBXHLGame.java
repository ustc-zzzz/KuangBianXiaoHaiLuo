package io.izzel.kbxhl;

import org.bukkit.event.EventHandler;

public class KBXHLGame {

    private KBXHLBukkit instance;

    void init(KBXHLBukkit instance) {
        this.instance = instance;
        instance.getServer().getPluginManager().registerEvents(new Listener(), instance);
    }

    private class Listener implements org.bukkit.event.Listener {

        @EventHandler
        public void on(KBXHLEvent.Start event) {

        }

        @EventHandler
        public void on(KBXHLEvent.Stop event) {

        }

    }

}
