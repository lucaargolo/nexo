package dev.lucaargolo.nexo.role;

import dev.lucaargolo.nexo.NexoRegistryHandler;
import dev.lucaargolo.nexo.api.feature.Feature;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.role.item.BlockItemRole;
import dev.lucaargolo.nexo.role.item.MinecraftBlockItemRole;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MinecraftRoleType<F extends Feature<F>, R extends Role, M, N extends M> {

    public static final MinecraftRoleType<ItemBase, BlockItemRole, Item, BlockItem> BLOCK_ITEM = new MinecraftRoleType<>(MinecraftBlockItemRole::craft, MinecraftBlockItemRole::uncraft);

    private final Function<F, Optional<N>> craft;
    private final BiFunction<NexoRegistryHandler<?>, M, Optional<R>> uncraft;

    public MinecraftRoleType(Function<F, Optional<N>> craft, BiFunction<NexoRegistryHandler<?>, M, Optional<R>> uncraft) {
        this.craft = craft;
        this.uncraft = uncraft;
    }

}
