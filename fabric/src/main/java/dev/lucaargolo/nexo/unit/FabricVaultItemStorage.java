package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class FabricVaultItemStorage extends SnapshotParticipant<FabricVaultItemStorage.State> implements SlottedStorage<ItemVariant> {

    private final @NotNull NexoMinecraft nexo;
    private final @NotNull Vault.Slotted<ItemUnit> vault;
    private final @NotNull List<SingleSlotStorage<ItemVariant>> slots;

    private boolean changed;
    private long version;

    public FabricVaultItemStorage(@NotNull NexoMinecraft nexo, @NotNull Vault.Slotted<ItemUnit> vault) {
        this.nexo = nexo;
        this.vault = vault;
        List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>(vault.slots());
        for (int slot = 0; slot < vault.slots(); slot++) {
            slots.add(new Slot(slot));
        }
        this.slots = List.copyOf(slots);
    }

    @Override
    public boolean supportsInsertion() {
        return this.vault.canInsert();
    }

    @Override
    public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0 || !this.vault.canInsert()) {
            return 0;
        }
        return this.insert(this.unit(resource), maxAmount, transaction);
    }

    @Override
    public boolean supportsExtraction() {
        return this.vault.canExtract();
    }

    @Override
    public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0 || !this.vault.canExtract()) {
            return 0;
        }
        return this.extract(this.unit(resource), maxAmount, transaction);
    }

    @Override
    public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
        Iterator<SingleSlotStorage<ItemVariant>> slots = this.slots.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return slots.hasNext();
            }

            @Override
            public @NotNull StorageView<ItemVariant> next() {
                return slots.next();
            }
        };
    }

    @Override
    public long getVersion() {
        return this.version;
    }

    @Override
    public int getSlotCount() {
        return this.slots.size();
    }

    @Override
    public @NotNull SingleSlotStorage<ItemVariant> getSlot(int slot) {
        return this.slots.get(Objects.checkIndex(slot, this.slots.size()));
    }

    private long insert(@NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
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
                this.changed = true;
                inserted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return inserted;
    }

    private long insert(int slot, @NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
        long inserted = 0;
        while (inserted < maxAmount) {
            int requested = (int) Math.min(maxAmount - inserted, Integer.MAX_VALUE);
            int simulated = checkedAmount("insert", this.vault.insert(slot, value, requested, true), requested);
            if (simulated == 0) {
                break;
            }

            this.updateSnapshots(transaction);
            int transferred = checkedAmount("insert", this.vault.insert(slot, value, simulated, false), simulated);
            if (transferred > 0) {
                this.changed = true;
                inserted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return inserted;
    }

    private long extract(@NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
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
                this.changed = true;
                extracted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return extracted;
    }

    private long extract(int slot, @NotNull ItemUnit value, long maxAmount, @NotNull TransactionContext transaction) {
        long extracted = 0;
        while (extracted < maxAmount) {
            int requested = (int) Math.min(maxAmount - extracted, Integer.MAX_VALUE);
            int simulated = checkedAmount("extract", this.vault.extract(slot, value, requested, true), requested);
            if (simulated == 0) {
                break;
            }

            this.updateSnapshots(transaction);
            int transferred = checkedAmount("extract", this.vault.extract(slot, value, simulated, false), simulated);
            if (transferred > 0) {
                this.changed = true;
                extracted += transferred;
            }
            if (transferred < simulated) {
                break;
            }
        }
        return extracted;
    }

    private @NotNull ItemUnit unit(@NotNull ItemVariant variant) {
        return this.nexo.stackToUnit(variant.toStack());
    }

    private @NotNull ItemVariant variant(@NotNull ItemUnit unit) {
        if (!(unit instanceof MinecraftItemUnit minecraftUnit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return ItemVariant.of(minecraftUnit.get());
    }

    private long amount(@NotNull ItemUnit unit) {
        if (!(unit instanceof MinecraftItemUnit minecraftUnit)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " only accepts MinecraftItemUnit instances");
        }
        return minecraftUnit.get().getCount();
    }

    @Override
    protected @NotNull State createSnapshot() {
        return new State(this.vault, this.changed);
    }

    @Override
    protected void readSnapshot(@NotNull State state) {
        state.restore(this.vault);
        this.changed = state.changed();
    }

    @Override
    protected void onFinalCommit() {
        if (this.changed) {
            this.changed = false;
            this.version++;
            this.vault.changed();
        }
    }

    private static int checkedAmount(@NotNull String operation, int amount, int maximum) {
        if (amount < 0 || amount > maximum) {
            throw new IllegalStateException("Vault " + operation + " returned " + amount + " for a maximum of " + maximum);
        }
        return amount;
    }

    public record State(@NotNull List<ItemUnit> slots, boolean changed) {

        State(@NotNull Vault.Slotted<ItemUnit> vault, boolean changed) {
            this(IntStream.range(0, vault.slots()).mapToObj(slot -> vault.get(slot).copy()).toList(), changed);
        }

        void restore(@NotNull Vault.Slotted<ItemUnit> vault) {
            if (this.slots.size() != vault.slots()) {
                throw new IllegalStateException("Vault slot count changed during a Fabric transaction");
            }
            for (int slot = 0; slot < this.slots.size(); slot++) {
                vault.set(slot, this.slots.get(slot).copy());
            }
        }

    }

    private final class Slot implements SingleSlotStorage<ItemVariant> {

        private final int slot;

        private Slot(int slot) {
            this.slot = slot;
        }

        @Override
        public boolean supportsInsertion() {
            return FabricVaultItemStorage.this.vault.canInsert(this.slot);
        }

        @Override
        public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !this.supportsInsertion()) {
                return 0;
            }
            return FabricVaultItemStorage.this.insert(this.slot, FabricVaultItemStorage.this.unit(resource), maxAmount, transaction);
        }

        @Override
        public boolean supportsExtraction() {
            return FabricVaultItemStorage.this.vault.canExtract(this.slot);
        }

        @Override
        public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (maxAmount == 0 || !this.supportsExtraction()) {
                return 0;
            }
            return FabricVaultItemStorage.this.extract(this.slot, FabricVaultItemStorage.this.unit(resource), maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return this.getResource().isBlank();
        }

        @Override
        public @NotNull ItemVariant getResource() {
            return FabricVaultItemStorage.this.variant(FabricVaultItemStorage.this.vault.get(this.slot));
        }

        @Override
        public long getAmount() {
            return FabricVaultItemStorage.this.amount(FabricVaultItemStorage.this.vault.get(this.slot));
        }

        @Override
        public long getCapacity() {
            return Math.max(0, FabricVaultItemStorage.this.vault.maxStackAmount(this.slot));
        }
    }
}