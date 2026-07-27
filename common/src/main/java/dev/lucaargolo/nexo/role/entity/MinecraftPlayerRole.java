package dev.lucaargolo.nexo.role.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.MinecraftRegistryHandler;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.UUID;
import java.util.function.Function;

public class MinecraftPlayerRole {

    public static MinecraftRoleType.Info<Entity, MinecraftEntity.Parameters> craft(NexoMinecraft nexo, EntityBase base) {
        if (base.role() instanceof PlayerRole(UUID uuid, String name)) {
            Utils.Extender<Entity> extender = Utils.extend(nexo, ServerPlayer.class);
            Function<MinecraftEntity.Parameters, Entity> function = parameters -> nexo.createFakePlayer(parameters.level(), uuid, name);
            return new MinecraftRoleType.Info<>(extender, function);
        }
        return null;
    }

    public static PlayerRole uncraft(MinecraftRegistryHandler<?> handler, EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return new PlayerRole(new UUID(0 ,0), "Null");
        };
        return null;
    }

}
