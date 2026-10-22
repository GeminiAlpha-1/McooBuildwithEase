package dev.huskuraft.universal.api.core;

import dev.huskuraft.universal.api.math.Vector3d;

public record EntityInteraction(Vector3d position, Player entity) implements Interaction {

    public EntityInteraction(Player entity) {
        this(entity.getPosition(), entity);
    }

    public Player getEntity() {
        return this.entity;
    }

    public Target getTarget() {
        return Target.ENTITY;
    }

    @Override
    public Vector3d getPosition() {
        return position;
    }
}
