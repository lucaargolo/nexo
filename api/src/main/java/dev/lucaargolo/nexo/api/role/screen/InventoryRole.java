package dev.lucaargolo.nexo.api.role.screen;

import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.List;
import java.util.Objects;

public record InventoryRole(@NotNull List<Config> configs) implements Role {

    public record Config(@NotNull Target target, @NotNull String key, @NotNull SlotDistribution distribution, int slotWidth, int slotHeight) {

        public Config {
            if (slotWidth <= 0 || slotHeight <= 0) {
                throw new IllegalArgumentException("Slot dimensions must be positive");
            }
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(distribution, "distribution");
        }

        public Config(@NotNull Target target, @NotNull String key, @NotNull SlotDistribution distribution) {
            this(target, key, distribution, 18, 18);
        }

        public @NotNull Vector2i position(int index, int slotCount) {
            if (slotCount < 0) {
                throw new IllegalArgumentException("Slot count must not be negative");
            }
            if (index < 0 || index >= slotCount) {
                throw new IndexOutOfBoundsException("Slot index " + index + " outside slot count " + slotCount);
            }
            return Objects.requireNonNull(distribution.position(index, slotCount, slotWidth, slotHeight), "Slot distribution returned null");
        }

    }

    public enum Target {
        ENTITY,
        OWNER
    }

    @FunctionalInterface
    public interface SlotDistribution {

        @NotNull Vector2i position(int index, int slotCount, int slotWidth, int slotHeight);

        default @NotNull SlotDistribution margin(int x, int y) {
            return (index, slotCount, width, height) -> {
                Vector2i position = position(index, slotCount, width, height);
                return new Vector2i(position.x() + x, position.y() + y);
            };
        }

        static @NotNull SlotDistribution row() {
            return row(0);
        }

        static @NotNull SlotDistribution row(int padding) {
            if (padding < 0) {
                throw new IllegalArgumentException("Padding must not be negative");
            }
            return (index, slotCount, slotWidth, slotHeight) -> new Vector2i(index * (slotWidth + padding), 0);
        }

        static @NotNull SlotDistribution column() {
            return column(0);
        }

        static @NotNull SlotDistribution column(int padding) {
            if (padding < 0) {
                throw new IllegalArgumentException("Padding must not be negative");
            }
            return (index, slotCount, slotWidth, slotHeight) -> new Vector2i(0, index * (slotHeight + padding));
        }

        static @NotNull SlotDistribution grid(int columns) {
            return grid(columns, 0, 0);
        }

        static @NotNull SlotDistribution grid(int columns, int horizontalPadding, int verticalPadding) {
            if (columns <= 0) {
                throw new IllegalArgumentException("Grid columns must be positive");
            }
            if (horizontalPadding < 0 || verticalPadding < 0) {
                throw new IllegalArgumentException("Padding must not be negative");
            }
            return (index, slotCount, slotWidth, slotHeight) -> {
                int column = index % columns;
                int row = index / columns;
                return new Vector2i(
                        column * (slotWidth + horizontalPadding),
                        row * (slotHeight + verticalPadding)
                );
            };
        }

    }

}
