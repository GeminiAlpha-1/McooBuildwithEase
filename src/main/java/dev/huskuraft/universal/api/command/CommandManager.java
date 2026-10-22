package dev.huskuraft.universal.api.command;

public abstract class CommandManager implements CommandRegister {

    public abstract void dispatch(Command command);

}
