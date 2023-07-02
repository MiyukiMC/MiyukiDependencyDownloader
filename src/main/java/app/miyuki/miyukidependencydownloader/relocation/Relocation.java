package app.miyuki.miyukidependencydownloader.relocation;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class Relocation {

    public static Relocation of(@NotNull String from, @NotNull String to, Set<String> inclusions, Set<String> exclusions) {
        return new Relocation(from, to, inclusions, exclusions);
    }

    public static Relocation of(@NotNull String from, @NotNull String to) {
        return new Relocation(from, to, Collections.emptySet(), Collections.emptySet());
    }

    private final @NotNull String from;
    private final @NotNull String to;

    private final Set<String> inclusions;

    private final Set<String> exclusions;

    public Relocation(@NotNull String from, @NotNull String to, Set<String> inclusions, Set<String> exclusions) {
        this.from = from;
        this.to = to;
        this.inclusions = inclusions;
        this.exclusions = exclusions;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Set<String> getInclusions() {
        return inclusions;
    }

    public Set<String> getExclusions() {
        return exclusions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relocation that = (Relocation) o;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(inclusions, that.inclusions) && Objects.equals(exclusions, that.exclusions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, inclusions, exclusions);
    }



}
