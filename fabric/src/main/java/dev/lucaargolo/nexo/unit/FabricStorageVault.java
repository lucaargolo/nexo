package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FabricStorageVault extends AbstractCollection<ItemUnit<?>> implements Vault<ItemUnit<?>> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    final @NotNull Storage<ItemVariant> storage;

    public FabricStorageVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Storage<ItemVariant> storage) {
        this.nexo = nexo;
        this.storage = storage;
    }

    public static @NotNull <U extends Unit<?, ?>> Set<String> vaults(@NotNull Set<String> existing, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if (storage == null || !MinecraftContainerVault.supports(type)) {
            return existing;
        }
        Set<String> vaults = new HashSet<>(existing);
        vaults.add(MinecraftContainerVault.KEY);
        return Set.copyOf(vaults);
    }

    public static @Nullable <U extends Unit<?, ?>> Vault<U> create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if (storage == null || !MinecraftContainerVault.supports(type)) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(new FabricStorageVault(nexo, storage));
    }

    @Override
    public boolean isFull() {
        if (storage instanceof SlottedStorage<?> slotted) {
            if (slotted.getSlotCount() == 0) {
                return true;
            }
            for (int slot = 0; slot < slotted.getSlotCount(); slot++) {
                StorageView<?> view = slotted.getSlot(slot);
                if (view.getAmount() < view.getCapacity()) {
                    return false;
                }
            }
            return true;
        }

        boolean hasView = false;
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0) {
                return false;
            }
            hasView = true;
            if (view.getAmount() < view.getCapacity()) {
                return false;
            }
        }
        return hasView;
    }

    @Override
    public boolean canAdd() {
        return this.storage.supportsInsertion();
    }

    @Override
    public boolean canRemove() {
        return this.storage.supportsExtraction();
    }

    @Override
    public int size() {
        int size = 0;
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            size++;
        }
        return size;
    }

    @Override
    public boolean contains(Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            if (view.getResource().matches(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(@NotNull ItemUnit<?> item) {
        if (!(item instanceof MinecraftItemUnit<?> minecraftItem)) {
            throw new IllegalArgumentException("FabricStorageVault only accepts MinecraftItemUnit instances");
        }
        ItemStack source = minecraftItem.get();
        if (source.isEmpty()) {
            return false;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(source), source.getCount(), transaction);
            if (inserted <= 0) {
                return false;
            }
            transaction.commit();
            this.contentsChanged();
            return true;
        }
    }

    @Override
    public boolean remove(Object object) {
        if (!(object instanceof MinecraftItemUnit<?> item)) {
            return false;
        }
        ItemStack target = item.get();
        if (target.isEmpty()) {
            return false;
        }
        ItemVariant variant = ItemVariant.of(target);
        try (Transaction transaction = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
                if (view.getAmount() < target.getCount() || !view.getResource().matches(target)) {
                    continue;
                }
                long extracted = view.extract(variant, target.getCount(), transaction);
                if (extracted != target.getCount()) {
                    return false;
                }
                transaction.commit();
                this.contentsChanged();
                return true;
            }
            return false;
        }
    }

    @Override
    public void clear() {
        try (Transaction transaction = Transaction.openOuter()) {
            boolean changed = false;
            for (Iterator<StorageView<ItemVariant>> iterator = storage.nonEmptyIterator(); iterator.hasNext();) {
                StorageView<ItemVariant> view = iterator.next();
                view.extract(view.getResource(), view.getAmount(), transaction);
                changed = true;
            }
            transaction.commit();
            if (changed) {
                this.contentsChanged();
            }
        }
    }

    @Override
    public @NotNull Iterator<ItemUnit<?>> iterator() {
        Iterator<StorageView<ItemVariant>> iterator = storage.nonEmptyIterator();
        return new Iterator<>() {
            private StorageView<ItemVariant> current;
            private long currentAmount;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public @NotNull ItemUnit<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                current = iterator.next();
                currentAmount = current.getAmount();
                return nexo.stackToUnit(current.getResource().toStack((int) Math.min(Integer.MAX_VALUE, currentAmount)));
            }

            @Override
            public void remove() {
                if (current == null) {
                    throw new IllegalStateException();
                }
                try (Transaction transaction = Transaction.openOuter()) {
                    long extracted = current.extract(current.getResource(), currentAmount, transaction);
                    if (extracted != currentAmount) {
                        throw new IllegalStateException("Fabric transfer storage rejected vault iterator removal");
                    }
                    transaction.commit();
                    FabricStorageVault.this.contentsChanged();
                }
                current = null;
            }
        };
    }

}
