package dev.lucaargolo.nexo.role;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.entity.EntityBase;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.entity.PlayerRole;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.api.role.screen.InventoryRole;
import dev.lucaargolo.nexo.feature.entity.MinecraftEntity;
import dev.lucaargolo.nexo.feature.screen.MinecraftScreen;
import dev.lucaargolo.nexo.role.entity.MinecraftPlayerRole;
import dev.lucaargolo.nexo.role.item.MinecraftBlockItemRole;
import dev.lucaargolo.nexo.role.screen.MinecraftInventoryRole;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftRoleType<F extends Feature<?, ?>, M, E, P> {

    private static final Map<Feature.Type<?, ?>, List<MinecraftRoleType<?, ?, ?, ?>>> TYPES = new HashMap<>();

    public static final MinecraftRoleType<ItemBase, Item, Item, Item.Properties> BLOCK_ITEM = new MinecraftRoleType<>(Feature.Type.ITEM, Item.class, MinecraftBlockItemRole::craft, MinecraftBlockItemRole::uncraft);
    public static final MinecraftRoleType<EntityBase, EntityType<?>, Entity, MinecraftEntity.Parameters> PLAYER = new MinecraftRoleType<>(Feature.Type.ENTITY, Nexo.type(EntityType.class), MinecraftPlayerRole::craft, MinecraftPlayerRole::uncraft);
    public static final MinecraftRoleType<ScreenBase<?>, MinecraftScreen.ScreenCrafter, Screen, MinecraftScreen.ScreenParameters<?>> INVENTORY_SCREEN = new MinecraftRoleType<>(Feature.Type.SCREEN, Nexo.type(MinecraftScreen.ScreenCrafter.class), MinecraftInventoryRole::craftScreen, MinecraftInventoryRole::uncraftScreen);
    public static final MinecraftRoleType<ScreenBase<?>, MinecraftScreen.MenuCrafter<?>, AbstractContainerMenu, MinecraftScreen.MenuParameters<?>> INVENTORY_MENU = new MinecraftRoleType<>(Feature.Type.SCREEN, Nexo.type(MinecraftScreen.MenuCrafter.class), MinecraftInventoryRole::craftMenu, MinecraftInventoryRole::uncraftMenu);

    private final Feature.Type<F, ?> type;
    private final Class<M> clazz;
    private final BiFunction<NexoMinecraft<?, ?, ?, ?>, F, Info<E, P>> craft;
    private final BiFunction<NexoMinecraft<?, ?, ?, ?>, M, Role> uncraft;

    public MinecraftRoleType(Feature.Type<F, ?> type, Class<M> clazz, BiFunction<NexoMinecraft<?, ?, ?, ?>, F, Info<E, P>> craft, BiFunction<NexoMinecraft<?, ?, ?, ?>, M, Role> uncraft) {
        this.type = type;
        this.clazz = clazz;
        this.craft = craft;
        this.uncraft = uncraft;
        TYPES.computeIfAbsent(type, t -> new ArrayList<>()).add(this);
    }

    private Info<E, P> innerCraft(NexoMinecraft<?, ?, ?, ?> nexo, Feature<?, ?> feature) {
        if (this.type.isInstance(feature)) {
            return this.craft.apply(nexo, this.type.cast(feature));
        }
        return null;
    }

    private Role innerUncraft(NexoMinecraft<?, ?, ?, ?> nexo, Object object) {
        if (this.clazz.isInstance(object)) {
            return this.uncraft.apply(nexo, this.clazz.cast(object));
        }
        return null;
    }

    public static <F extends Feature<?, ?>, M, E, P> @Nullable Info<E, P> craft(NexoMinecraft<?, ?, ?, ?> nexo, F feature, Class<M> type) {
        List<MinecraftRoleType<?, ?, ?, ?>> list = TYPES.getOrDefault(feature.type(), List.of());
        for (MinecraftRoleType<?, ?, ?, ?> roleType : list) {
            Info<?, ?> optional = roleType.innerCraft(nexo, feature);
            if (optional != null) {
                Class<MinecraftRoleType<F, M, E, P>> clazz = Nexo.type(MinecraftRoleType.class);
                MinecraftRoleType<F, M, E, P> typedRoleType = clazz.cast(roleType);
                if(type.isAssignableFrom(optional.extender.type())) {
                    return typedRoleType.innerCraft(nexo, feature);
                }
            }
        }
        return null;
    }

    public static <F extends Feature<?, ?>, M> Supplier<Role> uncraft(NexoMinecraft<?, ?, ?, ?> nexo, Feature.Type<F, ?> type, Holder<M> holder) {
        return () -> {
            M crafted = holder.value();
            return innerUncraft(nexo, crafted, TYPES.getOrDefault(type, List.of()));
        };
    }

    public static <F extends Feature<?, ?>, M> Supplier<Role> uncraft(NexoMinecraft<?, ?, ?, ?> nexo, Feature.Type<F, ?> type, M crafted) {
        return () -> innerUncraft(nexo, crafted, TYPES.getOrDefault(type, List.of()));
    }

    @Nullable
    private static <F extends Feature<F, ?>, M> Role innerUncraft(NexoMinecraft<?, ?, ?, ?> nexo, M crafted, List<MinecraftRoleType<?, ?, ?, ?>> list) {
        for (MinecraftRoleType<?, ?, ?, ?> roleType : list) {
            Role role = roleType.innerUncraft(nexo, crafted);
            if (role != null) {
                return role;
            }
        }
        return null;
    }

    public record Info<M, P>(@NotNull Utils.Extender<M> extender, @Nullable Function<P, M> factory) {

        public Info {
            Objects.requireNonNull(extender, "extender");
        }
    }

}
