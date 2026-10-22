package com.mcoo.buildwithease.building.session;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;
import com.mcoo.buildwithease.building.interceptor.BuildInterceptor;
import com.mcoo.buildwithease.building.operation.OperationResult;

import java.util.List;

public interface Session {

    World getWorld();

    Player getPlayer();

    OperationResult commit();

    List<BuildInterceptor> getInterceptors();

}
