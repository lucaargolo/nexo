package dev.lucaargolo.test.util;

import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.block.BlockUnit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class TestChestVault<V extends Unit<?>> extends AbstractList<V> implements Vault<V> {

    private static final int CHEST_CAPACITY = 27;

    private final Class<V> type;
    private final BlockUnit block;
    private final DataBase<ItemUnit[]> data;
    private final @NotNull V defaultValue;
    private final @NotNull ItemUnit defaultItem;

    public TestChestVault(@NotNull Class<V> type, @NotNull V initial, @NotNull BlockUnit block, @NotNull DataBase<ItemUnit[]> data) {
        this.type = type;
        this.block = block;
        this.data = data;
        if (!(initial instanceof ItemUnit item)) {
            throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
        }
        this.defaultValue = initial;
        this.defaultItem = item;
        this.items();
    }

    @Override
    public @NotNull V empty() {
        return this.defaultValue;
    }

    @Override
    public @NotNull V get(int index) {
        ItemUnit[] items = this.items();
        Objects.checkIndex(index, items.length);
        ItemUnit item = items[index];
        return this.isEmptyItem(item) ? this.defaultValue : this.type.cast(item);
    }

    @Override
    public @NotNull V set(int index, @NotNull V unit) {
        ItemUnit[] items = this.items();
        Objects.checkIndex(index, items.length);
        if (!(unit instanceof ItemUnit item)) {
            throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
        }
        ItemUnit previous = items[index];
        items[index] = this.isEmptyItem(item) ? this.defaultItem : item;
        this.changed();
        return this.isEmptyItem(previous) ? this.defaultValue : this.type.cast(previous);
    }

    @Override
    public boolean isFull() {
        for (ItemUnit item : this.items()) {
            if (this.isEmptyItem(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(@NotNull V unit) {
        if (this.isFull()) {
            return false;
        }
        if (!(unit instanceof ItemUnit item)) {
            throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
        }
        if (this.isEmptyItem(item)) {
            return false;
        }
        ItemUnit[] items = this.items();
        for (int index = 0; index < items.length; index++) {
            if (this.isEmptyItem(items[index])) {
                items[index] = item;
                this.changed();
                return true;
            }
        }
        return false;
    }

    @Override
    public void setContents(@NotNull List<? extends V> contents) {
        ItemUnit[] items = this.items();
        if (contents.size() > items.length) {
            throw new IllegalArgumentException("Vault contents exceed fixed size");
        }
        Arrays.fill(items, this.defaultItem);
        int index = 0;
        for (V unit : contents) {
            if (!(unit instanceof ItemUnit item)) {
                throw new IllegalArgumentException("ChestVault only accepts ItemUnit instances");
            }
            items[index++] = this.isEmptyItem(item) ? this.defaultItem : item;
        }
        this.changed();
    }

    @Override
    public @NotNull V remove(int index) {
        ItemUnit[] items = this.items();
        Objects.checkIndex(index, items.length);
        ItemUnit previous = items[index];
        if (!this.isEmptyItem(previous)) {
            items[index] = this.defaultItem;
            this.changed();
        }
        return this.isEmptyItem(previous) ? this.defaultValue : this.type.cast(previous);
    }

    @Override
    public int size() {
        return CHEST_CAPACITY;
    }

    @Override
    public void changed() {
        this.block.setData(this.data, this.items());
    }

    private @NotNull ItemUnit[] items() {
        ItemUnit[] items = this.block.getData(this.data);
        if (items != null && items.length > CHEST_CAPACITY) {
            throw new IllegalArgumentException("Stored chest contents exceed fixed size");
        }
        if (items == null || items.length < CHEST_CAPACITY) {
            ItemUnit[] resized = new ItemUnit[CHEST_CAPACITY];
            Arrays.fill(resized, this.defaultItem);
            if (items != null) {
                System.arraycopy(items, 0, resized, 0, items.length);
            }
            this.block.setData(this.data, resized);
            return resized;
        }
        return items;
    }

    private boolean isEmptyItem(@NotNull ItemUnit item) {
        return item.feature() == this.defaultItem.feature();
    }
}
