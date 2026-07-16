package dev.lucaargolo.nexo.role.entity;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public class MinecraftPlayerRole {

    public static Optional<EntityType<Player>> craft(EntityBase base) {
        if (base.has(PlayerRole.class)) {
            return Optional.of(EntityType.PLAYER);
        }
        return Optional.empty();
    }

    public static Optional<PlayerRole> uncraft(NexoRegistryHandler<?> handler, EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return Optional.of(new PlayerRole(new UUID(0 ,0), "Null"));
        };
        return Optional.empty();
    }

}
