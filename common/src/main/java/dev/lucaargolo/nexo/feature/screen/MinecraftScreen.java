package dev.lucaargolo.nexo.feature.screen;

import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.feature.DataProvider;
import dev.lucaargolo.nexo.api.feature.data.DataBase;
import dev.lucaargolo.nexo.api.feature.screen.ScreenBase;
import dev.lucaargolo.nexo.api.feature.screen.ServerScreenBase;
import dev.lucaargolo.nexo.api.render.Graphics2D;
import dev.lucaargolo.nexo.api.render.Transform;
import dev.lucaargolo.nexo.api.feature.screen.widget.Widget;
import dev.lucaargolo.nexo.api.input.Input;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.input.GlfwGamepad;
import dev.lucaargolo.nexo.input.GlfwKeyConversions;
import dev.lucaargolo.nexo.render.DynamicMinecraftGraphics2D;
import dev.lucaargolo.nexo.unit.screen.MinecraftScreenUnit;
import dev.lucaargolo.nexo.util.Bijection;
import dev.lucaargolo.nexo.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MinecraftScreen extends ScreenBase {

    private static final Map<Location, ScreenBase> FEATURE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, Holder<ScreenBase>> HOLDER_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, Utils.Extender<?>> EXTENDERS = new ConcurrentHashMap<>();
    private static final Map<Location, Holder<MenuType<?>>> MENU_HOLDER_MAP = new ConcurrentHashMap<>();
    private static final Map<Screen, MinecraftScreenUnit<?>> UNITS = new ConcurrentHashMap<>();
    private static final Map<Screen, GlfwGamepad> GAMEPADS = new ConcurrentHashMap<>();
    private static final Map<Screen, double[]> MOUSE = new ConcurrentHashMap<>();

    private final @NotNull ScreenBase feature;
    private final @NotNull Screen screen;

    public static final ResourceKey<Registry<ScreenBase>> REGISTRY = ResourceKey.createRegistryKey(
            NexoMinecraft.rl(Location.of(NexoMinecraft.MOD_ID, "screen"))
    );

    public static final Bijection<ScreenBase, Holder<ScreenBase>> CONVERT = new Bijection<>() {
        @Override
        public Holder<ScreenBase> forward(ScreenBase screen) {
            return HOLDER_MAP.get(screen.location());
        }

        @Override
        public ScreenBase backward(Holder<ScreenBase> holder) {
            return FEATURE_MAP.get(NexoMinecraft.id(holder));
        }
    };

    private MinecraftScreen(@NotNull ScreenBase feature, @NotNull Screen screen) {
        super(feature.location(), feature::role);
        this.feature = feature;
        this.screen = screen;
    }

    public @NotNull Screen screen() {
        return this.screen;
    }

    @Override
    protected void onBuild(@NotNull dev.lucaargolo.nexo.api.unit.screen.ScreenUnit<?> unit) {
        this.feature.build(unit);
    }

    @Override
    public void render(@NotNull Graphics2D graphics, @NotNull dev.lucaargolo.nexo.api.unit.screen.ScreenUnit<?> unit) {
        this.feature.render(graphics, unit);
    }

    @Override
    public @NotNull Map<String, dev.lucaargolo.nexo.api.render.Material<?>> materials() {
        return this.feature.materials();
    }

    @Override
    public @NotNull java.util.List<@NotNull Tag> tags() {
        return this.feature.tags();
    }

    @Override
    public @NotNull java.util.List<@NotNull Widget> widgets() {
        return this.feature.widgets();
    }

    @Override
    public void addWidget(@NotNull Widget widget) {
        this.feature.addWidget(widget);
    }

    @Override
    public void removeWidget(@NotNull Widget widget) {
        this.feature.removeWidget(widget);
    }

    @Override
    public boolean onInputPressed(@NotNull dev.lucaargolo.nexo.api.unit.screen.ScreenUnit<?> unit, @NotNull Input input) {
        return this.feature.onInputPressed(unit, input);
    }

    @Override
    public boolean onInputReleased(@NotNull dev.lucaargolo.nexo.api.unit.screen.ScreenUnit<?> unit, @NotNull Input input) {
        return this.feature.onInputReleased(unit, input);
    }

    @Override
    public void onInputMove(@NotNull dev.lucaargolo.nexo.api.unit.screen.ScreenUnit<?> unit, @NotNull Input.Axis axis, float delta) {
        this.feature.onInputMove(unit, axis, delta);
    }

    @Override
    public @NotNull Transform transform(@NotNull Location location) {
        return this.feature.transform(location);
    }

    @Override
    public boolean resolved() {
        return this.feature.resolved();
    }

    @Override
    public boolean shaded() {
        return this.feature.shaded();
    }

    public static @Nullable ScreenBase lookup(@NotNull Location location) {
        return FEATURE_MAP.get(location);
    }

    public static @NotNull ScreenBase register(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase screen) {
        ScreenBase registered = FEATURE_MAP.putIfAbsent(screen.location(), screen);
        if (registered != null) {
            return registered;
        }
        craft(nexo, screen);
        Holder<ScreenBase> holder = nexo.getRegistryHandler().registerBuiltinFeature(REGISTRY, NexoMinecraft.rl(screen.location()), () -> screen);
        HOLDER_MAP.put(screen.location(), holder);
        if (screen instanceof ServerScreenBase<?> serverScreen) {
            registerMenu(nexo, serverScreen);
        }
        return screen;
    }

    public static @NotNull ScreenBase index(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull Holder<ScreenBase> holder) {
        Location location = NexoMinecraft.id(holder);
        HOLDER_MAP.put(location, holder);
        return FEATURE_MAP.computeIfAbsent(location, key -> holder.value());
    }

    public static @NotNull ScreenBase craft(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase screen) {
        if (screen instanceof ServerScreenBase<?>) {
            Utils.Extender<AbstractContainerScreen> extender = Utils.extend(nexo, AbstractContainerScreen.class);
            configureContainer(extender, nexo, screen);
            EXTENDERS.put(screen.location(), extender);
        } else {
            Utils.Extender<Screen> extender = Utils.extend(nexo, Screen.class);
            configureScreen(extender, nexo, screen);
            EXTENDERS.put(screen.location(), extender);
        }
        return screen;
    }

    public static @NotNull MinecraftScreen create(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @NotNull MinecraftScreenUnit<?> unit) {
        return create(nexo, feature, unit, null);
    }

    public static @NotNull MinecraftScreen create(
            @NotNull NexoMinecraft<?, ?, ?, ?> nexo,
            @NotNull ScreenBase feature,
            @NotNull MinecraftScreenUnit<?> unit,
            @Nullable AbstractContainerMenu menu
    ) {
        Utils.Extender<?> extender = EXTENDERS.get(feature.location());
        if (extender == null) {
            craft(nexo, feature);
            extender = EXTENDERS.get(feature.location());
        }
        Screen screen;
        if (feature instanceof ServerScreenBase<?>) {
            if (menu == null) {
                Inventory inventory = Minecraft.getInstance().player.getInventory();
                menu = menu(feature).create(0, inventory);
            }
            @SuppressWarnings("unchecked")
            Utils.Extender<AbstractContainerScreen> container = (Utils.Extender<AbstractContainerScreen>) extender;
            screen = container.instantiate(menu, Minecraft.getInstance().player.getInventory(), Component.translatable(feature.languageKey()));
        } else {
            @SuppressWarnings("unchecked")
            Utils.Extender<Screen> regular = (Utils.Extender<Screen>) extender;
            screen = regular.instantiate(Component.translatable(feature.languageKey()));
        }
        UNITS.put(screen, unit);
        GAMEPADS.put(screen, new GlfwGamepad(feature, unit));
        MOUSE.put(screen, new double[]{-1.0, -1.0});
        return new MinecraftScreen(feature, screen);
    }

    public static @NotNull MenuType<?> menu(@NotNull ScreenBase feature) {
        Holder<MenuType<?>> holder = MENU_HOLDER_MAP.get(feature.location());
        if (holder == null) {
            throw new IllegalStateException("No menu registered for server screen " + feature.location());
        }
        return holder.value();
    }

    private static void registerMenu(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ServerScreenBase<?> screen) {
        MenuType<?>[] type = new MenuType<?>[1];
        MenuType<AbstractContainerMenu> menu = nexo.getRegistryHandler().createMenuType((containerId, inventory, player) -> new AbstractContainerMenu(type[0], containerId) {
            {
                syncProvider(screen.provider());
            }

            @Override
            public @NotNull net.minecraft.world.item.ItemStack quickMoveStack(@NotNull net.minecraft.world.entity.player.Player player, int slot) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(@NotNull net.minecraft.world.entity.player.Player player) {
                return player == inventory.player;
            }

            @Override
            public void broadcastChanges() {
                super.broadcastChanges();
                syncProvider(screen.provider());
            }

            @Override
            public void broadcastFullState() {
                super.broadcastFullState();
                syncProvider(screen.provider());
            }
        });
        type[0] = menu;
        Holder<MenuType<?>> holder = nexo.getRegistryHandler().registerBuiltinFeature(BuiltInRegistries.MENU, NexoMinecraft.rl(screen.location()), () -> menu);
        MENU_HOLDER_MAP.put(screen.location(), holder);
        nexo.getRegistryHandler().registerMenuScreen(menu, (opened, inventory) -> {
            MinecraftScreenUnit<?> unit = new MinecraftScreenUnit<>(nexo, screen, screen.role(), opened);
            return unit.get().screen();
        });
    }

    private static void configureScreen(@NotNull Utils.Extender<Screen> extender, @NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature) {
        extender.override(Utils.At.AFTER_SUPER, "init", void.class, instance -> {
            feature.build(unit(instance));
            return null;
        });
        extender.override(Utils.At.REPLACE, "render", void.class, GuiGraphics.class, int.class, int.class, float.class, (instance, graphics, mouseX, mouseY, partialTick) -> {
            renderFeature(nexo, feature, unit(instance), graphics, mouseX, mouseY, partialTick);
            return null;
        });
        extender.override(Utils.At.AFTER_SUPER, "tick", void.class, instance -> {
            GAMEPADS.get(instance).poll();
            return null;
        });
        configureInput(extender, feature);
        extender.override(Utils.At.AFTER_SUPER, "removed", void.class, instance -> {
            unregister(instance);
            return null;
        });
    }

    private static void configureContainer(@NotNull Utils.Extender<AbstractContainerScreen> extender, @NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature) {
        extender.override(Utils.At.REPLACE, "init", void.class, instance -> {
            feature.build(unit(instance));
            return null;
        });
        extender.override(Utils.At.REPLACE, "render", void.class, GuiGraphics.class, int.class, int.class, float.class, (instance, graphics, mouseX, mouseY, partialTick) -> {
            renderFeature(nexo, feature, unit(instance), graphics, mouseX, mouseY, partialTick);
            return null;
        });
        extender.override(Utils.At.REPLACE, "renderBg", void.class, GuiGraphics.class, float.class, int.class, int.class, (instance, graphics, partialTick, mouseX, mouseY) -> null);
        extender.override(Utils.At.AFTER_SUPER, "containerTick", void.class, instance -> {
            GAMEPADS.get(instance).poll();
            return null;
        });
        configureInput(extender, feature);
        extender.override(Utils.At.AFTER_SUPER, "removed", void.class, instance -> {
            unregister(instance);
            return null;
        });
    }

    private static <S extends Screen> void configureInput(@NotNull Utils.Extender<S> extender, @NotNull ScreenBase feature) {
        extender.override(Utils.At.REPLACE, "keyPressed", boolean.class, int.class, int.class, int.class, (instance, keyCode, scanCode, modifiers) -> pressed(instance, feature, Input.keyboard(GlfwKeyConversions.key(keyCode))));
        extender.override(Utils.At.REPLACE, "keyReleased", boolean.class, int.class, int.class, int.class, (instance, keyCode, scanCode, modifiers) -> released(instance, feature, Input.keyboard(GlfwKeyConversions.key(keyCode))));
        extender.override(Utils.At.REPLACE, "mouseClicked", boolean.class, double.class, double.class, int.class, (instance, mouseX, mouseY, button) -> pressed(instance, feature, Input.mouse(GlfwKeyConversions.mouse(button))));
        extender.override(Utils.At.REPLACE, "mouseReleased", boolean.class, double.class, double.class, int.class, (instance, mouseX, mouseY, button) -> released(instance, feature, Input.mouse(GlfwKeyConversions.mouse(button))));
        extender.override(Utils.At.REPLACE, "mouseDragged", boolean.class, double.class, double.class, int.class, double.class, double.class, (instance, mouseX, mouseY, button, dragX, dragY) -> {
            MinecraftScreenUnit<?> unit = unit(instance);
            if (unit == null) return false;
            feature.onInputMove(unit, Input.Axis.MOUSE_X, dragX.floatValue());
            feature.onInputMove(unit, Input.Axis.MOUSE_Y, dragY.floatValue());
            return true;
        });
        extender.override(Utils.At.REPLACE, "mouseScrolled", boolean.class, double.class, double.class, double.class, double.class, (instance, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
            MinecraftScreenUnit<?> unit = unit(instance);
            if (unit == null) return false;
            feature.onInputMove(unit, Input.Axis.SCROLL, verticalAmount.floatValue());
            return true;
        });
        extender.override(Utils.At.REPLACE, "mouseMoved", void.class, double.class, double.class, (instance, mouseX, mouseY) -> {
            move(instance, feature, mouseX, mouseY);
            return null;
        });
    }

    private static boolean pressed(@NotNull Screen screen, @NotNull ScreenBase feature, @NotNull Input input) {
        MinecraftScreenUnit<?> unit = unit(screen);
        return unit != null && feature.onInputPressed(unit, input);
    }

    private static boolean released(@NotNull Screen screen, @NotNull ScreenBase feature, @NotNull Input input) {
        MinecraftScreenUnit<?> unit = unit(screen);
        return unit != null && feature.onInputReleased(unit, input);
    }

    private static void move(@NotNull Screen screen, @NotNull ScreenBase feature, double mouseX, double mouseY) {
        double[] previous = MOUSE.get(screen);
        if (previous != null && previous[0] >= 0.0 && previous[1] >= 0.0) {
            MinecraftScreenUnit<?> unit = unit(screen);
            if (unit != null) {
                feature.onInputMove(unit, Input.Axis.MOUSE_X, (float) (mouseX - previous[0]));
                feature.onInputMove(unit, Input.Axis.MOUSE_Y, (float) (mouseY - previous[1]));
            }
        }
        if (previous != null) {
            previous[0] = mouseX;
            previous[1] = mouseY;
        }
    }

    private static void renderFeature(@NotNull NexoMinecraft<?, ?, ?, ?> nexo, @NotNull ScreenBase feature, @NotNull MinecraftScreenUnit<?> unit, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        unit.setMouse(mouseX, mouseY);
        DynamicMinecraftGraphics2D g = new DynamicMinecraftGraphics2D(nexo, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        try {
            feature.render(g, unit);
        } catch (Throwable t) {
            NexoMinecraft.LOGGER.error("Failed to render Nexo screen {}", feature.location(), t);
            throw t;
        } finally {
            g.finish();
        }
    }

    private static void unregister(@NotNull Screen screen) {
        UNITS.remove(screen);
        GAMEPADS.remove(screen);
        MOUSE.remove(screen);
    }

    private static @Nullable MinecraftScreenUnit<?> unit(@NotNull Screen screen) {
        return UNITS.get(screen);
    }

    private static void syncProvider(@NotNull DataProvider provider) {
        for (DataBase<?> data : provider.data()) {
            syncProviderData(provider, data);
        }
    }

    private static <D> void syncProviderData(@NotNull DataProvider provider, @NotNull DataBase<D> data) {
        provider.setData(data, provider.getData(data));
    }
}
