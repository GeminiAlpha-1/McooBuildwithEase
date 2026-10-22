package dev.huskuraft.universal.api.platform;

import dev.huskuraft.universal.api.events.impl.EventRegistry;
import dev.huskuraft.universal.api.networking.NetworkChannel;

import java.util.NoSuchElementException;
import java.util.Optional;

public interface Entrance {

    int PROTOCOL_VERSION = 0;

    static Entrance getInstance() {
        return PlatformLoader.getSingleton();
    }

    String getId();

    EventRegistry getEventRegistry();

    NetworkChannel<?> getChannel();

    ServerManager getServerManager();

    default Server getServer() {
        return getServerManager().getRunningServer();
    }

    /**
     * 查找可选插件的实现。
     *
     * <p>【本仓库修改】上游只捕获 {@link LinkageError}：它假设插件的实现类一定随 universal 一起打包，
     * 所以 ServiceLoader 找不到实现时会抛 {@link NoSuchElementException} 直接把客户端带崩。
     * 本移植按范围决策**不打包** FTB Chunks / OpenPAC 的实现（服务器上也没有这两个模组），
     * 因此这里必须把「插件没装」当成正常情况返回空 Optional，由调用方自行降级。
     */
    default <T extends Plugin> Optional<T> findPlugin(Class<T> pluginClass) {
        try {
            return Optional.of(PlatformLoader.getSingleton(pluginClass));
        } catch (LinkageError | NoSuchElementException e) {
            return Optional.empty();
        }
    }

}

