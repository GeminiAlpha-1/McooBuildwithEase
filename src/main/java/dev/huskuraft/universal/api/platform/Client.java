package dev.huskuraft.universal.api.platform;

import java.util.List;

import dev.huskuraft.universal.api.core.Interaction;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.PlayerInfo;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.gui.Screen;
import dev.huskuraft.universal.api.gui.Typeface;
import dev.huskuraft.universal.api.renderer.Camera;
import dev.huskuraft.universal.api.renderer.Window;
import dev.huskuraft.universal.api.sound.SoundManager;

public interface Client extends PlatformReference {

    Window getWindow();

    Camera getCamera();

    Screen getPanel();

    void setPanel(Screen screen);

    Player getPlayer();

    List<PlayerInfo> getOnlinePlayers();

    Typeface getTypeface();

    World getWorld();

    boolean isLoaded();

    Interaction getLastInteraction();

    String getClipboard();

    void setClipboard(String content);

    SoundManager getSoundManager();

    void sendChat(String chat);

    void sendCommand(String command);

    void execute(Runnable runnable);

    Options getOptions();

    ParticleEngine getParticleEngine();

    boolean isLocalServer();

    boolean hasSinglePlayerServer();

}
