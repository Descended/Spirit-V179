package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.handlers.EventManager;
import net.swordie.ms.life.Life;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class InGameEventManager {

    private static InGameEventManager instance = new InGameEventManager();
    public static final int REGISTRATION_DURATION_MINS = ServerConfig.DEBUG_MODE ? 1 : 5; // devs want fast

    private HashMap<InGameEventType, InGameEvent> events = new HashMap<>();
    private ScheduledFuture schedule;
    private ScheduledFuture reminderTimer;
    private InGameEventType previousEvent;
    private int remindersSent = 0;

    public static InGameEventManager getInstance() {
        return instance;
    }

    public InGameEventManager() {
        events.put(InGameEventType.RussianRoulette, new RussianRouletteEvent());
        events.put(InGameEventType.PinkZakumBattle, new PinkZakumEvent());
        // more to come...

        if (!ServerConfig.DEBUG_MODE)
            schedule = EventManager.addFixedRateEvent(this::doEvent, 5, 40, TimeUnit.MINUTES);
    }

    public void forceNextEvent() { // for testing only, this should not be used in prod
        if (ServerConfig.DEBUG_MODE) {
            InGameEvent curEvent = getActiveEvent();
            if (curEvent != null) {
                curEvent.endEvent();
            }
            doEvent();
        } // else notify character that server is in prod
    }

    private void doEvent() {
        InGameEvent event = events.entrySet().stream()
                .filter(e -> e.getValue().getEventType() != previousEvent)
                .findFirst().get().getValue();

        previousEvent = event.getEventType();

        event.clear(); // reset map info for next run
        if (!ServerConfig.DEBUG_MODE) {
            reminderTimer = EventManager.addFixedRateEvent(this::sendReminder, 30, 60, TimeUnit.SECONDS);
        }

        Server.getInstance().getWorldById(ServerConfig.WORLD_ID)
                .broadcastPacket(WvsContext.broadcastMsg(BroadcastMsg.notice(event.getEventName() + " event registration has started! Registration will close in " + REGISTRATION_DURATION_MINS + " minutes.")));
        event.doEvent();
    }

    private void sendReminder() {
        Server.getInstance().getWorldById(ServerConfig.WORLD_ID)
                .broadcastPacket(WvsContext.broadcastMsg(BroadcastMsg.notice(getActiveEvent().getEventName() + " event registration is currently open and will begin soon!")));
        remindersSent += 1;
        if (remindersSent >= 4) // = 5 minutes
            reminderTimer.cancel(true);
    }

    public InGameEvent getOpenEvent() {
        InGameEvent e = null;
        for (InGameEvent ige : events.values())
            if (ige.isOpen()) {
                e = ige;
            }
        return e;
    }

    public InGameEvent getActiveEvent() {
        for (InGameEvent ige : events.values())
            if (ige.isActive()) {
                return ige;
            }
        return null;
    }

    public void joinPublicEvent(Char c) {
        InGameEvent e = getActiveEvent();

        if (e == null) {
            c.chatMessage(ChatType.SystemNotice, "There are no ongoing events. Please check back later!");
        } else if (!e.isOpen()) {
            c.chatMessage(ChatType.SystemNotice, "The event has closed for new entries.");
        } else {
            e.joinEvent(c);
        }
    }

    public InGameEvent getEvent(InGameEventType type) {
        for (InGameEvent ige : events.values()) {
            if (ige.getEventType() == type) {
                return ige;
            }
        }
        return null; // shouldnt reach this point
    }

    public boolean charInEventMap(int charId) {
        InGameEvent e = getActiveEvent();

        if (e == null) {
            return false;
        }

        return e.charInEvent(charId);
    }
}
