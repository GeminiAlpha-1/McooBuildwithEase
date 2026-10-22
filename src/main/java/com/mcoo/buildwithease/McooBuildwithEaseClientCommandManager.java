package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.command.Command;
import dev.huskuraft.universal.api.command.CommandManager;

public final class McooBuildwithEaseClientCommandManager extends CommandManager {

    private final McooBuildwithEaseClient entrance;

    public McooBuildwithEaseClientCommandManager(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
    }

    public McooBuildwithEaseClient getEntrance() {
        return entrance;
    }

    @Override
    public void dispatch(Command command) {
        command.execute(getEntrance().getClient()::sendCommand);
    }

//    @Override
//    public void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
//        ActualSettingsCommands.register(dispatcher);
//        ActualBuildCommands.register(dispatcher, commandBuildContext);
//    }


}
