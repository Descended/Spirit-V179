package net.swordie.ms.client.character.commands;

import net.swordie.ms.ServerConfig;

/**
 * Created on 12/22/2017.
 */
public abstract class AdminCommand implements ICommand {

    public AdminCommand() {
    }

    public static char getPrefix() {
        return ServerConfig.ADMIN_COMMAND;
    }
}
