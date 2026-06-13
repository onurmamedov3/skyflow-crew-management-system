package az.azal.skyflow.crew.service.impl;

import az.azal.skyflow.auth.model.AppUser;
import az.azal.skyflow.auth.repository.AppUserRepository;
import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.dto.CrewAssignmentRequest;
import az.azal.skyflow.crew.dto.CrewAssignmentResponse;
import az.azal.skyflow.crew.mapper.CrewMapper;
import az.azal.skyflow.crew.model.*;
import az.azal.skyflow.crew.repository.CrewMemberRepository;
import az.azal.skyflow.crew.repository.FlightCrewAssignmentRepository;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightCrewAssignment Service Unit Tests — Business Rules")
class FlightCrewAssignmentServiceImplTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private CrewMemberRepository crewMemberRepository;
    @Mock
    private FlightCrewAssignmentRepository assignmentRepository;
    @Mock
    private CrewMapper crewMapper;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private FlightCrewAssignmentServiceImpl service;

    @Captor
    private ArgumentCaptor<FlightCrewAssignment> assignmentCaptor;

    private Flight flight;
    private CrewMember crewMember;
    private UUID flightId;
    private CrewAssignmentRequest request;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();

        flight = new Flight();
        flight.setId(flightId);
        flight.setFlightNumber("J2-101");
        flight.setDepartureTime(LocalDateTime.of(2026, 6, 10, 10, 0));
        flight.setArrivalTime(LocalDateTime.of(2026, 6, 10, 13, 0));
        flight.setStatus(FlightStatus.SCHEDULED);

        crewMember = new CrewMember();
        crewMember.setId(UUID.randomUUID());
        crewMember.setEmployeeId("CPT-001");
        crewMember.setStatus(CrewStatus.AVAILABLE);
        crewMember.setTotalFlightMinutes(0);

        request = new CrewAssignmentRequest(crewMember.getId(), CrewRole.CAPTAIN);

        AppUser appUser = new AppUser();
        appUser.setUsername("system");
        lenient().when(appUserRepository.findByUsername("system")).thenReturn(Optional.of(appUser));
    }

    @Nested
    @DisplayName("Successful Crew Assignment")
    class SuccessfulAssignment {

        @Test
        @DisplayName("All conditions met → successfully assigned")
        void assignCrew_whenAllValid_shouldSucceed() {

            CrewAssignmentResponse expectedResponse = new CrewAssignmentResponse(
                    UUID.randomUUID(), flightId, crewMember.getId(), "CPT-001",
                    "XXX", "ZZZ", "YYY", CrewRole.CAPTAIN,
                    AssignmentStatus.ASSIGNED, LocalDateTime.now());

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(crewMember, flight.getDepartureTime(), flight.getArrivalTime()))
                    .thenReturn(false);
            when(assignmentRepository.sumFlightMinutesInWindow(any(), any(), any())).thenReturn(0L);
            when(crewMapper.toAssignmentResponse(any())).thenReturn(expectedResponse);

            CrewAssignmentResponse result = service.assignCrewToFlights(flightId, request, "system");

            assertThat(result).isNotNull();
            assertThat(result.crewMemberId()).isEqualTo(crewMember.getId());
            assertThat(result.roleOnFlight()).isEqualTo(CrewRole.CAPTAIN);

            verify(assignmentRepository).save(assignmentCaptor.capture());

            FlightCrewAssignment captured = assignmentCaptor.getValue();

            assertThat(captured.getCrewMember()).isEqualTo(crewMember);
            assertThat(captured.getFlight()).isEqualTo(flight);
            assertThat(captured.getRoleOnFlight()).isEqualTo(CrewRole.CAPTAIN);
            assertThat(captured.getAssignmentStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
            assertThat(captured.getAssignedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Resource Not Found")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("When flight not found → ResourceNotFoundException")
        void assignCrew_whenFlightNotFound_shouldThrow() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("When crew member not found → ResourceNotFoundException")
        void assignCrew_whenCrewNotFound_shouldThrow() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Crew Status Validation")
    class CrewStatusTests {

        @ParameterizedTest(name = "Status {0} → assignment should be rejected")
        @EnumSource(value = CrewStatus.class, names = "AVAILABLE", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Crew not AVAILABLE → BusinessRuleViolationException")
        void assignCrew_whenCrewNotAvailable_shouldThrow(CrewStatus invalidStatus) {
            crewMember.setStatus(invalidStatus);

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Duplicate Assignment")
    class DuplicateAssignmentTests {

        @Test
        @DisplayName("Already assigned crew → BusinessRuleViolationException")
        void assignCrew_whenAlreadyAssigned_shouldThrow() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(true);

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Time Conflict")
    class TimeConflictTests {

        @Test
        @DisplayName("Time conflict exists → BusinessRuleViolationException")
        void assignCrew_whenTimeConflict_shouldThrow() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(crewMember, flight.getDepartureTime(), flight.getArrivalTime()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(assignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Rest Period Validation")
    class RestPeriodTests {

        @Test
        @DisplayName("12+ flight hours in 24 hours → BusinessRuleViolationException (cumulative hours)")
        void assignCrew_whenCumulativeHoursExceeded_shouldThrow() {
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.sumFlightMinutesInWindow(any(), any(), any())).thenReturn(540L);

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Flight close to limit but not exceeding → successful")
        void assignCrew_whenJustBelowLimit_shouldSucceed() {

            CrewAssignmentResponse expectedResponse = new CrewAssignmentResponse(
                    UUID.randomUUID(), flightId, crewMember.getId(), "CPT-001",
                    "XXX", "YYY", null, CrewRole.CAPTAIN,
                    AssignmentStatus.ASSIGNED, LocalDateTime.now());

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.sumFlightMinutesInWindow(any(), any(), any())).thenReturn(480L);
            when(crewMapper.toAssignmentResponse(any())).thenReturn(expectedResponse);

            CrewAssignmentResponse result = service.assignCrewToFlights(flightId, request, "system");

            assertThat(result).isNotNull();

            verify(assignmentRepository).save(any(FlightCrewAssignment.class));
        }

        @Test
        @DisplayName("When lastFlightEnd exists, minimum rest period is checked — rejected if violated")
        void assignCrew_whenRestPeriodViolated_shouldThrow() {

            crewMember.setLastFlightEnd(flight.getDepartureTime().minusHours(4));

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.sumFlightMinutesInWindow(any(), any(), any())).thenReturn(1L);

            assertThatThrownBy(() -> service.assignCrewToFlights(flightId, request, "system"))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("When lastFlightEnd is null → rest period is not checked, successful")
        void assignCrew_whenLastFlightEndNull_shouldSkipRestCheck() {
            crewMember.setLastFlightEnd(null);
            CrewAssignmentResponse expectedResponse = new CrewAssignmentResponse(
                    UUID.randomUUID(), flightId, crewMember.getId(), "CPT-001",
                    "XXX", "YYY", null, CrewRole.CAPTAIN,
                    AssignmentStatus.ASSIGNED, LocalDateTime.now());

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(crewMemberRepository.findById(crewMember.getId())).thenReturn(Optional.of(crewMember));
            when(assignmentRepository.existsByFlightAndCrewMember(flight, crewMember)).thenReturn(false);
            when(assignmentRepository.hasTimeConflicts(any(), any(), any())).thenReturn(false);
            when(assignmentRepository.sumFlightMinutesInWindow(any(), any(), any())).thenReturn(0L);
            when(crewMapper.toAssignmentResponse(any())).thenReturn(expectedResponse);

            CrewAssignmentResponse result = service.assignCrewToFlights(flightId, request, "system");

            assertThat(result).isNotNull();

            verify(assignmentRepository).save(any(FlightCrewAssignment.class));
        }
    }

    @Nested
    @DisplayName("handleCrewUnavailability")
    class HandleCrewUnavailabilityTests {

        @Test
        @DisplayName("When future assignments exist → statuses become REMOVED")
        void handleUnavailability_whenFutureAssignmentsExist_shouldRemoveThem() {
            FlightCrewAssignment futureAssignment = new FlightCrewAssignment();

            futureAssignment.setId(UUID.randomUUID());
            futureAssignment.setCrewMember(crewMember);
            futureAssignment.setFlight(flight);

            when(assignmentRepository.findFutureAssignmentsByCrewMember(eq(crewMember), any(LocalDateTime.class)))
                    .thenReturn(List.of(futureAssignment));

            List<FlightCrewAssignment> result = service.handleCrewUnavailability(crewMember);

            assertThat(result).hasSize(1);

            verify(assignmentRepository).updateFutureAssignmentStatuses(
                    eq(crewMember), eq(AssignmentStatus.REMOVED), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("When no future assignments → empty list returned, update not called")
        void handleUnavailability_whenNoFutureAssignments_shouldReturnEmpty() {
            when(assignmentRepository.findFutureAssignmentsByCrewMember(eq(crewMember), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            List<FlightCrewAssignment> result = service.handleCrewUnavailability(crewMember);

            assertThat(result).isEmpty();
            verify(assignmentRepository, never()).updateFutureAssignmentStatuses(any(), any(), any());
        }
    }
}