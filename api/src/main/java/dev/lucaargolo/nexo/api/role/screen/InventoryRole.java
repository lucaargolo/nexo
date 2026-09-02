package dev.lucaargolo.nexo.api.role.screen;

import dev.lucaargolo.nexo.api.feature.VaultProvider;
import dev.lucaargolo.nexo.api.role.Role;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record InventoryRole(@NotNull Map<VaultProvider, Map<String, Config>> vaults) implements Role {

    public InventoryRole {
        Objects.requireNonNull(vaults, "vaults");
        Map<VaultProvider, Map<String, Config>> copy = new LinkedHashMap<>(vaults.size());
        for (Map.Entry<VaultProvider, Map<String, Config>> providerEntry : vaults.entrySet()) {
            VaultProvider provider = Objects.requireNonNull(providerEntry.getKey(), "Vault provider");
            Map<String, Config> configs = Objects.requireNonNull(providerEntry.getValue(), "Vault configurations");
            Map<String, Config> configCopy = new LinkedHashMap<>(configs.size());
            for (Map.Entry<String, Config> configEntry : configs.entrySet()) {
                String key = Objects.requireNonNull(configEntry.getKey(), "Vault key");
                Config config = Objects.requireNonNull(configEntry.getValue(), "Vault config");
                configCopy.put(key, config);
            }
            copy.put(provider, Collections.unmodifiableMap(configCopy));
        }
        vaults = Collections.unmodifiableMap(copy);
    }

    public record Config(int slotWidth, int slotHeight, @NotNull SlotDistribution distribution) {

        public Config {
            if (slotWidth <= 0 || slotHeight <= 0) {
                throw new IllegalArgumentException("Slot dimensions must be positive");
            }
            Objects.requireNonNull(distribution, "distribution");
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

}
