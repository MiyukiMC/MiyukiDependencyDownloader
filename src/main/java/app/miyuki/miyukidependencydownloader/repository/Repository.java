package app.miyuki.miyukidependencydownloader.repository;

import lombok.Data;

@Data
public class Repository {

    public static Repository of(String url) {
        return new Repository(url);
    }

    private final String repository;


}
