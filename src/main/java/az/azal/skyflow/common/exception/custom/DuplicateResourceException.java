package az.azal.skyflow.common.exception.custom;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException byField(String resource, String field, Object value) {
        return new DuplicateResourceException(String.format("%s already exists with %s: '%s'", resource, field, value));
    }
}

