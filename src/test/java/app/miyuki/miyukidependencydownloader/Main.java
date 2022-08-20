package app.miyuki.miyukidependencydownloader;

import java.nio.file.Paths;

public class Main {


    public static void main(String[] args)  {
        DependencyDownloader.builder()
                .repositories()
                .dependencies()
                .relocations()
                .build()
                .inject()
                .thenAccept(done -> {
                    if (done) {
                        System.out.println("Dependency injection done.");
                    } else {
                        System.out.println("Dependency injection failed.");
                    }
                });
    }

}
