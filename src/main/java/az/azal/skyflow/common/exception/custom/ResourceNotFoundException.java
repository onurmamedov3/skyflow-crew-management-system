package az.azal.skyflow.common.exception.custom;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException byId(String resource, Object id) {
        return new ResourceNotFoundException(String.format("%s not found with id: '%s'", resource, id));
    }

    public static ResourceNotFoundException byField(String resource, String field, Object value) {
        return new ResourceNotFoundException(String.format("%s not found with %s: '%s'", resource, field, value));
    }
}
