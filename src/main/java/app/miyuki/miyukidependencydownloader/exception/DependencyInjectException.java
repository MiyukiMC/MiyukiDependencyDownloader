package app.miyuki.miyukidependencydownloader.exception;

public class DependencyInjectException extends RuntimeException {

    public DependencyInjectException(String message) {
        super(message);
    }

    public DependencyInjectException(String message, Throwable cause) {
        super(message, cause);
    }

}
