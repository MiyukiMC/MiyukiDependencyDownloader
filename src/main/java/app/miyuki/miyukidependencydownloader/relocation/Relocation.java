package app.miyuki.miyukidependencydownloader.relocation;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Relocation {
    public static Relocation of(@NotNull String from, @NotNull String to) {
        return new Relocation(from, to);
    }

    public final @NotNull String from;
    public final @NotNull String to;

}
