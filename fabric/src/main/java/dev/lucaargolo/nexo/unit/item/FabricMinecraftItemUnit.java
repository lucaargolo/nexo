package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.FabricStorageVault;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricMinecraftItemUnit<C extends Role> extends MinecraftItemUnit<C> {

    public FabricMinecraftItemUnit(@NotNull FabricNexoMinecraft nexo, @NotNull ItemBase feature, @Nullable C role, @NotNull ItemStack stack) {
        super(nexo, feature, role, stack);
    }

    @Override
    protected @Nullable Vault<ItemUnit<?>> itemVault() {
        ContainerItemContext context = ContainerItemContext.withConstant(this.get());
        Storage<ItemVariant> storage = ItemStorage.ITEM.find(this.get(), context);
        return storage == null ? null : new FabricStorageVault(this.nexo, storage);
    }

}
