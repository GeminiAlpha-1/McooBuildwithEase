package dev.huskuraft.universal.vanilla.platform;

import java.util.List;
import java.util.stream.Collectors;

import dev.huskuraft.universal.api.core.Interaction;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.PlayerInfo;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.gui.Screen;
import dev.huskuraft.universal.api.gui.Typeface;
import dev.huskuraft.universal.api.platform.Client;
import dev.huskuraft.universal.api.platform.Options;
import dev.huskuraft.universal.api.platform.ParticleEngine;
import dev.huskuraft.universal.api.renderer.Camera;
import dev.huskuraft.universal.api.renderer.Window;
import dev.huskuraft.universal.api.sound.SoundManager;
import dev.huskuraft.universal.vanilla.core.MinecraftConvertor;
import dev.huskuraft.universal.vanilla.core.MinecraftPlayer;
import dev.huskuraft.universal.vanilla.core.MinecraftPlayerInfo;
import dev.huskuraft.universal.vanilla.core.MinecraftWorld;
import dev.huskuraft.universal.vanilla.gui.MinecraftProxyScreen;
import dev.huskuraft.universal.vanilla.gui.MinecraftScreen;
import dev.huskuraft.universal.vanilla.gui.MinecraftTypeface;
import dev.huskuraft.universal.vanilla.renderer.MinecraftCamera;
import dev.huskuraft.universal.vanilla.renderer.MinecraftWindow;
import dev.huskuraft.universal.vanilla.sound.MinecraftParticleEngine;
import dev.huskuraft.universal.vanilla.sound.MinecraftSoundManager;
import net.minecraft.client.Minecraft;

public record MinecraftClient(
        Minecraft refs
) implements Client {

    @Override
    public Window getWindow() {
        return new MinecraftWindow(refs.getWindow());
    }

    @Override
    public Camera getCamera() {
        return new MinecraftCamera(refs.gameRenderer.mainCamera());
    }

    @Override
    public Screen getPanel() {
        // 26.2 起当前界面不再挂在 Minecraft.screen 上，而是由 Minecraft.gui 持有
        var currentScreen = refs.gui.screen();
        if (currentScreen == null) {
            return null;
        }
        if (currentScreen instanceof MinecraftProxyScreen proxyScreen) {
            return proxyScreen.getProxy();
        }
        return new MinecraftScreen(currentScreen);
    }

    @Override
    public void setPanel(Screen screen) {
        if (screen == null) {
            refs.gui.setScreen(null);
            return;
        }
        if (screen instanceof MinecraftScreen minecraftScreen) {
            refs.gui.setScreen(minecraftScreen.refs());
            return;
        }
        refs.gui.setScreen(new MinecraftProxyScreen(screen));

    }

    @Override
    public Player getPlayer() {
        return MinecraftPlayer.ofNullable(refs.player);
    }

    @Override
    public List<PlayerInfo> getOnlinePlayers() {
        if (refs.getConnection() == null) return List.of();
        return refs.getConnection().getOnlinePlayers().stream().map(MinecraftPlayerInfo::new).collect(Collectors.toList());
    }

    @Override
    public Typeface getTypeface() {
        return new MinecraftTypeface(refs.font);
    }

    @Override
    public World getWorld() {
        return MinecraftWorld.ofNullable(refs.level);
    }

    @Override
    public boolean isLoaded() {
        return getWorld() != null;
    }

    @Override
    public Interaction getLastInteraction() {
        return MinecraftConvertor.fromPlatformInteraction(refs.hitResult);
    }

    @Override
    public String getClipboard() {
        return refs.keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String content) {
        refs.keyboardHandler.setClipboard(content);
    }

    @Override
    public SoundManager getSoundManager() {
        return new MinecraftSoundManager(refs.getSoundManager());
    }

    @Override
    public void sendChat(String chat) {
        refs.getConnection().sendChat(chat);
    }

    @Override
    public void sendCommand(String command) {
        refs.getConnection().sendCommand(command);
    }

    @Override
    public void execute(Runnable runnable) {
        refs.execute(runnable);
    }

    @Override
    public Options getOptions() {
        return new MinecraftOptions(refs.options);
    }

    @Override
    public ParticleEngine getParticleEngine() {
        return new MinecraftParticleEngine(refs.particleEngine);
    }

    @Override
    public boolean isLocalServer() {
        return refs.isLocalServer();
    }

    @Override
    public boolean hasSinglePlayerServer() {
        return refs.hasSingleplayerServer();
    }
}
