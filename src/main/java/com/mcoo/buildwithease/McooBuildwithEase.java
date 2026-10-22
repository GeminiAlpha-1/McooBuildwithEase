package com.mcoo.buildwithease;

import dev.huskuraft.universal.api.platform.PlatformLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.events.impl.EventRegistry;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;

@AutoService(Entrance.class)
public class McooBuildwithEase implements Entrance {

    public static final String MOD_ID = "mcoo_build_with_ease";
    public static final String DEFAULT_CHANNEL = "default";
    public static final int PROTOCOL_VERSION = 13;
    public static final Logger LOGGER = LoggerFactory.getLogger(McooBuildwithEase.class.getName());

    private final EventRegistry eventRegistry = PlatformLoader.getSingleton(EventRegistry.class);
    private final McooBuildwithEaseNetworkChannel networkChannel = new McooBuildwithEaseNetworkChannel(this);
    private final McooBuildwithEaseStructureBuilder structureBuilder = new McooBuildwithEaseStructureBuilder(this);
    private final McooBuildwithEaseConfigStorage sessionConfigStorage = new McooBuildwithEaseConfigStorage(this);
    private final McooBuildwithEaseSessionManager sessionManager = new McooBuildwithEaseSessionManager(this);
    private final McooBuildwithEaseServerManager serverManager = new McooBuildwithEaseServerManager(this);

    public static McooBuildwithEase getInstance() {
        return (McooBuildwithEase) Entrance.getInstance();
    }

    public static Text getSystemMessage(Text msg) {
        return Text.text("[").append(Text.translate("mcoo_build_with_ease.symbol")).append("] ").withStyle(ChatFormatting.GRAY).append(msg.withStyle(ChatFormatting.WHITE));
    }

    public static Text getMessage(Text msg) {
        return msg;
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public McooBuildwithEaseNetworkChannel getChannel() {
        return networkChannel;
    }

    public McooBuildwithEaseStructureBuilder getStructureBuilder() {
        return structureBuilder;
    }

    public McooBuildwithEaseConfigStorage getSessionConfigStorage() {
        return sessionConfigStorage;
    }

    public McooBuildwithEaseSessionManager getSessionManager() {
        return sessionManager;
    }

    public McooBuildwithEaseServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public String getId() {
        return MOD_ID;
    }

}
