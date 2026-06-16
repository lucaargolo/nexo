package dev.lucaargolo.nexo.neoforge;

import dev.lucaargolo.nexo.NexoMod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal ModContainer implementation for Nexo-discovered mods on NeoForge.
 * Extends NeoForge's abstract ModContainer to be insertable into ModList.
 */
public class NeoForgeNexoModContainer extends ModContainer {

    public NeoForgeNexoModContainer(NexoMod mod) {
        super(new ModInfo(mod));
    }

    @Override
    public net.neoforged.bus.api.IEventBus getEventBus() {
        return null;
    }

    /** Exposes the IModInfo so it can be added to ModList.sortedList. */
    public net.neoforged.neoforgespi.language.IModInfo getModInfo() {
        return modInfo;
    }

    private static class ModInfo implements IModInfo {
        private final NexoMod mod;

        ModInfo(NexoMod mod) { this.mod = mod; }

        @Override public String getModId() { return mod.modId(); }
        @Override public String getDisplayName() { return mod.name(); }
        @Override public String getDescription() { return mod.description(); }
        @Override public ArtifactVersion getVersion() { return new DefaultArtifactVersion(mod.version()); }
        @Override public List<? extends ModVersion> getDependencies() { return List.of(); }
        @Override public List<? extends ForgeFeature.Bound> getForgeFeatures() { return List.of(); }
        @Override public String getNamespace() { return mod.modId(); }
        @Override public Map<String, Object> getModProperties() { return Map.of(); }
        @Override public Optional<java.net.URL> getUpdateURL() { return Optional.empty(); }
        @Override public Optional<java.net.URL> getModURL() { return Optional.empty(); }
        @Override public Optional<String> getLogoFile() { return Optional.empty(); }
        @Override public boolean getLogoBlur() { return false; }
        @Override public IConfigurable getConfig() { return Config.INSTANCE; }
        @Override public IModFileInfo getOwningFile() { return new FileInfo(mod); }
        @Override public IModLanguageLoader getLoader() { return Loader.INSTANCE; }
    }

    private static class FileInfo implements IModFileInfo {
        private final NexoMod mod;
        FileInfo(NexoMod mod) { this.mod = mod; }
        @Override public List<IModInfo> getMods() { return List.of(); }
        @Override public List<LanguageSpec> requiredLanguageLoaders() { return List.of(); }
        @Override public boolean showAsResourcePack() { return false; }
        @Override public boolean showAsDataPack() { return false; }
        @Override public Map<String, Object> getFileProperties() { return Map.of(); }
        @Override public String getLicense() { return ""; }
        @Override public String moduleName() { return mod.modId(); }
        @Override public String versionString() { return "0.0.0"; }
        @Override public List<String> usesServices() { return List.of(); }
        @Override public net.neoforged.neoforgespi.locating.IModFile getFile() { return null; }
        @Override public IConfigurable getConfig() { return Config.INSTANCE; }
    }

    private static class Loader implements IModLanguageLoader {
        static final Loader INSTANCE = new Loader();
        @Override public String name() { return "nexo"; }
        @Override public String version() { return "1.0"; }
        @Override public ModContainer loadMod(IModInfo info, net.neoforged.neoforgespi.language.ModFileScanData scanData, java.lang.ModuleLayer layer) {
            return null;
        }
    }

    private static class Config implements IConfigurable {
        static final Config INSTANCE = new Config();
        @Override public <T> Optional<T> getConfigElement(String... keys) { return Optional.empty(); }
        @Override public List<? extends IConfigurable> getConfigList(String... keys) { return List.of(); }
    }
}
