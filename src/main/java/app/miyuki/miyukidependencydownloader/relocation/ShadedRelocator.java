package app.miyuki.miyukidependencydownloader.relocation;

import app.miyuki.miyukidependencydownloader.DependencyDownloader;
import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.exception.DependencyRelocationException;
import me.lucko.jarrelocator.JarRelocator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class ShadedRelocator implements Relocator {

    private static final BooleanSupplier SUPPORTED = () -> {
        try {
            Class.forName("me.lucko.jarrelocator.JarRelocator");
            Class.forName("org.objectweb.asm.commons.Remapper");
            Class.forName("org.objectweb.asm.ClassReader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    };

    private final DependencyDownloader dependencyDownloader;


    public ShadedRelocator(@NotNull DependencyDownloader dependencyDownloader) throws DependencyRelocationException {
        this.dependencyDownloader = dependencyDownloader;
    }

    public boolean relocate(@NotNull Dependency dependency) {
        try {
            Path downloadPath = dependency.getDownloadPath(dependencyDownloader.getDefaultPath());
            if (!Files.exists(downloadPath))
                return false;

            Path relocationPath = dependency.getRelocationPath(dependencyDownloader.getDefaultPath());
            if (Files.exists(relocationPath))
                return true;

            List<Relocation> relocations = new ArrayList<>(dependency.getRelocations());
            relocations.addAll(dependencyDownloader.getRelocations());
            if (relocations.isEmpty())
                return false;

            List<me.lucko.jarrelocator.Relocation> luckoRelocation = relocations
                    .stream()
                    .map(relocation -> new me.lucko.jarrelocator.Relocation(
                                    relocation.getFrom().replace("#", "."),
                                    relocation.getTo().replace("#", "."),
                                    relocation.getInclusions().stream().map(it -> it.replace("#", ".")).collect(Collectors.toSet()),
                                    relocation.getExclusions().stream().map(it -> it.replace("#", ".")).collect(Collectors.toSet())
                            )
                    ).collect(Collectors.toList());

            new JarRelocator(
                    downloadPath.toFile(),
                    relocationPath.toFile(),
                    luckoRelocation
            ).run();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isSupported() {
        return SUPPORTED.getAsBoolean();
    }

    @Override
    public void close() {

    }
}
