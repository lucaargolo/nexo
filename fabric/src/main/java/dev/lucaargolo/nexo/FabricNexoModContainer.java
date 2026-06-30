package dev.lucaargolo.nexo;

import dev.lucaargolo.nexo.api.Nexo;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FabricNexoModContainer implements ModContainer {

    private final Nexo.Mod mod;
    private final ModMetadata metadata;
    private final ModOrigin origin;

    public FabricNexoModContainer(Nexo.Mod mod) {
        this.mod = mod;
        this.metadata = new NexoModMetadata(mod);
        this.origin = new NexoModOrigin(mod);
    }

    @Override
    public ModMetadata getMetadata() {
        return metadata;
    }

    @Override
    public List<Path> getRootPaths() {
        return mod.path() != null ? List.of(mod.path()) : List.of();
    }

    @Override
    public ModOrigin getOrigin() {
        return origin;
    }

    @Override
    public Optional<ModContainer> getContainingMod() {
        return Optional.empty();
    }

    @Override
    public Collection<ModContainer> getContainedMods() {
        return List.of();
    }

    @Override
    public Path getRootPath() {
        return mod.path() != null ? mod.path() : Path.of(".");
    }

    @Override
    public Path getPath(String file) {
        Path root = getRootPath();
        return root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
    }

    private record NexoModMetadata(Nexo.Mod mod) implements ModMetadata {

        @Override
        public String getType() {
            return "nexo";
        }

        @Override
        public String getId() {
            return mod.value();
        }

        @Override
        public Collection<String> getProvides() {
            return List.of();
        }

        @Override
        public Version getVersion() {
            try {
                return Version.parse(mod.version());
            } catch (VersionParsingException e) {
                try {
                    return Version.parse("0.0.0");
                } catch (VersionParsingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public ModEnvironment getEnvironment() {
            return ModEnvironment.UNIVERSAL;
        }

        @Override
        public String getName() {
            return mod.name();
        }

        @Override
        public String getDescription() {
            return mod.description();
        }


        @Override
        public Collection<Person> getAuthors() {
            String[] authors = mod.authors();
            if (authors.length == 0) return List.of();
            Person[] result = new Person[authors.length];
            for (int i = 0; i < authors.length; i++) {
                String a = authors[i];
                result[i] = new Person() {
                    @Override
                    public String getName() {
                        return a;
                    }

                    @Override
                    public ContactInformation getContact() {
                        return ContactInformation.EMPTY;
                    }
                };
            }
            return List.of(result);
        }

        @Override
        public Collection<Person> getContributors() {
            return List.of();
        }

        @Override
        public ContactInformation getContact() {
            return ContactInformation.EMPTY;
        }

        @Override
        public Collection<String> getLicense() {
            return List.of();
        }

        @Override
        public Optional<String> getIconPath(int size) {
            return Optional.empty();
        }

        @Override
        public Collection<ModDependency> getDependencies() {
            return List.of();
        }

        @Override
        public boolean containsCustomValue(String key) {
            return false;
        }

        @Override
        public CustomValue getCustomValue(String key) {
            return null;
        }

        @Override
        public Map<String, CustomValue> getCustomValues() {
            return Map.of();
        }

        @Override
        public boolean containsCustomElement(String key) {
            return false;
        }

    }

    private record NexoModOrigin(Nexo.Mod mod) implements ModOrigin {
        @Override
        public Kind getKind() {
            return Kind.PATH;
        }

        @Override
        public List<Path> getPaths() {
            return mod.path() != null ? List.of(mod.path()) : List.of();
        }

        @Override
        public String getParentModId() {
            return null;
        }

        @Override
        public String getParentSubLocation() {
            return null;
        }
    }

}
