package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.FabricItemStorageVault;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Set;

public class FabricMinecraftItemUnit extends MinecraftItemUnit {

    public FabricMinecraftItemUnit(@NotNull NexoMinecraft nexo, @NotNull ItemBase feature, @Nullable Role role, @NotNull ItemStack stack) {
        super(nexo, feature, role, stack);
    }

    @Override
    public @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Class<U> type) {
        if(type == ItemUnit.class) {
            if (this.itemStorage() != null) {
                return Set.of("inventory");
            }
        }
        return Set.of();
    }

    @Override
    public @Nullable <U extends Unit<?>> Vault<U> vault(@NotNull Class<U> type, @NotNull String key) {
        if(type == ItemUnit.class) {
            if (key.equals("inventory")) {
                return FabricItemStorageVault.create(this.nexo, type, this.itemStorage());
            }
        }
        return null;
    }

    private @Nullable Storage<ItemVariant> itemStorage() {
        return ItemStorage.ITEM.find(this.get(), ContainerItemContext.withConstant(this.get()));
    }
}