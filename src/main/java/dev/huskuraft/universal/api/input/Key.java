package dev.huskuraft.universal.api.input;

import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.api.platform.PlatformReference;
import dev.huskuraft.universal.api.text.Text;

public interface Key extends PlatformReference {

    String getName();

    int getValue();

    default boolean isDown() {
        return ClientEntrance.getInstance().getClient().getWindow().isKeyDown(getValue());
    }

    default Text getNameText() {
        return Text.translate(getName());
    }

}
