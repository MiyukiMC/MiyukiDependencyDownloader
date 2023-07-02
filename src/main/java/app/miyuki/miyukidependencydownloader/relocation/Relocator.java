package app.miyuki.miyukidependencydownloader.relocation;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import org.jetbrains.annotations.NotNull;

public interface Relocator extends AutoCloseable {

    boolean relocate(@NotNull Dependency dependency);

    boolean isSupported();

}
