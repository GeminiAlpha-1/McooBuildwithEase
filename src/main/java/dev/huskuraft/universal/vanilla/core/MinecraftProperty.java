package dev.huskuraft.universal.vanilla.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.huskuraft.universal.api.core.Property;
import dev.huskuraft.universal.api.core.PropertyValue;

public record MinecraftProperty(
        net.minecraft.world.level.block.state.properties.Property<?> refs
) implements Property {

    /**
     * 26.2 起 {@code StateHolder#getValues()} 返回的是 {@code Stream<Property.Value<?>>}，
     * 不再是 Map。改成按 {@code getProperties()} 逐个取 {@code getValue(property)} 组装。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<Property, PropertyValue> mapOf(net.minecraft.world.level.block.state.StateHolder<?, ?> state) {
        var map = new LinkedHashMap<Property, PropertyValue>();
        for (Object property : state.getProperties()) {
            var p = (net.minecraft.world.level.block.state.properties.Property) property;
            map.put(new MinecraftProperty(p), new MinecraftPropertyValue((Comparable<?>) state.getValue(p)));
        }
        return map;
    }

    public String getName() {
        return refs.getName();
    }

    @Override
    public String getName(PropertyValue value) {
        return refs.getName(value.reference());
    }

    @Override
    public Optional<PropertyValue> getValue(String value) {
        return refs.getValue(value).map(MinecraftPropertyValue::new);
    }

    @Override
    public Collection<PropertyValue> getPossibleValues() {
        return refs.getPossibleValues().stream().map(MinecraftPropertyValue::new).collect(Collectors.toList());
    }

    @Override
    public Class<?> getValueClass() {
        return refs.getValueClass();
    }

}
