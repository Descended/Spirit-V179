package net.swordie.ms.world;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.life.Life;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.AreaBossInfo;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created on 11/2/2017.
 */
public class Channel {
    //CHANNELITEM struct
    private int port;
    private String name;
    private int worldId, channelId;
    private boolean adultChannel;
    private List<Field> fields;
    private Map<Integer, Tuple<Byte, Client>> transfers;
    private Map<Integer, Char> chars = new HashMap<>();
    public final int MAX_SIZE = 1000;
    private Map<Integer, Map<Integer, Long>> areaBossSpawns = new HashMap<>();

    private Channel(String name, World world, int channelId, boolean adultChannel) {
        this.name = name;
        this.worldId = world.getWorldId();
        this.channelId = channelId;
        this.adultChannel = adultChannel;
        this.port = ServerConstants.LOGIN_PORT + 100 + channelId;
        this.fields = new CopyOnWriteArrayList<>();
        this.transfers = new HashMap<>();
    }

    public Channel(World world, int channelId) {
        this(world.getName() + "-" + channelId, world, channelId, false);
    }

    public Channel(String worldName, int worldId, int channelId) {
        this.name = worldName + "-" + channelId;
        this.worldId = worldId;
        this.channelId = channelId;
        this.adultChannel = false;
        this.port = ServerConstants.LOGIN_PORT + (100 * worldId) + channelId;
        this.fields = new CopyOnWriteArrayList<>();
        this.transfers = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getGaugePx() {
        return Math.max(1, (chars.size() * 64) / MAX_SIZE);
    }

    public int getWorldId() {
        return worldId;
    }

    public int getChannelId() {
        return channelId;
    }

    public boolean isAdultChannel() {
        return adultChannel;
    }

    public void setAdultChannel(boolean adultChannel) {
        this.adultChannel = adultChannel;
    }

    public int getPort() {
        return port;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Gets a {@link Field} corresponding to a given ID. If it doesn't exist, creates one.
     *
     * @param id The map ID of the field.
     * @return The (possibly newly created) Field.
     */
    public Field getField(int id) {
        for (Field field : getFields()) {
            if (field.getId() == id) {
                return field;
            }
        }
        return createAndReturnNewField(id);
    }

    public Field getFieldIfExists(int fieldId) {
        for (Field field : getFields()) {
            if (field.getId() == fieldId) {
                return field;
            }
        }
        return null;
    }

    private Field createAndReturnNewField(int id) {
        Field newField = FieldData.getFieldCopyById(id);
        if (newField != null) {
            newField.setChannelField(true);
            newField.setChannel(getChannelId());
            getFields().add(newField);
        }
        return newField;
    }

    public Map<Integer, Tuple<Byte, Client>> getTransfers() {
        if (transfers == null) {
            transfers = new HashMap<>();
        }
        return transfers;
    }

    public void addClientInTransfer(byte channelId, int characterId, Client client) {
        getTransfers().put(characterId, new Tuple<>(channelId, client));
    }

    public void removeClientFromTransfer(int characterId) {
        getTransfers().remove(characterId);
    }

    public Map<Integer, Char> getChars() {
        return chars;
    }

    public void addChar(Char chr) {
        getChars().put(chr.getId(), chr);
    }

    public void removeChar(Char chr) {
        getChars().remove(chr.getId());
    }

    public Char getCharById(int id) {
        return getChars().get(id);
    }

    public Char getCharByName(String name) {
        return Util.findWithPred(getChars().values(), chr -> chr.getName().equals(name));
    }

    public Account getAccountByID(int accID) {
        for (Char chr : getChars().values()) {
            if (chr.getAccId() == accID) {
                return chr.getAccount();
            }
        }
        return null;
    }

    public void broadcastPacket(OutPacket outPacket) {
        for (Char chr : getChars().values()) {
            chr.write(outPacket);
        }
    }

    public void clearCache() {
        Set<Field> toRemove = new HashSet<>();
        for (Field field : getFields()) {
            if (field.getChars().size() == 0 && field.getDrops().size() == 0) {
                toRemove.add(field);
            }
        }
        getFields().removeAll(toRemove);
    }

    public void trySpawnAreaBoss(Char c, int targetFieldId, int curChannelId) {
        AreaBossInfo bossInfo = AreaBossInfo.getByFieldId(targetFieldId);

        if (bossInfo == null) {
            return;
        }

        boolean canSpawn = canWarpAreaBoss(c, targetFieldId, curChannelId);

        if (canSpawn) {
            c.chatMessage("Dark forces bring something out of the shadows.");
            areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
            areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + bossInfo.getRespawnTimeMin() * 60 * 1000);

            // -1 means its handled elsewhere (scripted) so we don't need to spawn it
            if (bossInfo.getBossID() <= 0) {
                return;
            }

            getField(targetFieldId)
                    .spawnMob(bossInfo.getBossID(), bossInfo.getSpawnPoint(), false, bossInfo.getHealth());
        } else {
            c.chatMessage("The land lacks power... Someone has been here very recently.");
        }
    }

    public void overrideAreaBossTimer(int targetFieldId, int curChannelId) {
        AreaBossInfo bossInfo = AreaBossInfo.getByFieldId(targetFieldId);

        if (bossInfo == null) {
            return;
        }

        areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
        areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + bossInfo.getRespawnTimeMin() * 60 * 1000);
    }

    public boolean canWarpAreaBoss(Char c, int targetFieldId, int curChannelId) {
        try {
            long lastSpawnTime = areaBossSpawns.get(curChannelId).get(targetFieldId);
            if (lastSpawnTime - System.currentTimeMillis() > 0) {
                return false;
            }
        } catch (NullPointerException ex) {
            // no entry found, let it spawn
        }
        return true;
    }

    public boolean tryEnterSilentCrusadePortal(Char c, int targetFieldId, int curChannelId) {
        if (c.getOrCreateFieldByCurrentInstanceType(targetFieldId).getChars().size() > 0) { // there is already someone inside
            c.chatMessage("You may have better luck on another channel..");
            return false;
        }

        try {
            long lastSpawnTime = areaBossSpawns.get(curChannelId).get(targetFieldId);
            if (lastSpawnTime - System.currentTimeMillis() > 0) {
                return false;
            }
        } catch (NullPointerException ex) {
            // no entry, they can go
        }

        // add timer
        areaBossSpawns.putIfAbsent(curChannelId, new HashMap<>());
        areaBossSpawns.get(curChannelId).putIfAbsent(targetFieldId, System.currentTimeMillis() + GameConstants.SILENT_CRUSADE_BOSS_COOLDOWN * 60 * 1000);

        return true;
    }
}
