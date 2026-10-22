package com.mcoo.buildwithease.renderer.pattern;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.platform.ClientEntrance;
import dev.huskuraft.universal.api.renderer.RenderFadeEntry;
import dev.huskuraft.universal.api.renderer.Renderer;
import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.pattern.array.ArrayTransformer;
import com.mcoo.buildwithease.building.pattern.mirror.MirrorTransformer;
import com.mcoo.buildwithease.building.pattern.raidal.RadialTransformer;

public class PatternRenderer {

    private final Map<UUID, RenderFadeEntry<Context>> entries = Collections.synchronizedMap(new LinkedHashMap<>());
    private final ClientEntrance entrance;

    public PatternRenderer(ClientEntrance entrance) {
        this.entrance = entrance;
    }

    public McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) entrance;
    }

    public void tick() {
        var iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            entry.tick();
            if (!entry.isAlive()) {
                iterator.remove();
            }
        }
    }

    public void showPattern(UUID uuid, Context context) {
        entries.put(uuid, new RenderFadeEntry<>(context));
    }

    public void render(Renderer renderer, float deltaTick) {
        entries.forEach((k, v) -> {
            if (!v.getValue().pattern().enabled()) return;
            for (var transformer : v.getValue().pattern().transformers()) {
                if (transformer instanceof MirrorTransformer mirrorTransformer) {
                    new MirrorTransformerRenderer(getEntrance(), mirrorTransformer, true).render(renderer, deltaTick);
                }
                if (transformer instanceof ArrayTransformer arrayTransformer) {
                    new ArrayTransformerRenderer(getEntrance(), arrayTransformer).render(renderer, deltaTick);
                }
                if (transformer instanceof RadialTransformer radialTransformer) {
                    new RadialTransformerRenderer(getEntrance(), radialTransformer, true).render(renderer, deltaTick);
                }
            }
        });
    }


}

