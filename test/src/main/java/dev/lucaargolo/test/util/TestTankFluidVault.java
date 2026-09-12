package dev.lucaargolo.test.util;

import dev.lucaargolo.nexo.api.feature.Vault;
import dev.lucaargolo.nexo.api.unit.fluid.FluidUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class TestTankFluidVault implements Vault.Slotted<FluidUnit> {

    private final @NotNull FluidUnit empty;
    private final int capacity;
    private @Nullable FluidUnit fluid;

    public TestTankFluidVault(@NotNull FluidUnit prototype, int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Test tank maximum stack amount cannot be negative");
        }
        this.empty = prototype.copy();
        this.capacity = capacity;
        this.empty.decrement(this.empty.divisor(), this.empty.amount());
    }

    @Override
    public @NotNull FluidUnit empty() {
        return this.empty.copy();
    }

    @Override
    public int maxStackAmount() {
        return this.capacity;
    }

    @Override
    public int slots() {
        return 1;
    }

    @Override
    public @NotNull FluidUnit get(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.fluid == null ? this.empty() : this.fluid.copy();
    }

    @Override
    public @NotNull FluidUnit set(int slot, @NotNull FluidUnit value) {
        Objects.checkIndex(slot, this.slots());
        FluidUnit previous = this.fluid;
        if (value.amount() == 0) {
            this.fluid = null;
        } else {
            if (!this.supports(value) || value.amount() > this.maxStackAmount()) {
                throw new IllegalArgumentException("Test tank vault rejected fluid unit");
            }
            this.fluid = value.copy();
        }
        return previous == null ? this.empty() : previous.copy();
    }

    @Override
    public boolean canInsert() {
        return this.fluid == null || this.fluid.amount() < this.maxStackAmount();
    }

    @Override
    public boolean canInsert(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.canInsert();
    }

    @Override
    public boolean canInsert(@NotNull FluidUnit value) {
        if (!this.supports(value)) {
            return false;
        }
        return this.fluid == null || this.matches(this.fluid, value) && this.canInsert();
    }

    @Override
    public boolean canInsert(int slot, @NotNull FluidUnit value) {
        Objects.checkIndex(slot, this.slots());
        return this.canInsert(value);
    }

    @Override
    public int maxStackAmount(int slot, @NotNull FluidUnit value) {
        Objects.checkIndex(slot, this.slots());
        return this.supports(value) ? this.maxStackAmount(slot) : 0;
    }

    @Override
    public boolean canExtract() {
        return this.fluid != null;
    }

    @Override
    public boolean canExtract(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.canExtract();
    }

    @Override
    public int insert(@NotNull FluidUnit value, int max, boolean simulate) {
        return this.insert(0, value, max, simulate);
    }

    @Override
    public int insert(int slot, @NotNull FluidUnit value, int max, boolean simulate) {
        Objects.checkIndex(slot, this.slots());
        if (max < 0) {
            throw new IllegalArgumentException("Insertion amount cannot be negative");
        }
        if (max == 0 || !this.canInsert(slot, value)) {
            return 0;
        }

        int amount = Math.min(max, this.maxStackAmount() - (this.fluid == null ? 0 : this.fluid.amount()));
        if (amount <= 0) {
            return 0;
        }
        if (!simulate) {
            if (this.fluid == null) {
                FluidUnit result = value.copy();
                int difference = amount - result.amount();
                if (difference > 0) {
                    result.increment(result.divisor(), difference);
                } else if (difference < 0) {
                    result.decrement(result.divisor(), -difference);
                }
                this.fluid = result;
            } else {
                this.fluid.increment(this.fluid.divisor(), amount);
            }
        }
        return amount;
    }

    @Override
    public int extract(@NotNull FluidUnit value, int max, boolean simulate) {
        return this.extract(0, value, max, simulate);
    }

    @Override
    public int extract(int slot, @NotNull FluidUnit value, int max, boolean simulate) {
        Objects.checkIndex(slot, this.slots());
        if (max < 0) {
            throw new IllegalArgumentException("Extraction amount cannot be negative");
        }
        if (max == 0 || this.fluid == null || !this.supports(value) || !this.matches(this.fluid, value)) {
            return 0;
        }

        int amount = Math.min(max, this.fluid.amount());
        if (!simulate) {
            if (amount == this.fluid.amount()) {
                this.fluid = null;
            } else {
                this.fluid.decrement(this.fluid.divisor(), amount);
            }
        }
        return amount;
    }

    @Override
    public @NotNull FluidUnit clear(int slot) {
        Objects.checkIndex(slot, this.slots());
        FluidUnit previous = this.fluid;
        this.fluid = null;
        return previous == null ? this.empty() : previous.copy();
    }

    @Override
    public void clear() {
        this.fluid = null;
    }

    @Override
    public boolean isEmpty(int slot) {
        Objects.checkIndex(slot, this.slots());
        return this.fluid == null;
    }

    private boolean supports(@NotNull FluidUnit fluid) {
        return fluid.divisor() == this.empty.divisor();
    }

    private boolean matches(@NotNull FluidUnit first, @NotNull FluidUnit second) {
        return first.feature() == second.feature();
    }

}
