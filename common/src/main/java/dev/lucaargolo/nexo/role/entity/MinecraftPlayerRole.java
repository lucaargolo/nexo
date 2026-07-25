package dev.lucaargolo.nexo.role.entity;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.role.MinecraftRoleType;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class MinecraftPlayerRole {

    public static MinecraftRoleType.Info<EntityType<?>, ?> craft(NexoMinecraft nexo, EntityBase base) {
        if (base.has(PlayerRole.class)) {
            NexoUtils.Extender<EntityType<?>> extender = NexoUtils.extend(nexo, Nexo.type(EntityType.class));
            Function<?, EntityType<?>> function = ignored -> EntityType.PLAYER;
            return new MinecraftRoleType.Info<>(extender, function);
        }
        return null;
    }

    public static PlayerRole uncraft(NexoRegistryHandler<?> handler, EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return new PlayerRole(new UUID(0 ,0), "Null");
        };
        return null;
    }

}
