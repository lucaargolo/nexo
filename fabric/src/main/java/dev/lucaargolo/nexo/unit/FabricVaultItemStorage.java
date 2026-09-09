package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        if (vault instanceof Vault.Slotted<ItemUnit> slotted) {
            return InventoryStorage.of(new MinecraftVaultContainer(nexo, slotted), null);
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

}
