package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.SlottedVault;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FabricVaultItemStorage extends SnapshotParticipant<Integer> implements Storage<ItemVariant> {

    protected final @NotNull NexoMinecraft nexo;
    protected final @NotNull Vault<ItemUnit> vault;
    private final @NotNull List<Runnable> rollbacks = new ArrayList<>();

    public FabricVaultItemStorage(@NotNull NexoMinecraft nexo, @NotNull Vault<ItemUnit> vault) {
        this.nexo = nexo;
        this.vault = vault;
    }

    @Override
    public boolean supportsInsertion() {
        return this.vault.canAdd();
    }

    @Override
    public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0 || !this.vault.canAdd()) {
            return 0;
        }
        return this.insert(this.unit(resource), maxAmount, transaction);
    }

    @Override
    public boolean supportsExtraction() {
        return this.vault.canRemove();
    }

    @Override
    public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0 || !this.vault.canRemove()) {
            return 0;
        }
        return this.extract(this.unit(resource), maxAmount, transaction);
    }

    @Override
    public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
        Iterator<ItemUnit> units = this.vault.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return units.hasNext();
            }

            @Override
            public @NotNull StorageView<ItemVariant> next() {
                return new VaultView(units.next());
            }
        };
    }

    protected long insert(@NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
        long inserted = 0;
        while (inserted < maxAmount) {
            int requested = (int) Math.min(maxAmount - inserted, Integer.MAX_VALUE);
            int simulated = checkedAmount("insert", this.vault.insert(value, requested, true), requested);
            if (simulated == 0) {
                break;
            }

            this.updateSnapshots(transaction);
            int transferred = checkedAmount("insert", this.vault.insert(value, simulated, false), simulated);
            if (transferred > 0) {
                this.addRollback(() -> this.rollbackExtraction(value, transferred));
                inserted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return inserted;
    }

    protected long extract(@NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
        long extracted = 0;
        while (extracted < maxAmount) {
            int requested = (int) Math.min(maxAmount - extracted, Integer.MAX_VALUE);
            int simulated = checkedAmount("extract", this.vault.extract(value, requested, true), requested);
            if (simulated == 0) {
                break;
            }

            this.updateSnapshots(transaction);
            int transferred = checkedAmount("extract", this.vault.extract(value, simulated, false), simulated);
            if (transferred > 0) {
                this.addRollback(() -> this.rollbackInsertion(value, transferred));
                extracted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return extracted;
    }

    protected final @NotNull ItemUnit unit(@NotNull ItemVariant variant) {
        return this.nexo.stackToUnit(variant.toStack());
    }

    protected final @NotNull ItemVariant variant(@NotNull ItemUnit unit) {
        if (!(unit instanceof MinecraftItemUnit minecraftUnit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return ItemVariant.of(minecraftUnit.get());
    }

    protected final void addRollback(@NotNull Runnable rollback) {
        this.rollbacks.add(rollback);
    }

    @Override
    protected @NotNull Integer createSnapshot() {
        return this.rollbacks.size();
    }

    @Override
    protected void readSnapshot(@NotNull Integer snapshot) {
        for (int index = this.rollbacks.size() - 1; index >= snapshot; index--) {
            this.rollbacks.remove(index).run();
        }
    }

    @Override
    protected void onFinalCommit() {
        if (!this.rollbacks.isEmpty()) {
            this.rollbacks.clear();
            this.vault.changed();
        }
    }

    private void rollbackExtraction(@NotNull ItemUnit value, int amount) {
        int extracted = this.vault.extract(value, amount, false);
        if (extracted != amount) {
            throw new IllegalStateException("Could not roll back Fabric vault insertion");
        }
    }

    private void rollbackInsertion(@NotNull ItemUnit value, int amount) {
        int inserted = this.vault.insert(value, amount, false);
        if (inserted != amount) {
            throw new IllegalStateException("Could not roll back Fabric vault extraction");
        }
    }

    public static @NotNull Storage<ItemVariant> create(@NotNull NexoMinecraft nexo, @NotNull Vault<ItemUnit> vault) {
        if (vault instanceof SlottedVault<ItemUnit> slotted) {
            return new FabricVaultItemStorage.Slotted(nexo, slotted);
        }
        return new FabricVaultItemStorage(nexo, vault);
    }

    private static int checkedAmount(@NotNull String operation, int amount, int maximum) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException("Vault " + operation + " returned " + amount + " for a maximum of " + maximum);
        }
        return amount;
    }

    private class VaultView implements StorageView<ItemVariant> {

        protected final @NotNull ItemUnit value;

        protected VaultView(@NotNull ItemUnit value) {
            this.value = value;
        }

        @Override
        public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            ItemVariant stored = this.getResource();
            if (stored.isBlank() || !stored.equals(resource)) {
                return 0;
            }
            return FabricVaultItemStorage.this.extract(resource, Math.min(maxAmount, this.getAmount()), transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return this.getResource().isBlank();
        }

        @Override
        public @NotNull ItemVariant getResource() {
            return FabricVaultItemStorage.this.variant(this.value);
        }

        @Override
        public long getAmount() {
            return this.value.amount();
        }

        @Override
        public long getCapacity() {
            return this.value.maxAmount();
        }
    }

    public static class Slotted extends FabricVaultItemStorage implements SlottedStorage<ItemVariant> {

        private final @NotNull SlottedVault<ItemUnit> slottedVault;

        public Slotted(@NotNull NexoMinecraft nexo, @NotNull SlottedVault<ItemUnit> vault) {
            super(nexo, vault);
            this.slottedVault = vault;
        }

        @Override
        public boolean supportsInsertion() {
            if (!this.slottedVault.canAdd()) {
                return false;
            }
            for (int slot = 0; slot < this.getSlotCount(); slot++) {
                if (this.slottedVault.canAdd(slot)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !this.slottedVault.canAdd()) {
                return 0;
            }

            long inserted = 0;
            for (int slot = 0; slot < this.getSlotCount() && inserted < maxAmount; slot++) {
                inserted += this.insertIntoSlot(slot, resource, maxAmount - inserted, transaction);
            }
            return inserted;
        }

        @Override
        public boolean supportsExtraction() {
            if (!this.slottedVault.canRemove()) {
                return false;
            }
            for (int slot = 0; slot < this.getSlotCount(); slot++) {
                if (this.slottedVault.canRemove(slot)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !this.slottedVault.canRemove()) {
                return 0;
            }

            long extracted = 0;
            for (int slot = 0; slot < this.getSlotCount() && extracted < maxAmount; slot++) {
                extracted += this.extractFromSlot(slot, resource, maxAmount - extracted, transaction);
            }
            return extracted;
        }

        @Override
        public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
            return new Iterator<>() {
                private int slot;

                @Override
                public boolean hasNext() {
                    return this.slot < Slotted.this.getSlotCount();
                }

                @Override
                public @NotNull StorageView<ItemVariant> next() {
                    if (!this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return Slotted.this.getSlot(this.slot++);
                }
            };
        }

        @Override
        public int getSlotCount() {
            return this.slottedVault.slots();
        }

        @Override
        public @NotNull SingleSlotStorage<ItemVariant> getSlot(int slot) {
            Objects.checkIndex(slot, this.getSlotCount());
            return new Slot(slot);
        }

        private long insertIntoSlot(int slot, @NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            if (!this.slottedVault.canAdd() || !this.slottedVault.canAdd(slot)) {
                return 0;
            }

            ItemUnit incoming = this.unit(resource);
            if (!this.slottedVault.canAdd(slot, incoming)) {
                return 0;
            }

            ItemUnit current = this.slottedVault.get(slot);
            int amount;
            ItemUnit replacement;
            if (this.slottedVault.isEmpty(slot)) {
                amount = (int) Math.min(maxAmount, (long) incoming.maxAmount());
                if (amount == 0) {
                    return 0;
                }
                replacement = withAmount(incoming, amount);
            } else {
                if (!Objects.equals(current, incoming)) {
                    return 0;
                }

                int available = current.maxAmount() - current.amount();
                if (available <= 0) {
                    return 0;
                }
                amount = (int) Math.min(maxAmount, (long) available);
                replacement = current.copy();
                replacement.increment(amount);
            }

            this.replaceSlot(slot, replacement, transaction);
            return amount;
        }

        private long extractFromSlot(int slot, @NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            if (!this.slottedVault.canRemove() || !this.slottedVault.canRemove(slot)) {
                return 0;
            }

            ItemUnit current = this.slottedVault.get(slot);
            if (this.slottedVault.isEmpty(slot) || !Objects.equals(this.variant(current), resource)) {
                return 0;
            }

            int amount = (int) Math.min(maxAmount, (long) current.amount());
            if (amount == 0) {
                return 0;
            }

            ItemUnit replacement;
            if (amount == current.amount()) {
                replacement = this.slottedVault.empty();
            } else {
                replacement = current.copy();
                replacement.decrement(amount);
            }

            this.replaceSlot(slot, replacement, transaction);
            return amount;
        }

        private void replaceSlot(int slot, @NotNull ItemUnit replacement, @NotNull TransactionContext transaction) {
            ItemUnit previous = this.slottedVault.get(slot).copy();
            this.updateSnapshots(transaction);
            this.slottedVault.set(slot, replacement);
            this.addRollback(() -> this.slottedVault.set(slot, previous));
        }

        private static @NotNull ItemUnit withAmount(@NotNull ItemUnit value, int amount) {
            ItemUnit result = value.copy();
            int difference = amount - result.amount();
            if (difference > 0) {
                result.increment(difference);
            } else if (difference < 0) {
                result.decrement(-difference);
            }
            return result;
        }

        private final class Slot implements SingleSlotStorage<ItemVariant> {

            private final int slot;

            private Slot(int slot) {
                this.slot = slot;
            }

            @Override
            public boolean supportsInsertion() {
                return Slotted.this.slottedVault.canAdd() && Slotted.this.slottedVault.canAdd(this.slot);
            }

            @Override
            public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
                StoragePreconditions.notBlankNotNegative(resource, maxAmount);
                return Slotted.this.insertIntoSlot(this.slot, resource, maxAmount, transaction);
            }

            @Override
            public boolean supportsExtraction() {
                return Slotted.this.slottedVault.canRemove() && Slotted.this.slottedVault.canRemove(this.slot);
            }

            @Override
            public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
                StoragePreconditions.notBlankNotNegative(resource, maxAmount);
                return Slotted.this.extractFromSlot(this.slot, resource, maxAmount, transaction);
            }

            @Override
            public boolean isResourceBlank() {
                return this.getResource().isBlank();
            }

            @Override
            public @NotNull ItemVariant getResource() {
                return Slotted.this.variant(Slotted.this.slottedVault.get(this.slot));
            }

            @Override
            public long getAmount() {
                return Slotted.this.slottedVault.get(this.slot).amount();
            }

            @Override
            public long getCapacity() {
                return Slotted.this.slottedVault.get(this.slot).maxAmount();
            }
        }
    }
}
