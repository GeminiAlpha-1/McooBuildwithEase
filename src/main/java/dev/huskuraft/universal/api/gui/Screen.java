package dev.huskuraft.universal.api.gui;

import dev.huskuraft.universal.api.text.Text;

public interface Screen extends ContainerWidget {

    Text getScreenTitle();

    void init(int width, int height);

    void onAttach();

    void onDetach();

    boolean isPauseGame();

    void attach();

    void detach();

}

