package app.miyuki.miyukidependencydownloader.dependency.impl;

import app.miyuki.miyukidependencydownloader.dependency.Dependency;
import app.miyuki.miyukidependencydownloader.helper.ConnectionHelper;
import app.miyuki.miyukidependencydownloader.helper.DocumentParserHelper;
import app.miyuki.miyukidependencydownloader.relocation.Relocation;
import app.miyuki.miyukidependencydownloader.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MavenDependency extends Dependency {

    private static final String MAVEN_FORMAT = "%s%s/%s/%s/%s-%s.jar";

    private static final String SNAPSHOT_MAVEN_FORMAT = "%s%s/%s/%s/%s-%s-%s-%s.jar";
    private static final String SNAPSHOT_MAVEN_DATA_FORMAT = "%s%s/%s/%s/maven-data.xml";

    private final String group;

    public MavenDependency(int priority, @NotNull String group, @NotNull String artifact, @NotNull String version, List<@NotNull Relocation> relocations) {
        super(priority, artifact, version, relocations);
        this.group = group.replace("#", ".");
    }


    public String getGroup() {
        return group;
    }

    @Override
    public @Nullable String getUrl(@Nullable Repository repository) {
        if (repository == null)
            return null;

        if (version.contains("SNAPSHOT")) {
            try {
                String snapshotUrl = String.format(
                        SNAPSHOT_MAVEN_DATA_FORMAT,
                        repository.getRepository(),
                        group.replace(".", "/"),
                        artifact,
                        version
                );

                HttpURLConnection connection = ConnectionHelper.createConnection(snapshotUrl);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {

                    Document mavenDataDocument = DocumentParserHelper.parseDocument(inputStream);

                    Element snapshot = (Element) mavenDataDocument.getElementsByTagName("snapshot").item(0);
                    String timestamp = snapshot.getElementsByTagName("timestamp").item(0).getTextContent();
                    String buildNumber = snapshot.getElementsByTagName("buildNumber").item(0).getTextContent();

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
                }
            } catch (IOException | ParserConfigurationException | SAXException exception) {
                return null;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MavenDependency that = (MavenDependency) o;
        return Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), group);
    }

    @Override
    public String toString() {
        return "MavenDependency{" +
                "group='" + group + '\'' +
                ", priority=" + priority +
                ", artifact='" + artifact + '\'' +
                ", version='" + version + '\'' +
                ", relocations=" + relocations +
                '}';
    }

}
