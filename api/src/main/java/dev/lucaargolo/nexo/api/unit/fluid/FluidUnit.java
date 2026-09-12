package dev.lucaargolo.nexo.api.unit.fluid;

import dev.lucaargolo.nexo.api.Nexo;
import dev.lucaargolo.nexo.api.feature.fluid.FluidBase;
import dev.lucaargolo.nexo.api.unit.Stackable;
import dev.lucaargolo.nexo.api.unit.Unit;
import org.jetbrains.annotations.NotNull;

public final class FluidUnit extends Unit<FluidBase> implements Stackable<FluidUnit> {

    private int divisor;
    private int amount;

    public FluidUnit(@NotNull Nexo nexo, @NotNull FluidBase feature, int divisor, int amount) {
        super(nexo, feature, feature.role());
        if (divisor <= 0) {
            throw new IllegalArgumentException("Fluid divisor must be positive");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative");
        }
        this.divisor = divisor;
        this.amount = amount;
    }

    @Override
    public int divisor() {
        return this.divisor;
    }

    @Override
    public int amount() {
        return this.amount;
    }

    @Override
    public void increment(int amount) {
        this.increment(1, amount);
    }

    @Override
    public void increment(int divisor, int amount) {
        this.change(divisor, amount, true);
    }

    @Override
    public void decrement(int amount) {
        this.decrement(1, amount);
    }

    @Override
    public void decrement(int divisor, int amount) {
        this.change(divisor, amount, false);
    }

    @Override
    public @NotNull FluidUnit copy() {
        return new FluidUnit(this.nexo, this.feature, this.divisor, this.amount);
    }

    @Override
    public int maxAmount() {
        return Integer.MAX_VALUE;
    }

    private void change(int divisor, int amount, boolean increment) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Fluid divisor must be positive");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative");
        }
        int greatestCommonDivisor = greatestCommonDivisor(this.divisor, divisor);
        long commonDivisor = (long) this.divisor / greatestCommonDivisor * divisor;
        if (commonDivisor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Fluid divisor exceeds integer range");
        }
        long currentAmount = (long) this.amount * (commonDivisor / this.divisor);
        long changedAmount = (long) amount * (commonDivisor / divisor);
        long result = increment ? currentAmount + changedAmount : currentAmount - changedAmount;
        if (result < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative");
        }
        if (result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Fluid amount exceeds integer range");
        }
        this.divisor = (int) commonDivisor;
        this.amount = (int) result;
    }

    private static int greatestCommonDivisor(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }

}
