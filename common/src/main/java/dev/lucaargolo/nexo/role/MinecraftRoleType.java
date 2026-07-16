package dev.lucaargolo.nexo.role;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.role.entity.MinecraftPlayerRole;
import dev.lucaargolo.nexo.role.item.MinecraftBlockItemRole;
import dev.lucaargolo.nexo.util.NexoHolder;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftRoleType<R extends Role, F extends Feature<F>, M, N extends M> {

    private static final Map<Feature.Type<?>, List<MinecraftRoleType<?, ?, ?, ?>>> TYPES = new HashMap<>();

    public static final MinecraftRoleType<BlockItemRole, ItemBase, Item, BlockItem> BLOCK_ITEM = new MinecraftRoleType<>(Feature.Type.ITEM, Item.class, MinecraftBlockItemRole::craft, MinecraftBlockItemRole::uncraft);
    public static final MinecraftRoleType<PlayerRole, EntityBase, EntityType<?>, EntityType<Player>> PLAYER = new MinecraftRoleType<>(Feature.Type.ENTITY, NexoUtils.type(EntityType.class), MinecraftPlayerRole::craft, MinecraftPlayerRole::uncraft);

    private final Feature.Type<F> type;
    private final Class<M> clazz;
    private final Function<F, Optional<N>> craft;
    private final BiFunction<NexoRegistryHandler<?>, M, Optional<R>> uncraft;

    public MinecraftRoleType(Feature.Type<F> type, Class<M> clazz, Function<F, Optional<N>> craft, BiFunction<NexoRegistryHandler<?>, M, Optional<R>> uncraft) {
        this.type = type;
        this.clazz = clazz;
        this.craft = craft;
        this.uncraft = uncraft;
        TYPES.computeIfAbsent(type, t -> new ArrayList<>()).add(this);
    }

    private Optional<N> craft(Feature<?> feature) {
        if(this.type.isInstance(feature)) {
            return this.craft.apply(this.type.cast(feature));
        }
        return Optional.empty();
    }

    private Optional<R> uncraft(NexoRegistryHandler<?> helper, Object object) {
        if(this.clazz.isInstance(object)) {
            return this.uncraft.apply(helper, this.clazz.cast(object));
        }
        return Optional.empty();
    }

    public static <F extends Feature<F>, M> M craft(Class<M> type, M crafted, F feature) {
        List<MinecraftRoleType<?, ?, ?, ?>> list = TYPES.getOrDefault(feature.type(), List.of());
        for(MinecraftRoleType<?, ?, ?, ?> role : list) {
            Optional<M> optional = role.craft(feature).map(type::cast);
            if(optional.isPresent()) {
                return optional.get();
            }
        }
        return crafted;
    }

    public static <F extends Feature<F>, M> Supplier<Role> uncraft(NexoRegistryHandler<?> helper, Feature.Type<F> type, NexoHolder<M> holder) {
        return () -> {
            M crafted = holder.get();
            List<MinecraftRoleType<?, ?, ?, ?>> list = TYPES.getOrDefault(type, List.of());
            for(MinecraftRoleType<?, ?, ?, ?> role : list) {
                Optional<? extends Role> optional = role.uncraft(helper, crafted);
                if(optional.isPresent()) {
                    return optional.get();
                }
            }
            return null;
        };
    }

}
