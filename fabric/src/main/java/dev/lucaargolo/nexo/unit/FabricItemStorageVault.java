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

import java.util.Iterator;
import java.util.Objects;

public class FabricItemStorageVault implements Vault<ItemUnit> {

    protected final @NotNull NexoMinecraft nexo;
    protected final @NotNull Storage<ItemVariant> storage;
    protected final @NotNull ItemUnit empty;

    public FabricItemStorageVault(@NotNull NexoMinecraft nexo, @NotNull Storage<ItemVariant> storage) {
        this.nexo = nexo;
        this.storage = storage;
        this.empty = nexo.stackToUnit(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public boolean canInsert() {
        return this.storage.supportsInsertion();
    }

    @Override
    public int insert(@NotNull ItemUnit value, int max, boolean simulate) {
        if (max < 0) {
            throw new IllegalArgumentException("Insertion amount cannot be negative");
        }
        if (max == 0 || !this.canInsert()) {
            return 0;
        }

        ItemVariant variant = this.variant(value);
        if (variant.isBlank()) {
            return 0;
        }

        try (Transaction transaction = openTransaction()) {
            long inserted = this.storage.insert(variant, max, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return Math.toIntExact(inserted);
        }
    }

    @Override
    public boolean canExtract() {
        return this.storage.supportsExtraction();
    }

    @Override
    public int extract(@NotNull ItemUnit value, int max, boolean simulate) {
        if (max < 0) {
            throw new IllegalArgumentException("Extraction amount cannot be negative");
        }
        if (max == 0 || !this.canExtract()) {
            return 0;
        }

        ItemVariant variant = this.variant(value);
        if (variant.isBlank()) {
            return 0;
        }

        try (Transaction transaction = openTransaction()) {
            long extracted = this.storage.extract(variant, max, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return Math.toIntExact(extracted);
        }
    }

    @Override
    public void clear() {
        if (!this.canExtract()) {
            return;
        }

        try (Transaction transaction = openTransaction()) {
            for (StorageView<ItemVariant> view : this.storage) {
                if (view.isResourceBlank() || view.getAmount() == 0) {
                    continue;
                }

                long amount = view.getAmount();
                long extracted = view.extract(view.getResource(), amount, transaction);
                if (extracted != amount) {
                    throw new IllegalStateException("Could not clear Fabric item storage view");
                }
            }
            transaction.commit();
        }
    }

    @Override
    public boolean isEmpty() {
        return !this.storage.nonEmptyIterator().hasNext();
    }

    @Override
    public int maxStackAmount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public @NotNull Iterator<ItemUnit> iterator() {
        Iterator<StorageView<ItemVariant>> iterator = this.storage.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public @NotNull ItemUnit next() {
                return FabricItemStorageVault.this.unit(iterator.next());
            }
        };
    }

    protected @NotNull ItemUnit unit(@NotNull StorageView<ItemVariant> view) {
        if (view.isResourceBlank() || view.getAmount() == 0) {
            return this.empty;
        }
        return this.unit(view.getResource(), view.getAmount());
    }

    protected @NotNull ItemUnit unit(@NotNull ItemVariant variant, long amount) {
        if (variant.isBlank() || amount == 0) {
            return this.empty;
        }
        return this.nexo.stackToUnit(variant.toStack(Math.toIntExact(amount)));
    }

    protected @NotNull ItemVariant variant(@NotNull ItemUnit value) {
        if (!(value instanceof MinecraftItemUnit unit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return ItemVariant.of(unit.get());
    }

    @SuppressWarnings("deprecation")
    protected static @NotNull Transaction openTransaction() {
        return Transaction.openNested(Transaction.getCurrentUnsafe());
    }

    public static @Nullable <U extends Unit<?>> Vault<U> create(@NotNull NexoMinecraft nexo, @NotNull Class<U> type, @Nullable Storage<ItemVariant> storage) {
        if (storage == null) {
            return null;
        }
        if (type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit FabricItemStorageVault");
        }

        Vault<ItemUnit> vault;
        if (storage instanceof SlottedStorage<?> slottedStorage) {
            Class<SlottedStorage<ItemVariant>> slottedStorageType = Nexo.type(SlottedStorage.class);
            vault = new Slotted(nexo, slottedStorageType.cast(slottedStorage));
        } else {
            vault = new FabricItemStorageVault(nexo, storage);
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(vault);
    }

    public static final class Slotted extends FabricItemStorageVault implements Vault.Slotted<ItemUnit> {

        private final @NotNull SlottedStorage<ItemVariant> slottedStorage;

        public Slotted(@NotNull NexoMinecraft nexo, @NotNull SlottedStorage<ItemVariant> storage) {
            super(nexo, storage);
            this.slottedStorage = storage;
        }

        @Override
        public int slots() {
            return this.slottedStorage.getSlotCount();
        }

        @Override
        public @NotNull ItemUnit get(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.unit(this.slottedStorage.getSlot(slot));
        }

        @Override
        public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
            Objects.checkIndex(slot, this.slots());

            StorageView<ItemVariant> view = this.slottedStorage.getSlot(slot);
            ItemUnit previous = this.unit(view);
            ItemVariant oldVariant = view.getResource();
            long oldAmount = view.getAmount();
            ItemVariant newVariant = this.variant(value);
            long newAmount = newVariant.isBlank() ? 0 : ((MinecraftItemUnit) value).get().getCount();

            try (Transaction transaction = openTransaction()) {
                if (!oldVariant.isBlank() && oldAmount > 0) {
                    long extracted = view.extract(oldVariant, oldAmount, transaction);
                    if (extracted != oldAmount) {
                        throw new IllegalStateException("Could not clear Fabric item storage slot " + slot);
                    }
                }

                if (!newVariant.isBlank() && newAmount > 0) {
                    long inserted = this.slottedStorage.getSlot(slot).insert(newVariant, newAmount, transaction);
                    if (inserted != newAmount) {
                        throw new IllegalStateException("Could not set Fabric item storage slot " + slot);
                    }
                }

                transaction.commit();
            }

            return previous;
        }

        @Override
        public boolean canInsert(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.slottedStorage.getSlot(slot).supportsInsertion();
        }

        @Override
        public boolean canInsert(int slot, @NotNull ItemUnit value) {
            Objects.checkIndex(slot, this.slots());
            ItemVariant variant = this.variant(value);
            if (variant.isBlank() || !this.canInsert(slot)) {
                return false;
            }

            try (Transaction transaction = openTransaction()) {
                return this.slottedStorage.getSlot(slot).insert(variant, 1, transaction) > 0;
            }
        }

        @Override
        public boolean canExtract(int slot) {
            Objects.checkIndex(slot, this.slots());
            return this.slottedStorage.getSlot(slot).supportsExtraction();
        }

        @Override
        public int maxStackAmount(int slot) {
            Objects.checkIndex(slot, this.slots());
            long capacity = this.slottedStorage.getSlot(slot).getCapacity();
            return (int) Math.min(capacity, this.maxStackAmount());
        }

        @Override
        public int maxStackAmount(int slot, @NotNull ItemUnit value) {
            Objects.checkIndex(slot, this.slots());
            return value instanceof MinecraftItemUnit unit ? Math.min(this.maxStackAmount(slot), unit.get().getMaxStackSize()) : 0;
        }
    }
}
