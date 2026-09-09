package dev.lucaargolo.test.util;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.SlottedVault;
import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.unit.Unit;
import dev.lucaargolo.nexo.api.unit.item.ItemUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class TestChestItemVault implements SlottedVault<ItemUnit> {

    private static final int SIZE = 27;

    private final @NotNull DataProvider<?> owner;
    private final @NotNull DataBase<ItemUnit[]> data;
    private final @NotNull ItemUnit empty;
    private final @NotNull ItemUnit[] array;

    public TestChestItemVault(@NotNull DataProvider<?> owner, @NotNull DataBase<ItemUnit[]> data, @NotNull ItemUnit empty) {
        this.owner = owner;
        this.data = data;
        this.empty = empty;
        ItemUnit[] array = owner.getData(data);
        if(array == null || array.length != SIZE) {
            array = new ItemUnit[27];
            Arrays.fill(array, empty);
        }
        this.array = array;
    }

    @Override
    public @NotNull ItemUnit empty() {
        return this.empty;
    }

    @Override
    public int slots() {
        return array.length;
    }

    @Override
    public @NotNull ItemUnit get(int slot) {
        return array[slot];
    }

    @Override
    public @NotNull ItemUnit set(int slot, @NotNull ItemUnit value) {
        ItemUnit previous = this.array[slot];
        this.array[slot] = value;
        this.owner.setData(this.data, this.array);
        return previous;
    }

    public static <U extends Unit<?>> @NotNull Vault<U> create(@NotNull Class<U> type, @NotNull DataProvider<?> owner, @NotNull DataBase<ItemUnit[]> data, @NotNull ItemUnit empty) {
        if(type != ItemUnit.class) {
            throw new IllegalArgumentException("Tried to create non ItemUnit TestChestItemVault");
        }
        return Nexo.<Vault<U>>type(Vault.class).cast(new TestChestItemVault(owner, data, empty));
    }

}
