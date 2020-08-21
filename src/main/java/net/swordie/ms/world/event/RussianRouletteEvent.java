package net.swordie.ms.world.event;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.WeatherEffNoticeType;
import net.swordie.ms.handlers.EventManager;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.field.ClockPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RussianRouletteEvent implements InGameEvent {

    public static final int EVENT_MAP = 910030000; // same map extalia used to use
    private final int EVENT_EXIT_MAP = 100000000; // TODO figure out a good exit map
    private final int TOTAL_ROUNDS = 10; // edit this later, make it scalable by # of players registered for event
    private final int ROUND_LENGTH_SECONDS = 10;

    private final int[][] SECTIONS = { // these X-values are specific to fieldID 910030000
            {-800, -254}, // left
            {-327, 248},  // middle
            {171, 800}    // right
    };

    private boolean started = false;
    private boolean active = false;
    private long startTimeMillis;
    private ScheduledFuture startTimer;
    private ScheduledFuture killTimer;
    private int currentRound = 1;
    private Channel channelInstance;

    private final int eventNpc = 9000155; // Dizzy | Roulette Event Manager
    private final int[] npcPos = { 208, -206 }; // spawn points for above npc

    public RussianRouletteEvent() {
        channelInstance = Server.getInstance().getWorlds().get(0).getChannels().get(0);
    }

    @Override
    public String getEventName() {
        return "Russian Roulette";
    }

    @Override
    public void doEvent() {
        active = true;
        channelInstance = Server.getInstance().getWorlds().get(0).getChannels().get(0);
        startTimer = EventManager.addEvent(this::start, InGameEventManager.REGISTRATION_DURATION_MINS, TimeUnit.MINUTES);
        startTimeMillis = System.currentTimeMillis() + InGameEventManager.REGISTRATION_DURATION_MINS * 60 * 1000;
        channelInstance.getField(EVENT_MAP).setDropsDisabled(true); // lag reduction, look at old extalia events and youll see trolls dropping items to cause major lag
        channelInstance.getField(EVENT_MAP).getNpcs().clear(); // remove the standard npcs
        channelInstance.getField(EVENT_MAP).spawnNpc(eventNpc, npcPos[0], npcPos[1]); // add our custom npc to the map
    }

    private void start() {
        if (startTimer != null && !startTimer.isDone()) {
            startTimer.cancel(true);
        }

        started = true;
        Server.getInstance().getWorldById(ServerConfig.WORLD_ID)
                .broadcastPacket(WvsContext.broadcastMsg(BroadcastMsg.notice("Event registration has ended!")));

        if (channelInstance.getField(EVENT_MAP).getChars().size() > 0) {
            sendNotice("Get in position!", ROUND_LENGTH_SECONDS);
            broadcastClock(ROUND_LENGTH_SECONDS);
            killTimer = EventManager.addEvent(this::broadcastCountdownEffect, ROUND_LENGTH_SECONDS - 3, TimeUnit.SECONDS);
        } else {
            endEvent();
        }
    }

    private void broadcastCountdownEffect() {
        channelInstance.getField(EVENT_MAP).broadcastPacket(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/defense/count", false, 0, 4, 0)));
        killTimer = EventManager.addEvent(this::executeRound, 3, TimeUnit.SECONDS);
    }

    private void executeRound() {
        killCharsInRect();

        if (currentRound >= TOTAL_ROUNDS) {
            endEvent();
        } else {
            channelInstance.getField(EVENT_MAP).broadcastPacket(FieldPacket.clock(ClockPacket.removeClock()));
            startTimer = EventManager.addEvent(this::resetRound, 3, TimeUnit.SECONDS);
        }
        currentRound += 1;
    }

    private void resetRound() {
        broadcastClock(ROUND_LENGTH_SECONDS);
        killTimer = EventManager.addEvent(this::executeRound, ROUND_LENGTH_SECONDS, TimeUnit.SECONDS);
    }

    private void killCharsInRect() {
        if (channelInstance.getField(EVENT_MAP).getChars().size() <= 0) {
            endEvent();
        } else {
            int[] randDomain = SECTIONS[Util.getRandom(2)]; // holds two X values

            // y-axis is inverted for some reason
            final int UPPER_BOUNDARY = -77;
            final int LOWER_BOUNDARY = 100;

            for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
                c.chatMessage(ChatType.Mob, "X: " + c.getPosition().getX() + " Y: " + c.getPosition().getY());
                // range
                if (c.getPosition().getY() <= UPPER_BOUNDARY || c.getPosition().getY() >= LOWER_BOUNDARY) {
                    c.damage(c.getMaxHP());
                }
                // domain
                if (c.getPosition().getX() >= randDomain[0] && c.getPosition().getX() <= randDomain[1]) {
                    c.damage(c.getMaxHP());
                }
            }

            List<Char> deadChars = new ArrayList<>();
            for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
                if (c.getHP() <= 0) {
                    deadChars.add(c);
                    deathEffect(c);
                }
            }
            // killTimer = EventManager.addEvent(() -> warpOut(deadChars), 5, TimeUnit.SECONDS); // what do we want to do with the dead?
            updatePlayerCount(deadChars.size());
        }
    }

    @Override
    public void endEvent() {
        active = false;
        started = false;
        startTimer = null;
        killTimer = null;
        for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
            warpOut(c);
        }
    }

    @Override
    public void clear() {
        // unused rn
    }

    private void warpOut(List<Char> charList) {
        for (Char c : charList) {
            warpOut(c);
        }
    }

    private void warpOut(Char c) {
        try {
            c.warp(EVENT_EXIT_MAP, 0, false);
        } catch (Exception ex) {
            // if the player has disconnected before they get warped
        }
    }

    public void joinEvent(Char c) {
        c.changeChannelAndWarp((byte) channelInstance.getChannelId(), EVENT_MAP);
    }

    private void updatePlayerCount(int dead) {
        sendNotice("Players Left: " + (channelInstance.getField(EVENT_MAP).getChars().size() - dead), ROUND_LENGTH_SECONDS);
    }

    private void deathEffect(Char c) {
        c.write(UserPacket.effect(Effect.effectFromWZ("Map/Effect.img/Yut/Lose", false, 0, 4, 0)));
    }

    private void sendNotice(String msg, int seconds) {
        channelInstance.getField(EVENT_MAP)
                .broadcastPacket(WvsContext.weatherEffectNotice(WeatherEffNoticeType.Fireworks, msg,
                        seconds * 1000));
    }

    private void broadcastClock(int seconds) {
        channelInstance.getField(EVENT_MAP).broadcastPacket(FieldPacket.clock(ClockPacket.secondsClock(seconds)));
    }

    @Override
    public void sendLobbyClock(Char c) {
        long timeLeft = (startTimeMillis - System.currentTimeMillis()) / 1000;
        if (timeLeft >= 2) {
            c.write(FieldPacket.clock(ClockPacket.secondsClock((int) timeLeft)));
            sendNotice("Get ready for some Russian Roulette, MapleStory style!", (int) timeLeft);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isOpen() {
        return !started;
    }

    @Override
    public InGameEventType getEventType() {
        return InGameEventType.RussianRoulette;
    }

    @Override
    public int getEventEntryMap() {
        return EVENT_MAP;
    }

    @Override
    public boolean charInEvent(int charId) {
        if (!active) {
            return false;
        }

        for (Char c : channelInstance.getField(EVENT_MAP).getChars()) {
            if (c.getId() == charId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMigrateDeath(Char c) {
        c.warp(EVENT_EXIT_MAP, 0, false);
    }
}
