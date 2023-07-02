package app.miyuki.miyukidependencydownloader.repository;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Repository {

    public static Repository of(@NotNull String url) {
        return new Repository(url);
    }

    private final String repository;

    public Repository(@NotNull String repository) {
        this.repository = repository;
    }

    public String getRepository() {
        return repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repository that = (Repository) o;
        return Objects.equals(repository, that.repository);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository);
    }

    @Override
    public String toString() {
        return "Repository{" +
                "repository='" + repository + '\'' +
                '}';
    }

}
