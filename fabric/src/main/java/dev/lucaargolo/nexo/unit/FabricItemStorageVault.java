package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FabricItemStorageVault implements Vault<ItemUnit> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull Storage<ItemVariant> storage;
    private final @NotNull List<StorageView<ItemVariant>> views;
    private final @NotNull ItemUnit empty;

    public FabricItemStorageVault(@NotNull NexoMinecraft nexo, @NotNull Storage<ItemVariant> storage) {
        this.nexo = nexo;
        this.storage = storage;
        this.views = new ArrayList<>();
        storage.forEach(views::add);
        this.empty = nexo.stackToUnit(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public int slots() {
        return this.views.size();
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());

        StorageView<ItemVariant> view = this.views.get(slot);

        if (view.isResourceBlank()) {
            return this.empty;
        }

        ItemVariant variant = view.getResource();
        long amount = view.getAmount();
        return this.nexo.stackToUnit(variant.toStack(Math.toIntExact(amount)));
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        Objects.checkIndex(slot, this.slots());

        StorageView<ItemVariant> view = this.views.get(slot);

        ItemVariant oldVariant = view.getResource();
        long oldAmount = view.getAmount();
        ItemUnit previous = this.nexo.stackToUnit(oldVariant.toStack(Math.toIntExact(oldAmount)));

        MinecraftItemUnit minecraftValue = (MinecraftItemUnit) value;
        ItemStack newStack = minecraftValue.get(); // use your actual accessor here

        try (Transaction transaction = Transaction.openOuter()) {
            // Remove exactly what is currently in the requested view.
            if (!oldVariant.isBlank() && oldAmount > 0) {
                long extracted = view.extract(oldVariant, oldAmount, transaction);

                if (extracted != oldAmount) {
                    throw new IllegalStateException("Could not clear slot " + slot);
                }
            }

            // Insert the replacement somewhere into the storage.
            if (!newStack.isEmpty()) {
                ItemVariant newVariant = ItemVariant.of(newStack);
                long newAmount = newStack.getCount();

                long inserted = this.storage.insert(newVariant, newAmount, transaction);

                if (inserted != newAmount) {
                    throw new IllegalStateException("Could not insert replacement item into storage");
                }
            }

            transaction.commit();
        }

        return previous;
    }

    @Override
    public boolean canAdd() {
        return this.storage.supportsInsertion();
    }

    @Override
    public boolean canRemove() {
        return this.storage.supportsExtraction();
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if(storage == null) {
            return null;
        }
        if(type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit FabricItemStorageVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new FabricItemStorageVault(nexo, storage));
    }
}
