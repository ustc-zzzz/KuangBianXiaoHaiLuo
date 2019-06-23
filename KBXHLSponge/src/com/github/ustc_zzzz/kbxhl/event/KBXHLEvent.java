package com.github.ustc_zzzz.kbxhl.event;

import org.spongepowered.api.event.Event;
import org.spongepowered.api.util.annotation.NonnullByDefault;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
public interface KBXHLEvent extends Event
{
    interface Start extends KBXHLEvent {}

    interface Stop extends KBXHLEvent {}
}
