package dev.huskuraft.universal.api.platform;

import dev.huskuraft.universal.api.gui.Screen;

public interface ClientManager {

    Client getRunningClient();

    void setRunningClient(Client client);

    void pushScreen(Screen screen);

    void popScreen(Screen screen);

}
