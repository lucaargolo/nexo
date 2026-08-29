package dev.lucaargolo.nexo.unit.item;

import dev.lucaargolo.nexo.FabricNexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.item.ItemBase;
import dev.lucaargolo.nexo.api.role.Role;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.FabricTransferVault;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FabricMinecraftItemUnit<C extends Role> extends MinecraftItemUnit<C> {

    public FabricMinecraftItemUnit(@NotNull FabricNexoMinecraft nexo, @NotNull ItemBase feature, @Nullable C role, @NotNull ItemStack stack) {
        super(nexo, feature, role, stack);
    }

    @Override
    protected @Nullable Vault<ItemUnit<?>> itemVault() {
        SingleSlotStorage<ItemVariant> mainSlot = new SingleStackStorage() {
            @Override
            protected @NotNull ItemStack getStack() {
                return FabricMinecraftItemUnit.this.get();
            }

            @Override
            protected void setStack(@NotNull ItemStack stack) {
                FabricMinecraftItemUnit.this.setStack(stack);
            }
        };
        ContainerItemContext context = new ContainerItemContext() {
            @Override
            public @NotNull SingleSlotStorage<ItemVariant> getMainSlot() {
                return mainSlot;
            }

            @Override
            public long insertOverflow(ItemVariant itemVariant, long maxAmount, TransactionContext transactionContext) {
                return 0;
            }

            @Override
            public @NotNull List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
                return List.of();
            }
        };
        Storage<ItemVariant> storage = ItemStorage.ITEM.find(this.get(), context);
        return storage == null ? null : new FabricTransferVault(this.nexo, storage);
    }

}
