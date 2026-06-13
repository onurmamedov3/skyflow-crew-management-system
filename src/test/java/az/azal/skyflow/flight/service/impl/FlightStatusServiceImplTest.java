package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.model.FlightStatusHistory;
import az.azal.skyflow.flight.repository.FlightStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightStatusService Unit Tests — State Machine")
class FlightStatusServiceImplTest {

    @Mock private FlightStatusHistoryRepository historyRepository;

    @InjectMocks
    private FlightStatusServiceImpl flightStatusService;

    @Captor
    private ArgumentCaptor<FlightStatusHistory> historyCaptor;

    private Flight flight;

    @BeforeEach
    void setUp() {
        flight = new Flight();
        flight.setId(UUID.randomUUID());
        flight.setFlightNumber("J2-101");
    }

    @Nested
    @DisplayName("Valid Status Transitions")
    class ValidTransitions {

        @ParameterizedTest(name = "{0} → {1} transition should succeed")
        @CsvSource({
                "SCHEDULED, BOARDING",
                "SCHEDULED, DELAYED",
                "SCHEDULED, CANCELLED",
                "DELAYED, BOARDING",
                "DELAYED, CANCELLED",
                "BOARDING, IN_FLIGHT",
                "IN_FLIGHT, ARRIVED"
        })
        @DisplayName("Valid status transitions")
        void changeStatus_validTransitions_shouldSucceed(String from, String to) {
            flight.setStatus(FlightStatus.valueOf(from));

            assertDoesNotThrow(() ->
                    flightStatusService.changeFlightStatus(
                            flight, FlightStatus.valueOf(to), "SYSTEM", "Test reason")
            );

            assertThat(flight.getStatus()).isEqualTo(FlightStatus.valueOf(to));

            verify(historyRepository).save(historyCaptor.capture());

            FlightStatusHistory capturedHistory = historyCaptor.getValue();

            assertThat(capturedHistory.getOldStatus()).isEqualTo(FlightStatus.valueOf(from));
            assertThat(capturedHistory.getNewStatus()).isEqualTo(FlightStatus.valueOf(to));
            assertThat(capturedHistory.getChangedBy()).isEqualTo("SYSTEM");
            assertThat(capturedHistory.getChangeReason()).isEqualTo("Test reason");
            assertThat(capturedHistory.getFlight()).isEqualTo(flight);
        }
    }

    @Nested
    @DisplayName("Invalid Status Transitions")
    class InvalidTransitions {

        @ParameterizedTest(name = "{0} → {1} transition should be rejected")
        @CsvSource({
                "ARRIVED, SCHEDULED",
                "ARRIVED, BOARDING",
                "ARRIVED, IN_FLIGHT",
                "ARRIVED, DELAYED",
                "ARRIVED, CANCELLED",
                "CANCELLED, SCHEDULED",
                "CANCELLED, BOARDING",
                "CANCELLED, IN_FLIGHT",
                "CANCELLED, DELAYED",
                "CANCELLED, ARRIVED",
                "BOARDING, SCHEDULED",
                "BOARDING, DELAYED",
                "IN_FLIGHT, BOARDING",
                "IN_FLIGHT, DELAYED",
                "IN_FLIGHT, SCHEDULED"
        })
        @DisplayName("Invalid status transitions are rejected")
        void changeStatus_invalidTransitions_shouldThrow(String from, String to) {
            flight.setStatus(FlightStatus.valueOf(from));

            assertThatThrownBy(() ->
                    flightStatusService.changeFlightStatus(
                            flight, FlightStatus.valueOf(to), "SYSTEM", "Test")
            ).isInstanceOf(BusinessRuleViolationException.class);

            verify(historyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Transition to same status → should be rejected (SCHEDULED → SCHEDULED)")
        void changeStatus_toSameStatus_shouldThrow() {
            flight.setStatus(FlightStatus.SCHEDULED);

            assertThatThrownBy(() ->
                    flightStatusService.changeFlightStatus(
                            flight, FlightStatus.SCHEDULED, "SYSTEM", "No change")
            ).isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}