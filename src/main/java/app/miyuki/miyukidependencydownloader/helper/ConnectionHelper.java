package app.miyuki.miyukidependencydownloader.helper;

import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionHelper {

    private ConnectionHelper() {

    }

    public static HttpURLConnection createConnection(@NotNull String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create connection to " + url, e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }


}
