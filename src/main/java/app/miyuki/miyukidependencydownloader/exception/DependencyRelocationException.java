package app.miyuki.miyukidependencydownloader.exception;

public class DependencyRelocationException extends RuntimeException {

    public DependencyRelocationException(String message) {
        super(message);
    }

    public DependencyRelocationException(String message, Throwable cause) {
        super(message, cause);
    }

}
