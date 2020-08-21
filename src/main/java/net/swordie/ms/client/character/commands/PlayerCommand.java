package net.swordie.ms.client.character.commands;

import net.swordie.ms.ServerConfig;

public abstract class PlayerCommand implements ICommand {

    public PlayerCommand(){
    }

    public static char getPrefix() {
        return ServerConfig.PLAYER_COMMAND;
    }
}
