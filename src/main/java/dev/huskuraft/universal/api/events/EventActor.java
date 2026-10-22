package dev.huskuraft.universal.api.events;

@FunctionalInterface
public interface EventActor<T> {

    EventResult get(T t);

}
