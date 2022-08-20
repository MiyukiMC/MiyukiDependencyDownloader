package app.miyuki.miyukidependencydownloader.exception;

public class DependencyInjectNotSupportedException extends RuntimeException {

    public DependencyInjectNotSupportedException(String message) {
        super(message);
    }

    public DependencyInjectNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

}
