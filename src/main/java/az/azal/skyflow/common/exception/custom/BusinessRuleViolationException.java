package az.azal.skyflow.common.exception.custom;

public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public static BusinessRuleViolationException invalidStatusTransition(String from, String to) {
        return new BusinessRuleViolationException(
                String.format("Cannot transition from %s to %s", from, to)
        );
    }

    public static BusinessRuleViolationException crewNotAvailable(String employeeId, String status) {
        return new BusinessRuleViolationException(
                String.format("Crew member %s is not available (status: %s)", employeeId, status)
        );
    }

    public static BusinessRuleViolationException timeConflict(String employeeId, String conflictingFlight) {
        return new BusinessRuleViolationException(
                String.format("Crew member %s has a conflicting flight: %s", employeeId, conflictingFlight)
        );
    }

    public static BusinessRuleViolationException restPeriodViolation(String employeeId, String earliestTime) {
        return new BusinessRuleViolationException(
                String.format("Crew member %s needs rest until %s", employeeId, earliestTime)
        );
    }

    public static BusinessRuleViolationException aircraftConflict(String registrationNumber) {
        return new BusinessRuleViolationException(
                String.format("Aircraft %s has a conflicting flight at the requested time", registrationNumber)
        );
    }
}