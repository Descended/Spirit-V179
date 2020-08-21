package net.swordie.ms.handlers.social;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.commands.*;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.ChatSocket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.ChatUserType;
import net.swordie.ms.enums.GroupMessageType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.World;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.ChatType.*;

public class ChatHandler {

    private static final Logger log = Logger.getLogger(ChatHandler.class);

    @Handler(op = InHeader.USER_CHAT)
    public static void handleUserChat(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        inPacket.decodeInt(); // timestamp
        String msg = inPacket.decodeString();

        if (msg.length() <= 0) {
            return;
        }

        String command = msg.split(" ")[0].replace(String.valueOf(msg.charAt(0)), "");
        Class[] commandClasses = null;

        if (msg.startsWith(String.valueOf(ServerConfig.PLAYER_COMMAND))) {
            commandClasses = PlayerCommands.class.getClasses();
        } else if (msg.startsWith(String.valueOf(ServerConfig.ADMIN_COMMAND))) {
            commandClasses = AdminCommands.class.getClasses();
        }

        // doesn't start with command prefix
        if (commandClasses == null) {
            chr.getField().broadcastPacket(UserPacket.chat(chr.getId(), ChatUserType.User, msg,
                    false, 0, c.getWorldId()));
            return;
        }

        for (Class commandClass : commandClasses) {
            Command cmd = (Command) commandClass.getAnnotation(Command.class);
            for (String name : cmd.names()) {

                if (!name.equalsIgnoreCase(command)) {
                    continue;
                }

                if (chr.getUser().getAccountType().ordinal() < cmd.requiredType().ordinal()) {
                    continue;
                }

                try {
                    ICommand iCommand = null;

                    // TODO replace this switch statement with something prettier
                    switch (msg.charAt(0)) {
                        case ServerConfig.PLAYER_COMMAND:
                            iCommand = (PlayerCommand) commandClass.getConstructor().newInstance();
                            break;
                        case ServerConfig.ADMIN_COMMAND:
                            iCommand = (AdminCommand) commandClass.getConstructor().newInstance();
                            break;
                    }

                    commandClass.getDeclaredMethod("execute", Char.class, String[].class)
                            .invoke(iCommand, chr, msg.split(" "));

                } catch (Exception e) {
                    chr.chatMessage("Exception: " + e.getCause().toString());
                    e.printStackTrace();
                }
                return;
            }
        }

        // only reaches this point if no matching command was found
        chr.chatMessage(Expedition, "Unknown command \"" + command + "\"");
    }

    @Handler(op = InHeader.WHISPER)
    public static void handleWhisper(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        byte type = inPacket.decodeByte();
        inPacket.decodeInt(); // tick
        String destName = inPacket.decodeString();
        Char dest = c.getWorld().getCharByName(destName);
        if (dest == null) {
            chr.chatMessage("Character not found.");
            return;
        }
        switch (type) {
            case 5: // /find command
                int fieldId = dest.getField().getId();
                int channel = dest.getClient().getChannel();
                if (channel != chr.getClient().getChannel()) {
                    chr.chatMessage("%s is in channel %s-%d.", dest.getName(), dest.getWorld().getName(), channel);
                } else {
                    String fieldString = StringData.getMapStringById(fieldId);
                    if (fieldString == null) {
                        fieldString = "Unknown field.";
                    }
                    chr.chatMessage("%s is at %s.", dest.getName(), fieldString);
                }
                break;
            case 68:
                break;
            case 6: // whisper
                String msg = inPacket.decodeString();
                dest.write(FieldPacket.whisper(chr.getName(), (byte) (c.getChannel() - 1), false, msg, false));
                chr.chatMessage(Whisper, String.format("%s<< %s", dest.getName(), msg));
                break;
        }

    }

    @Handler(op = InHeader.GROUP_MESSAGE)
    public static void handleGroupMessage(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte(); // party = 1, alliance = 3
        byte idk2 = inPacket.decodeByte();
        int idk3 = inPacket.decodeInt(); // party id?
        String msg = inPacket.decodeString();
        if (msg.length() > 1000 || !Util.isValidString(msg)) {
            return;
        }
        switch (type) {
            case 0: // buddy
                // TODO
                break;
            case 1: // party
                if (chr.getParty() != null) {
                    chr.getParty().broadcast(FieldPacket.groupMessage(GroupMessageType.Party, chr.getName(), msg), chr);
                }
                break;
            case 2: // guild
                if (chr.getGuild() != null) {
                    chr.getGuild().broadcast(FieldPacket.groupMessage(GroupMessageType.Guild, chr.getName(), msg), chr);
                }
                break;
            case 3: // alliance
                if (chr.getGuild() != null && chr.getGuild().getAlliance() != null) {
                    chr.getGuild().getAlliance().broadcast(FieldPacket.groupMessage(GroupMessageType.Alliance, chr.getName(), msg), chr);
                }
                break;
            default:
                log.error("Unhandled group message type " + type);
        }
    }

    @Handler(op = InHeader.CONNECT_CHAT)
    public static void handleConnect(Client c, InPacket inPacket) {
        int accID = inPacket.decodeInt();
        int idk = inPacket.decodeInt(); // always 1?
        long idk2 = inPacket.decodeLong();
        boolean idk3 = inPacket.decodeByte() != 0;
        int charID = inPacket.decodeInt();
        String charName = inPacket.decodeString();
        int level = inPacket.decodeInt();
        int job = inPacket.decodeInt();
        Char chr = null;
        for (World w : Server.getInstance().getWorlds()) {
            chr = w.getCharByID(charID);
            if (chr != null) {
                break;
            }
        }
        if (chr != null) {
            chr.setChatClient(c);
            c.setChr(chr);
            chr.getWorld().getConnectedChatClients().put(accID, c);
        }
        c.write(ChatSocket.loginResult(chr != null));
    }

    @Handler(op = InHeader.FRIEND_CHAT)
    public static void handleFriendChat(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        int accID = inPacket.decodeInt();
        String msg = inPacket.decodeString();
        int size = inPacket.decodeInt();
        for (int i = 0; i < size; i++) {
            if (chr.getWorld().getConnectedChatClients().containsKey(i)) {
                chr.getWorld().getConnectedChatClients().get(i).write(ChatSocket.friendChatMessage(accID, chr.getId(), null, msg, false));
            }
        }
    }

    @Handler(op = InHeader.GUILD_CHAT)
    public static void handleGuildChat(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        int charID = inPacket.decodeInt();
        int guildID = inPacket.decodeInt();
        String msg = inPacket.decodeString();
        if (chr.getGuild() != null) {
            chr.getGuild().broadcast(FieldPacket.groupMessage(GroupMessageType.Guild, chr.getName(), msg));
        }
    }

}
