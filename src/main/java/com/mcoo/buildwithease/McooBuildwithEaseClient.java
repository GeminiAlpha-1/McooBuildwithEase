package com.mcoo.buildwithease;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.RenderManager;
import dev.huskuraft.universal.api.events.impl.ClientEventRegistry;
import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.api.platform.PlatformLoader;

@AutoService(ClientEntrance.class)
public class McooBuildwithEaseClient implements ClientEntrance {

    private final ClientEventRegistry eventRegistry = PlatformLoader.getSingleton(ClientEventRegistry.class);
    private final McooBuildwithEaseClientNetworkChannel channel = new McooBuildwithEaseClientNetworkChannel(this);
    private final McooBuildwithEaseClientStructureBuilder structureBuilder = new McooBuildwithEaseClientStructureBuilder(this);
    private final McooBuildwithEaseClientManager clientManager = new McooBuildwithEaseClientManager(this);
    private final McooBuildwithEaseClientTagConfigStorage tagConfigStorage = new McooBuildwithEaseClientTagConfigStorage(this);
    private final McooBuildwithEaseClientConfigStorage configStorage = new McooBuildwithEaseClientConfigStorage(this);
    private final McooBuildwithEaseClientSessionManager sessionManager = new McooBuildwithEaseClientSessionManager(this);
    private final RenderManager renderManager = new RenderManager();

    public McooBuildwithEaseClient() {
        // 渲染模块在这里被发现和注册 —— 平台层的上下文工厂必须已经就位
        // （FabricClientInitializer 里先 set 再 getInstance()，顺序是对的）
        renderManager.initialize();
    }

    public static McooBuildwithEaseClient getInstance() {
        return (McooBuildwithEaseClient) ClientEntrance.getInstance();
    }

    public ClientEventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public McooBuildwithEaseClientNetworkChannel getChannel() {
        return channel;
    }

    public McooBuildwithEaseClientStructureBuilder getStructureBuilder() {
        return structureBuilder;
    }

    @Override
    public McooBuildwithEaseClientManager getClientManager() {
        return clientManager;
    }

    @Deprecated
    public McooBuildwithEaseClientTagConfigStorage getTagConfigStorage() {
        return tagConfigStorage;
    }

    public McooBuildwithEaseClientConfigStorage getConfigStorage() {
        return configStorage;
    }

    public McooBuildwithEaseClientSessionManager getSessionManager() {
        return sessionManager;
    }

    /** 自建渲染器的总管。 */
    public RenderManager getRenderManager() {
        return renderManager;
    }

    @Override
    public String getId() {
        return McooBuildwithEase.MOD_ID;
    }

}
