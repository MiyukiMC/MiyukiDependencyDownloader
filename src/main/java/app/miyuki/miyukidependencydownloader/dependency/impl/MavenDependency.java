package app.miyuki.miyukidependencydownloader.dependency.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import lombok.Cleanup;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

@Getter
public class MavenDependency extends Dependency {

    private static final String MAVEN_FORMAT = "%s%s/%s/%s/%s-%s.jar";

    private static final String SNAPSHOT_MAVEN_FORMAT = "%s%s/%s/%s/%s-%s-%s-%s.jar";
    private static final String SNAPSHOT_MAVEN_DATA_FORMAT = "%s%s/%s/%s/maven-data.xml";

    private final String group;

    public MavenDependency(int priority, @NotNull String group, @NotNull String artifact, @NotNull String version) {
        super(priority, artifact, version);
        this.group = group.replace("#", ".");
    }


    @Override
    public @Nullable String getUrl(@Nullable Repository repository) {
        if (repository == null)
            return null;

        if (version.contains("SNAPSHOT")) {

            HttpURLConnection connection = null;
            try {
                val snapshotUrl = new URL(
                        String.format(
                                SNAPSHOT_MAVEN_DATA_FORMAT,
                                repository.getRepository(),
                                group.replace(".", "/"),
                                artifact,
                                version
                        )
                );
                connection = (HttpURLConnection) snapshotUrl.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                val documentBuilderFactory = DocumentBuilderFactory.newInstance();
                val documentBuilder = documentBuilderFactory.newDocumentBuilder();

                @Cleanup val inputStream = new BufferedInputStream(connection.getInputStream());

                val mavenDataDocument = documentBuilder.parse(inputStream);

                val snapshot = (Element) mavenDataDocument.getElementsByTagName("snapshot").item(0);
                val timestamp = snapshot.getElementsByTagName("timestamp").item(0).getTextContent();
                val buildNumber = snapshot.getElementsByTagName("buildNumber").item(0).getTextContent();

                return String.format(
                        SNAPSHOT_MAVEN_FORMAT,
                        repository.getRepository(),
                        group.replace(".", "/"),
                        artifact,
                        version,
                        artifact,
                        version.replace("-SNAPSHOT", ""),
                        timestamp,
                        buildNumber
                );
            } catch (IOException | ParserConfigurationException | SAXException exception) {
                return null;
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
        } else {
            return String.format(
                    MAVEN_FORMAT,
                    repository.getRepository(),
                    group.replace(".", "/"),
                    artifact,
                    version,
                    artifact,
                    version
            );
        }

    }

    @Override
    public @NotNull Path getDownloadPath(@NotNull Path defaultPath) {
        return defaultPath
                .resolve(group)
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".jar");
    }

    @Override
    public @NotNull Path getRelocationPath(@NotNull Path defaultPath) {
        return defaultPath
                .resolve(group)
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".relocated.jar");
    }

}
