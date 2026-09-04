package dev.lucaargolo.test.util;

import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TestChestVault<V extends Unit<?>> extends AbstractList<V> implements Vault<V> {

    private static final int CHEST_CAPACITY = 27;

    private final Class<V> type;
    private final BlockUnit block;
    private final DataBase<List<ItemUnit>> data;
    private final List<@Nullable ItemUnit> items;
    private final @NotNull V defaultValue;

    public TestChestVault(@NotNull Class<V> type, @NotNull V initial, @NotNull BlockUnit block, @NotNull DataBase<List<ItemUnit>> data) {
        this.type = type;
        this.block = block;
        this.data = data;
        List<ItemUnit> stored = block.getData(data);
        this.items = new ArrayList<>(Collections.nCopies(CHEST_CAPACITY, null));
        if (stored != null) {
            if (stored.size() > CHEST_CAPACITY) {
                throw new IllegalArgumentException("Stored chest contents exceed fixed size");
            }
            for (int index = 0; index < stored.size(); index++) {
                this.items.set(index, stored.get(index));
            }
        }
        this.defaultValue = initial;
    }

    @Override
    public @NotNull V defaultValue() {
        return this.defaultValue;
    }

    @Override
    public @NotNull V get(int index) {
        Objects.checkIndex(index, this.items.size());
        ItemUnit item = this.items.get(index);
        return item == null ? this.defaultValue : this.type.cast(item);
    }

    @Override
    public @NotNull V set(int index, @NotNull V unit) {
        Objects.checkIndex(index, this.items.size());
        if (!(unit instanceof ItemUnit item)) {
            throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
        }
        ItemUnit previous = this.items.set(index, item == this.defaultValue ? null : item);
        this.contentsChanged();
        return previous == null ? this.defaultValue : this.type.cast(previous);
    }

    @Override
    public boolean isFull() {
        return this.items.stream().allMatch(Objects::nonNull);
    }

    @Override
    public boolean add(@NotNull V unit) {
        if (this.isFull()) {
            return false;
        }
        if (!(unit instanceof ItemUnit item)) {
            throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
        }
        if (item == this.defaultValue) {
            return false;
        }
        for (int index = 0; index < this.items.size(); index++) {
            if (this.items.get(index) == null) {
                this.items.set(index, item);
                this.contentsChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public void setContents(@NotNull Collection<? extends V> contents) {
        if (contents.size() > this.items.size()) {
            throw new IllegalArgumentException("Vault contents exceed fixed size");
        }
        this.items.clear();
        this.items.addAll(Collections.nCopies(CHEST_CAPACITY, null));
        int index = 0;
        for (V unit : contents) {
            if (!(unit instanceof ItemUnit item)) {
                throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
            }
            this.items.set(index++, item == this.defaultValue ? null : item);
        }
        this.contentsChanged();
    }

    @Override
    public @NotNull V remove(int index) {
        Objects.checkIndex(index, this.items.size());
        ItemUnit previous = this.items.set(index, null);
        if (previous != null) {
            this.contentsChanged();
        }
        return previous == null ? this.defaultValue : this.type.cast(previous);
    }

    @Override
    public int size() {
        return CHEST_CAPACITY;
    }

    @Override
    public void contentsChanged() {
        List<ItemUnit> contents = new ArrayList<>();
        for (ItemUnit item : this.items) {
            if (item != null) {
                contents.add(item);
            }
        }
        this.block.setData(this.data, List.copyOf(contents));
    }
}
