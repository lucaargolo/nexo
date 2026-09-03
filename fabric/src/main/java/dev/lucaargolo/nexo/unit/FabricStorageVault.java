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

public final class FabricStorageVault extends AbstractList<ItemUnit> implements Vault<ItemUnit> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull ItemUnit defaultValue;
    final @NotNull Storage<ItemVariant> storage;
    private final @Nullable SlottedStorage<ItemVariant> slottedStorage;
    private final int slotCount;

    public FabricStorageVault(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Storage<ItemVariant> storage) {
        this.nexo = nexo;
        this.defaultValue = MinecraftItemVault.emptyValue(nexo);
        this.storage = storage;
        Class<SlottedStorage<ItemVariant>> type = Nexo.type(SlottedStorage.class);
        this.slottedStorage = storage instanceof SlottedStorage<?> ? type.cast(storage) : null;
        this.slotCount = this.slottedStorage == null ? -1 : this.slottedStorage.getSlotCount();
    }

    @Override
    public @NotNull ItemUnit defaultValue() {
        return this.defaultValue;
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        Objects.checkIndex(slot, this.size());
        StorageView<ItemVariant> view = this.view(slot);
        if (view == null || view.isResourceBlank() || view.getAmount() <= 0) {
            return this.defaultValue();
        }
        return this.nexo.stackToUnit(view.getResource().toStack((int) Math.min(Integer.MAX_VALUE, view.getAmount())));
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit item) {
        Objects.checkIndex(slot, this.size());
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
            throw new IllegalArgumentException("FabricStorageVault only accepts MinecraftItemUnit instances");
        }
        ItemUnit previous = this.get(slot);
        ItemStack stack = minecraftItem.get();
        StorageView<ItemVariant> view = this.view(slot);
        Storage<ItemVariant> target = this.slottedStorage == null ? this.storage : this.slottedStorage.getSlot(slot);
        try (Transaction transaction = Transaction.openOuter()) {
            if (view != null && !view.isResourceBlank() && view.getAmount() > 0) {
                long amount = view.getAmount();
                long extracted = view.extract(view.getResource(), amount, transaction);
                if (extracted != amount) {
                    throw new IllegalArgumentException("Fabric storage rejected item for slot " + slot);
                }
            }
            long inserted = stack.isEmpty() ? 0 : target.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (inserted != stack.getCount() || (this.slottedStorage != null && !stack.isEmpty() && !this.matches(this.view(slot), stack))) {
                throw new IllegalArgumentException("Fabric storage rejected item for slot " + slot);
            }
            transaction.commit();
            this.contentsChanged();
        }
        return previous;
    }

    public static @NotNull <U extends Unit<?>> Set<String> vaults(@NotNull Set<String> existing, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if (storage == null || !MinecraftContainerVault.supports(type)) {
            return existing;
        }
        Set<String> vaults = new HashSet<>(existing);
        vaults.add(MinecraftContainerVault.KEY);
        return Set.copyOf(vaults);
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if (storage == null || !MinecraftContainerVault.supports(type)) {
            return null;
        }
        Class<Vault<U>> clazz = Nexo.type(Vault.class);
        return clazz.cast(new FabricStorageVault(nexo, storage));
    }

    @Override
    public boolean isFull() {
        if (this.slottedStorage != null) {
            if (this.slottedStorage.getSlotCount() == 0) {
                return true;
            }
            for (int slot = 0; slot < this.slottedStorage.getSlotCount(); slot++) {
                StorageView<ItemVariant> view = this.slottedStorage.getSlot(slot);
                if (view.isResourceBlank() || view.getAmount() <= 0 || view.getAmount() < view.getCapacity()) {
                    return false;
                }
            }
            return true;
        }
        boolean hasView = false;
        for (StorageView<ItemVariant> view : this.storage) {
            if (view.isResourceBlank() || view.getAmount() <= 0 || view.getAmount() < view.getCapacity()) {
                return false;
            }
            hasView = true;
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
        if (this.slotCount >= 0) {
            return this.slotCount;
        }
        int size = 0;
        for (StorageView<ItemVariant> view : this.storage.nonEmptyViews()) {
            size++;
        }
        return size;
    }

    @Override
    public boolean contains(Object object) {
        if (!(object instanceof MinecraftItemUnit item)) {
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
    public boolean add(@NotNull ItemUnit item) {
        if (!(item instanceof MinecraftItemUnit minecraftItem)) {
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
    public void setContents(@NotNull Collection<? extends ItemUnit> contents) {
        if (this.slottedStorage != null) {
            Vault.super.setContents(contents);
            return;
        }
        List<? extends ItemUnit> copy = new ArrayList<>(contents);
        this.clear();
        for (ItemUnit item : copy) {
            this.add(item);
        }
    }

    @Override
    public boolean remove(Object object) {
        if (!(object instanceof MinecraftItemUnit item)) {
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
    public @NotNull ItemUnit remove(int slot) {
        Objects.checkIndex(slot, this.size());
        ItemUnit previous = this.get(slot);
        StorageView<ItemVariant> view = this.view(slot);
        if (view != null && !view.isResourceBlank() && view.getAmount() > 0) {
            try (Transaction transaction = Transaction.openOuter()) {
                long amount = view.getAmount();
                long extracted = view.extract(view.getResource(), amount, transaction);
                if (extracted != amount) {
                    throw new IllegalStateException("Fabric transfer storage rejected vault indexed removal");
                }
                transaction.commit();
                this.contentsChanged();
            }
        }
        return previous;
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

    private @Nullable StorageView<ItemVariant> view(int slot) {
        if (this.slottedStorage != null) {
            return this.slottedStorage.getSlot(slot);
        }
        int index = 0;
        for (StorageView<ItemVariant> view : this.storage.nonEmptyViews()) {
            if (index++ == slot) {
                return view;
            }
        }
        return null;
    }

    private boolean matches(@Nullable StorageView<ItemVariant> view, @NotNull ItemStack stack) {
        return view != null && !view.isResourceBlank() && view.getAmount() == stack.getCount() && view.getResource().matches(stack);
    }

}
