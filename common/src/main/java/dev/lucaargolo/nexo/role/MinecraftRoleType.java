package dev.lucaargolo.nexo.role;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.role.entity.MinecraftPlayerRole;
import dev.lucaargolo.nexo.role.item.MinecraftBlockItemRole;
import dev.lucaargolo.nexo.util.NexoUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftRoleType<R extends Role, F extends Feature<F, ?>, M, P> {

    private static final Map<Feature.Type<?, ?>, List<MinecraftRoleType<?, ?, ?, ?>>> TYPES = new HashMap<>();

    public static final MinecraftRoleType<BlockItemRole, ItemBase, Item, Item.Properties> BLOCK_ITEM = new MinecraftRoleType<>(Feature.Type.ITEM, Item.class, MinecraftBlockItemRole::craft, MinecraftBlockItemRole::uncraft);
    public static final MinecraftRoleType<PlayerRole, EntityBase, EntityType<?>, ?> PLAYER = new MinecraftRoleType<>(Feature.Type.ENTITY, Nexo.type(EntityType.class), MinecraftPlayerRole::craft, MinecraftPlayerRole::uncraft);

    private final Feature.Type<F, ?> type;
    private final Class<M> clazz;
    private final BiFunction<NexoMinecraft, F, Info<M, P>> craft;
    private final BiFunction<NexoRegistryHandler<?>, M, R> uncraft;

    public MinecraftRoleType(Feature.Type<F, ?> type, Class<M> clazz, BiFunction<NexoMinecraft, F, Info<M, P>> craft, BiFunction<NexoRegistryHandler<?>, M, R> uncraft) {
        this.type = type;
        this.clazz = clazz;
        this.craft = craft;
        this.uncraft = uncraft;
        TYPES.computeIfAbsent(type, t -> new ArrayList<>()).add(this);
    }

    private Info<M, P> innerCraft(NexoRegistryHandler<?> helper, Feature<?, ?> feature) {
        if(this.type.isInstance(feature)) {
            return this.craft.apply(helper.nexo(), this.type.cast(feature));
        }
        return null;
    }

    private R innerUncraft(NexoRegistryHandler<?> helper, Object object) {
        if(this.clazz.isInstance(object)) {
            return this.uncraft.apply(helper, this.clazz.cast(object));
        }
        return null;
    }

    public static <F extends Feature<F, ?>, M, P> Info<M, P> craft(NexoRegistryHandler<?> helper, Class<M> featureType, Class<P> parameterType, F feature) {
        List<MinecraftRoleType<?, ?, ?, ?>> list = TYPES.getOrDefault(feature.type(), List.of());
        for(MinecraftRoleType<?, ?, ?, ?> roleType : list) {
            Info<?, ?> optional = roleType.innerCraft(helper, feature);
            if(optional != null) {
                Class<MinecraftRoleType<?, F, M, P>> clazz = Nexo.type(MinecraftRoleType.class);
                MinecraftRoleType<?, F, M, P> typedRoleType = clazz.cast(roleType);
                return typedRoleType.innerCraft(helper, feature);
            }
        }
        NexoUtils.Extender<M> extender = NexoUtils.extend(helper.nexo(), featureType);
        Function<P, M> function = extender::instantiate;
        return new MinecraftRoleType.Info<>(extender, function);
    }

    public static <F extends Feature<F, ?>, M> Supplier<Role> uncraft(NexoRegistryHandler<?> helper, Feature.Type<F, ?> type, Holder<M> holder) {
        return () -> {
            M crafted = holder.value();
            List<MinecraftRoleType<?, ?, ?, ?>> list = TYPES.getOrDefault(type, List.of());
            for(MinecraftRoleType<?, ?, ?, ?> roleType : list) {
                Role role = roleType.innerUncraft(helper, crafted);
                if(role != null) {
                    return role;
                }
            }
            return null;
        };
    }

    public record Info<M, P>(NexoUtils.Extender<M> extender, Function<P, M> factory) {

    }

}
