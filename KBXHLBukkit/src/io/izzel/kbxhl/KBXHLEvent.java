package io.izzel.kbxhl;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public abstract class KBXHLEvent extends PlayerEvent {

    public KBXHLEvent(Player who) {
        super(who);
    }

    public static class Start extends KBXHLEvent {

        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Start(Player who) {
            super(who);
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        public static HandlerList getHandlerList() {
            return HANDLER_LIST;
        }

    }

    public static class Stop extends KBXHLEvent {

        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Stop(Player who) {
            super(who);
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        public static HandlerList getHandlerList() {
            return HANDLER_LIST;
        }

    }

}
