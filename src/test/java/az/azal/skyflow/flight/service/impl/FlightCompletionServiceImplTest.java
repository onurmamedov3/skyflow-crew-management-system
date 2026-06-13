package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.aircraft.model.Aircraft;
import az.azal.skyflow.aircraft.model.AircraftStatus;
import az.azal.skyflow.aircraft.repository.AircraftRepository;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.model.AssignmentStatus;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.CrewRole;
import az.azal.skyflow.crew.model.FlightCrewAssignment;
import az.azal.skyflow.crew.repository.FlightCrewAssignmentRepository;
import az.azal.skyflow.crew.service.CrewService;
import az.azal.skyflow.flight.dto.FlightResponse;
import az.azal.skyflow.flight.event.FlightCompletedEvent;
import az.azal.skyflow.flight.mapper.FlightMapper;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.repository.FlightRepository;
import az.azal.skyflow.flight.service.FlightStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightCompletionService Unit Tests")
class FlightCompletionServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @Mock private FlightStatusService flightStatusService;
    @Mock private AircraftRepository aircraftRepository;
    @Mock private FlightCrewAssignmentRepository assignmentRepository;
    @Mock private CrewService crewService;
    @Mock private FlightMapper flightMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FlightCompletionServiceImpl flightCompletionService;

    @Captor private ArgumentCaptor<FlightCompletedEvent> eventCaptor;
    @Captor private ArgumentCaptor<Aircraft> aircraftCaptor;

    private Flight flight;
    private Aircraft aircraft;
    private UUID flightId;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();

        aircraft = new Aircraft();
        aircraft.setId(UUID.randomUUID());
        aircraft.setRegistrationNumber("4K-AZ01");
        aircraft.setStatus(AircraftStatus.ACTIVE);

        flight = new Flight();
        flight.setId(flightId);
        flight.setFlightNumber("J2-101");
        flight.setDepartureTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        flight.setArrivalTime(LocalDateTime.of(2026, 7, 15, 13, 0));
        flight.setStatus(FlightStatus.IN_FLIGHT);
        flight.setAircraft(aircraft);
    }

    @Nested
    @DisplayName("Successful Flight Completion")
    class SuccessfulCompletion {

        @Test
        @DisplayName("completeFlight → status ARRIVED, aircraft MAINTENANCE, crew updated, event published")
        void completeFlight_shouldPerformAllSideEffects() {

            CrewMember captain = new CrewMember();

            captain.setId(UUID.randomUUID());
            captain.setEmployeeId("CPT-001");

            CrewMember firstOfficer = new CrewMember();

            firstOfficer.setId(UUID.randomUUID());
            firstOfficer.setEmployeeId("FO-001");

            FlightCrewAssignment assignment1 = new FlightCrewAssignment();

            assignment1.setCrewMember(captain);
            assignment1.setFlight(flight);
            assignment1.setRoleOnFlight(CrewRole.CAPTAIN);

            FlightCrewAssignment assignment2 = new FlightCrewAssignment();

            assignment2.setCrewMember(firstOfficer);
            assignment2.setFlight(flight);
            assignment2.setRoleOnFlight(CrewRole.FIRST_OFFICER);

            FlightResponse flightResponse = new FlightResponse(
                    flightId, "J2-101", aircraft.getId(), "GYD", "IST",
                    FlightStatus.ARRIVED, flight.getDepartureTime(), flight.getArrivalTime(),
                    null, null, null, null);

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(assignmentRepository.findByFlightAndAssignmentStatus(flight, AssignmentStatus.ASSIGNED))
                    .thenReturn(List.of(assignment1, assignment2));
            when(flightMapper.toResponse(flight)).thenReturn(flightResponse);

            FlightResponse result = flightCompletionService.completeFlight(flightId, "admin");

            verify(flightStatusService).changeFlightStatus(
                    eq(flight), eq(FlightStatus.ARRIVED), eq("admin"), eq("Flight completed"));

            assertThat(flight.getActualArrivalTime()).isNotNull();

            verify(aircraftRepository).save(aircraftCaptor.capture());

            assertThat(aircraftCaptor.getValue().getStatus()).isEqualTo(AircraftStatus.MAINTENANCE);

            verify(crewService).recordFlightCompletion(captain, flight);
            verify(crewService).recordFlightCompletion(firstOfficer, flight);
            verify(crewService, times(2)).recordFlightCompletion(any(CrewMember.class), eq(flight));
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            FlightCompletedEvent event = eventCaptor.getValue();

            assertThat(event.flightId()).isEqualTo(flightId);
            assertThat(event.flightNumber()).isEqualTo("J2-101");
            assertThat(event.aircraftId()).isEqualTo(aircraft.getId());
            assertThat(event.completedBy()).isEqualTo("admin");

            assertThat(result).isNotNull();
            assertThat(result.flightNumber()).isEqualTo("J2-101");
        }

        @Test
        @DisplayName("When aircraft is null → aircraft save not called, no NullPointerException")
        void completeFlight_whenNoAircraft_shouldSkipAircraftUpdate() {
            flight.setAircraft(null);

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(assignmentRepository.findByFlightAndAssignmentStatus(flight, AssignmentStatus.ASSIGNED))
                    .thenReturn(List.of());
            when(flightMapper.toResponse(flight)).thenReturn(
                    new FlightResponse(flightId, "J2-101", null, "GYD", "IST",
                            FlightStatus.ARRIVED, null, null, null, null, null, null));

            flightCompletionService.completeFlight(flightId, "admin");

            verify(aircraftRepository, never()).save(any());

            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue().aircraftId()).isNull();
        }

        @Test
        @DisplayName("When no crew assigned → crew update not called")
        void completeFlight_whenNoCrewAssigned_shouldNotCallCrewService() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(assignmentRepository.findByFlightAndAssignmentStatus(flight, AssignmentStatus.ASSIGNED))
                    .thenReturn(List.of());
            when(flightMapper.toResponse(flight)).thenReturn(
                    new FlightResponse(flightId, "J2-101", aircraft.getId(), "GYD", "IST",
                            FlightStatus.ARRIVED, null, null, null, null, null, null));

            flightCompletionService.completeFlight(flightId, "admin");

            verify(crewService, never()).recordFlightCompletion(any(), any());
        }
    }

    @Nested
    @DisplayName("Failed Flight Completion")
    class FailedCompletion {

        @Test
        @DisplayName("When flight not found → ResourceNotFoundException")
        void completeFlight_whenFlightNotFound_shouldThrow() {
            UUID fakeId = UUID.randomUUID();

            when(flightRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> flightCompletionService.completeFlight(fakeId, "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(aircraftRepository, never()).save(any());
            verify(crewService, never()).recordFlightCompletion(any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}