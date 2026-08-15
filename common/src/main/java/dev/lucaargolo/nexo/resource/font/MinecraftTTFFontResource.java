package dev.lucaargolo.nexo.resource.font;

import com.mojang.blaze3d.font.GlyphProvider;
import dev.lucaargolo.nexo.NexoMinecraft;
import dev.lucaargolo.nexo.api.resource.font.FontResource;
import dev.lucaargolo.nexo.api.util.Location;
import dev.lucaargolo.nexo.mixed.MinecraftFontManagerMixed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MinecraftTTFFontResource extends FontResource.TTF {

    private static final Map<Location, TTF> RESOURCE_MAP = new ConcurrentHashMap<>();
    private static final Map<Location, byte[]> REGISTERED_DATA = new ConcurrentHashMap<>();
    private static final Map<Location, GlyphProvider> REGISTERED_PROVIDERS = new ConcurrentHashMap<>();

    private static volatile FontManager activeFontManager;

    private final boolean resolved;

    private MinecraftTTFFontResource(Location location, boolean resolved, Supplier<byte[]> supplier) {
        super(location, supplier);
        this.resolved = resolved;
    }

    @Override
    public boolean resolved() {
        return data != null || resolved;
    }

    public static TTF lookup(NexoMinecraft nexo, Location location) {
        byte[] data = lookupFont(nexo, location);
        return RESOURCE_MAP.computeIfAbsent(location, l -> new MinecraftTTFFontResource(location, data != null, data != null ? () -> data : () -> lookupFont(nexo, location)));
    }

    private static byte @Nullable [] lookupFont(NexoMinecraft nexo, Location location) {
        byte[] data = nexo.loadResource(location);
        if (data != null) {
            return data;
        }
        NexoMinecraft.LOGGER.debug("Could not find ttf font for location {}", location);
        if (!location.path().contains("font/")) {
            data = lookupFont(nexo, location.withPathPrefix("font/"));
            if (data != null) {
                return data;
            }
        }
        if(!location.path().endsWith(".ttf")) {
            data = lookupFont(nexo, location.withPathSuffix(".ttf"));
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    @NotNull
    public static TTF register(@NotNull NexoMinecraft nexo, @NotNull Location location, byte[] data) {
        byte[] registeredData = data.clone();
        TTF ttf = new MinecraftTTFFontResource(location, true, registeredData::clone);
        REGISTERED_DATA.put(location, registeredData);
        RESOURCE_MAP.put(location, ttf);
        installInActiveFontManager(location);
        return ttf;
    }

    public static void reloadRegisteredFonts(@NotNull FontManager fontManager) {
        activeFontManager = fontManager;
        REGISTERED_PROVIDERS.clear();
        REGISTERED_DATA.forEach((location, data) -> {
            try {
                installFont(fontManager, location, data);
            } catch (Exception e) {
                NexoMinecraft.LOGGER.error("Failed to register TTF font {} during Minecraft font reload", location, e);
            }
        });
    }

    private static void installInActiveFontManager(Location location) {
        FontManager fontManager = activeFontManager;
        if (fontManager == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            installCurrentFont(fontManager, location);
        } else {
            minecraft.execute(() -> {
                try {
                    installCurrentFont(activeFontManager, location);
                } catch (Exception e) {
                    NexoMinecraft.LOGGER.error("Failed to register TTF font {} in Minecraft", location, e);
                }
            });
        }
    }

    private static void installCurrentFont(FontManager fontManager, Location location) {
        byte[] data = REGISTERED_DATA.get(location);
        if (fontManager == null || data == null) {
            return;
        }

        try {
            installFont(fontManager, location, data);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load TTF font " + location, e);
        }
    }

    private static void installFont(FontManager fontManager, Location location, byte[] data) throws IOException {
        MinecraftFontManagerMixed access = (MinecraftFontManagerMixed) fontManager;
        ResourceLocation fontId = NexoMinecraft.rl(location);
        GlyphProvider provider = loadProvider(fontId, data);
        FontSet fontSet = new FontSet(access.nexo$getTextureManager(), fontId);
        boolean installed = false;

        try {
            fontSet.reload(List.of(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS)), Set.of());

            FontSet previousFontSet = access.nexo$getFontSets().put(fontId, fontSet);
            GlyphProvider previousProvider = REGISTERED_PROVIDERS.put(location, provider);
            access.nexo$getProvidersToClose().add(provider);
            access.nexo$clearLastFontSetCache();
            installed = true;
            NexoMinecraft.LOGGER.debug("Registered TTF font {} with Minecraft font manager", location);

            if (previousFontSet != null) {
                previousFontSet.close();
            }
            if (previousProvider != null) {
                access.nexo$getProvidersToClose().remove(previousProvider);
                previousProvider.close();
            }
        } finally {
            if (!installed) {
                fontSet.close();
                provider.close();
            }
        }
    }

    private static GlyphProvider loadProvider(ResourceLocation fontId, byte[] data) throws IOException {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ResourceLocation fileId = fontId.withPrefix("font/");
        PackResources source = resources.listPacks().findFirst()
                .orElseThrow(() -> new IOException("Minecraft has no resource pack available to load " + fontId));
        Resource fontResource = new Resource(source, () -> new ByteArrayInputStream(data));
        ResourceManager inMemoryResources = new InMemoryResourceManager(resources, fileId, fontResource);
        TrueTypeGlyphProviderDefinition definition = new TrueTypeGlyphProviderDefinition(
                fontId,
                11.0F,
                1.0F,
                TrueTypeGlyphProviderDefinition.Shift.NONE,
                ""
        );
        GlyphProviderDefinition.Loader loader = definition.unpack().left().orElseThrow();
        return loader.load(inMemoryResources);
    }

    private record InMemoryResourceManager(
            ResourceManager delegate,
            ResourceLocation fontId,
            Resource fontResource
    ) implements ResourceManager {

        @Override
        public Set<String> getNamespaces() {
            return delegate.getNamespaces();
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            return fontId.equals(location) ? Optional.of(fontResource) : delegate.getResource(location);
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            return delegate.getResourceStack(location);
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
            return delegate.listResources(path, filter);
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
            return delegate.listResourceStacks(path, filter);
        }

        @Override
        public Stream<PackResources> listPacks() {
            return delegate.listPacks();
        }
    }

}
