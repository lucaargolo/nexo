package dev.lucaargolo.nexo.unit;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import dev.lucaargolo.nexo.unit.item.MinecraftItemUnit;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class FabricVaultStorage extends SnapshotParticipant<FabricVaultStorage.Snapshot> implements Storage<ItemVariant> {

    private final @NotNull NexoMinecraft<?, ?, ?, ?> nexo;
    private final @NotNull List<Vault<ItemUnit<?>>> vaults;

    private FabricVaultStorage(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull List<Vault<ItemUnit<?>>> vaults) {
        this.nexo = nexo;
        this.vaults = vaults;
    }

    public static @Nullable Storage<ItemVariant> create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Unit<?, ?> unit, @NotNull Map<String, ? extends Function<?, ? extends @Nullable Vault<ItemUnit<?>>>> vaultFactories) {
        List<Vault<ItemUnit<?>>> vaults = new ArrayList<>(vaultFactories.size());
        Class<Function<Unit<?, ?>, ? extends @Nullable Vault<ItemUnit<?>>>> type = Nexo.type(Function.class);
        for (Function<?, ? extends @Nullable Vault<ItemUnit<?>>> factory : vaultFactories.values()) {
            @Nullable Vault<ItemUnit<?>> vault = type.cast(factory).apply(unit);
            if (vault != null) {
                vaults.add(vault);
            }
        }
        return vaults.isEmpty() ? null : new FabricVaultStorage(nexo, List.copyOf(vaults));
    }

    @Override
    public long insert(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0) {
            return 0;
        }
        long remaining = maxAmount;
        long insertedTotal = 0;
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (!vault.canAdd()) {
                continue;
            }
            long inserted = this.insert(vault, resource, remaining, transaction);
            insertedTotal += inserted;
            remaining -= inserted;
            if (remaining == 0) {
                break;
            }
        }
        return insertedTotal;
    }

    @Override
    public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0) {
            return 0;
        }
        long remaining = maxAmount;
        long extractedTotal = 0;
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (!vault.canRemove()) {
                continue;
            }
            long extracted = this.extract(vault, null, resource, remaining, transaction, null);
            extractedTotal += extracted;
            remaining -= extracted;
            if (remaining == 0) {
                break;
            }
        }
        return extractedTotal;
    }

    @Override
    public boolean supportsInsertion() {
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (vault.canAdd()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supportsExtraction() {
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (vault.canRemove()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (vault instanceof FabricStorageVault transferVault) {
                Iterator<StorageView<ItemVariant>> iterator = transferVault.storage.nonEmptyIterator();
                while (iterator.hasNext()) {
                    views.add(new View(iterator.next()));
                }
            } else if (vault instanceof MinecraftItemVault minecraftVault) {
                for (int slot = 0; slot < minecraftVault.slotCount(); slot++) {
                    if (!minecraftVault.getItem(slot).isEmpty()) {
                        views.add(new View(vault, minecraftVault, slot));
                    }
                }
            } else {
                for (ItemUnit<?> item : vault) {
                    if (item instanceof MinecraftItemUnit<?> minecraftItem && !minecraftItem.get().isEmpty()) {
                        views.add(new View(vault, minecraftItem));
                    }
                }
            }
        }
        return Collections.unmodifiableList(views).iterator();
    }

    private long insert(@NotNull Vault<ItemUnit<?>> vault, @NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
        if (vault instanceof FabricStorageVault transferVault) {
            return transferVault.storage.insert(resource, maxAmount, transaction);
        }
        int amount = (int) Math.min(maxAmount, Integer.MAX_VALUE);
        ItemStack stack = resource.toStack(amount);
        if (vault instanceof MinecraftItemVault minecraftVault) {
            long inserted = 0;
            for (int slot = 0; slot < minecraftVault.slotCount() && inserted < amount; slot++) {
                ItemStack request = stack.copyWithCount(amount - (int) inserted);
                ItemStack simulated = minecraftVault.insertItem(slot, request, true, false);
                int possible = request.getCount() - simulated.getCount();
                if (possible <= 0) {
                    continue;
                }
                this.ensureSnapshot(transaction);
                ItemStack remaining = minecraftVault.insertItem(slot, request, false, false);
                inserted += request.getCount() - remaining.getCount();
            }
            return inserted;
        }
        this.ensureSnapshot(transaction);
        long stored = this.count(vault, resource);
        boolean changed = vault.add(this.nexo.stackToUnit(stack));
        return changed ? Math.clamp(this.count(vault, resource) - stored, 0, amount) : 0;
    }

    private long extract(
            @NotNull Vault<ItemUnit<?>> vault,
            @Nullable MinecraftItemUnit<?> target,
            @NotNull ItemVariant resource,
            long maxAmount,
            @NotNull TransactionContext transaction,
            @Nullable Integer physicalSlot
    ) {
        if (vault instanceof FabricStorageVault transferVault) {
            return transferVault.storage.extract(resource, maxAmount, transaction);
        }
        if (vault instanceof MinecraftItemVault minecraftVault) {
            long extracted = 0;
            int firstSlot = physicalSlot == null ? 0 : physicalSlot;
            int lastSlot = physicalSlot == null ? minecraftVault.slotCount() : physicalSlot + 1;
            for (int slot = firstSlot; slot < lastSlot && extracted < maxAmount; slot++) {
                ItemStack current = minecraftVault.getItem(slot);
                if (current.isEmpty() || !resource.matches(current)) {
                    continue;
                }
                int amount = (int) Math.min(Math.min(maxAmount - extracted, current.getCount()), Integer.MAX_VALUE);
                this.ensureSnapshot(transaction);
                ItemStack result = minecraftVault.extractItem(slot, amount, false, false);
                extracted += result.getCount();
            }
            return extracted;
        }
        long extractedTotal = 0;
        for (Iterator<ItemUnit<?>> iterator = vault.iterator(); iterator.hasNext() && extractedTotal < maxAmount;) {
            ItemUnit<?> item = iterator.next();
            if (!(item instanceof MinecraftItemUnit<?> minecraftItem) || !resource.matches(minecraftItem.get())) {
                continue;
            }
            if (target != null && minecraftItem != target && !ItemStack.isSameItemSameComponents(target.get(), minecraftItem.get())) {
                continue;
            }
            ItemStack source = minecraftItem.get();
            int amount = (int) Math.min(Math.min(maxAmount - extractedTotal, source.getCount()), Integer.MAX_VALUE);
            this.ensureSnapshot(transaction);
            iterator.remove();
            if (amount < source.getCount() && !vault.add(this.nexo.stackToUnit(source.copyWithCount(source.getCount() - amount)))) {
                throw new IllegalStateException("Vault rejected residual item during extraction");
            }
            extractedTotal += amount;
        }
        return extractedTotal;
    }

    private long count(@NotNull Vault<ItemUnit<?>> vault, @NotNull ItemVariant resource) {
        long count = 0;
        for (ItemUnit<?> item : vault) {
            if (item instanceof MinecraftItemUnit<?> minecraftItem && resource.matches(minecraftItem.get())) {
                count += minecraftItem.get().getCount();
            }
        }
        return count;
    }

    @Override
    protected @NotNull Snapshot createSnapshot() {
        List<@Nullable List<ItemStack>> snapshots = new ArrayList<>(this.vaults.size());
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            if (vault instanceof FabricStorageVault) {
                snapshots.add(null);
            } else if (vault instanceof MinecraftItemVault minecraftVault) {
                snapshots.add(minecraftVault.snapshot());
            } else {
                List<ItemStack> contents = new ArrayList<>();
                for (ItemUnit<?> item : vault) {
                    if (item instanceof MinecraftItemUnit<?> minecraftItem && !minecraftItem.get().isEmpty()) {
                        contents.add(minecraftItem.get().copy());
                    }
                }
                snapshots.add(List.copyOf(contents));
            }
        }
        return new Snapshot(List.copyOf(snapshots));
    }

    @Override
    protected void readSnapshot(@NotNull Snapshot snapshot) {
        for (int index = 0; index < this.vaults.size(); index++) {
            Vault<ItemUnit<?>> vault = this.vaults.get(index);
            @Nullable List<ItemStack> state = snapshot.vaults.get(index);
            if (state == null) {
                continue;
            }
            if (vault instanceof MinecraftItemVault minecraftVault) {
                minecraftVault.restore(state, false);
            } else {
                List<ItemUnit<?>> restored = new ArrayList<>(state.size());
                for (ItemStack stack : state) {
                    restored.add(this.nexo.stackToUnit(stack.copy()));
                }
                vault.setContents(restored);
            }
        }
    }

    @Override
    protected void onFinalCommit() {
        for (Vault<ItemUnit<?>> vault : this.vaults) {
            vault.contentsChanged();
        }
    }

    private void ensureSnapshot(@NotNull TransactionContext transaction) {
        super.updateSnapshots(transaction);
    }

    private final class View implements StorageView<ItemVariant> {

        private final @Nullable Vault<ItemUnit<?>> vault;
        private final @Nullable MinecraftItemUnit<?> item;
        private final @Nullable MinecraftItemVault physicalVault;
        private final int physicalSlot;
        private final @Nullable StorageView<ItemVariant> backingView;

        private View(@NotNull StorageView<ItemVariant> backingView) {
            this.vault = null;
            this.item = null;
            this.physicalVault = null;
            this.physicalSlot = -1;
            this.backingView = backingView;
        }

        private View(@NotNull Vault<ItemUnit<?>> vault, @NotNull MinecraftItemVault physicalVault, int physicalSlot) {
            this.vault = vault;
            this.item = null;
            this.physicalVault = physicalVault;
            this.physicalSlot = physicalSlot;
            this.backingView = null;
        }

        private View(@NotNull Vault<ItemUnit<?>> vault, @NotNull MinecraftItemUnit<?> item) {
            this.vault = vault;
            this.item = item;
            this.physicalVault = null;
            this.physicalSlot = -1;
            this.backingView = null;
        }

        @Override
        public long extract(@NotNull ItemVariant resource, long maxAmount, @NotNull TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (this.backingView != null) {
                return this.backingView.extract(resource, maxAmount, transaction);
            }
            if (this.vault == null) {
                return 0;
            }
            if (!this.vault.canRemove()) {
                return 0;
            }
            return FabricVaultStorage.this.extract(this.vault, this.item, resource, maxAmount, transaction, this.physicalSlot >= 0 ? this.physicalSlot : null);
        }

        @Override
        public boolean isResourceBlank() {
            if (this.backingView != null) {
                return this.backingView.isResourceBlank();
            }
            return this.currentStack().isEmpty();
        }

        @Override
        public @NotNull ItemVariant getResource() {
            if (this.backingView != null) {
                return this.backingView.getResource();
            }
            ItemStack stack = this.currentStack();
            return stack.isEmpty() ? ItemVariant.blank() : ItemVariant.of(stack);
        }

        @Override
        public long getAmount() {
            if (this.backingView != null) {
                return this.backingView.getAmount();
            }
            return this.currentStack().getCount();
        }

        @Override
        public long getCapacity() {
            if (this.backingView != null) {
                return this.backingView.getCapacity();
            }
            if (this.physicalVault != null) {
                return this.physicalVault.slotLimit(this.physicalSlot);
            }
            ItemStack stack = this.currentStack();
            return Math.max(stack.getCount(), stack.getMaxStackSize());
        }

        @Override
        public @NotNull StorageView<ItemVariant> getUnderlyingView() {
            return this.backingView != null ? this.backingView : this;
        }

        private @NotNull ItemStack currentStack() {
            if (this.physicalVault != null) {
                return this.physicalVault.getItem(this.physicalSlot);
            }
            return this.item == null ? ItemStack.EMPTY : this.item.get();
        }
    }

    protected record Snapshot(@NotNull List<@Nullable List<ItemStack>> vaults) {
    }
}
