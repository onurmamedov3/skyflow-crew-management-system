package az.azal.skyflow.crew.service.impl;

import az.azal.skyflow.common.exception.custom.DuplicateResourceException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.dto.CrewRequest;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.event.CrewStatusChangedEvent;
import az.azal.skyflow.crew.mapper.CrewMapper;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.CrewRole;
import az.azal.skyflow.crew.model.CrewStatus;
import az.azal.skyflow.crew.repository.CrewMemberRepository;
import az.azal.skyflow.flight.model.Flight;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrewService Unit Tests")
class CrewServiceImplTest {

    @Mock private CrewMemberRepository repository;
    @Mock private CrewMapper crewMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CrewServiceImpl crewService;

    @Captor private ArgumentCaptor<CrewMember> crewCaptor;
    @Captor private ArgumentCaptor<CrewStatusChangedEvent> eventCaptor;

    private CrewRequest request;
    private CrewMember crewMember;
    private CrewResponse response;
    private UUID crewId;

    @BeforeEach
    void setUp() {
        crewId = UUID.randomUUID();
        request = new CrewRequest("CPT-001", "XXX", "YYY", "ZZZ", "+994501234567", CrewRole.CAPTAIN);

        crewMember = new CrewMember();
        crewMember.setId(crewId);
        crewMember.setEmployeeId("CPT-001");
        crewMember.setFirstName("XXX");
        crewMember.setLastName("YYY");
        crewMember.setRole(CrewRole.CAPTAIN);
        crewMember.setStatus(CrewStatus.AVAILABLE);
        crewMember.setTotalFlightMinutes(0);

        response = new CrewResponse(crewId, "CPT-001", "XXX", "YYY", CrewRole.CAPTAIN,
                "ZZZ", "+994501234567", CrewStatus.AVAILABLE, 0, null);
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateTests {

        @Test
        @DisplayName("Valid request → created successfully, status becomes AVAILABLE")
        void create_whenValid_shouldCreateWithAvailableStatus() {
            when(repository.existsByEmployeeId("CPT-001")).thenReturn(false);
            when(crewMapper.toEntity(request)).thenReturn(crewMember);
            when(crewMapper.toResponse(crewMember)).thenReturn(response);

            CrewResponse result = crewService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.employeeId()).isEqualTo("CPT-001");
            assertThat(result.status()).isEqualTo(CrewStatus.AVAILABLE);

            verify(repository).save(crewCaptor.capture());

            assertThat(crewCaptor.getValue().getStatus()).isEqualTo(CrewStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Duplicate employeeId → DuplicateResourceException")
        void create_whenDuplicate_shouldThrow() {
            when(repository.existsByEmployeeId("CPT-001")).thenReturn(true);

            assertThatThrownBy(() -> crewService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("CPT-001");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Operations")
    class GetTests {

        @Test
        @DisplayName("Existing employeeId → CrewResponse returned")
        void getByEmployeeId_whenExists_shouldReturnResponse() {
            when(repository.findByEmployeeId("CPT-001")).thenReturn(Optional.of(crewMember));
            when(crewMapper.toResponse(crewMember)).thenReturn(response);

            CrewResponse result = crewService.getCrewByEmployeeId("CPT-001");

            assertThat(result).isNotNull();
            assertThat(result.employeeId()).isEqualTo("CPT-001");
        }

        @Test
        @DisplayName("Non-existing employeeId → ResourceNotFoundException")
        void getByEmployeeId_whenNotExists_shouldThrow() {
            when(repository.findByEmployeeId("NONE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crewService.getCrewByEmployeeId("NONE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("getAll → works with Pageable")
        void getAll_shouldReturnPage() {
            Page<CrewMember> page = new PageImpl<>(List.of(crewMember));

            when(repository.findAll(any(PageRequest.class))).thenReturn(page);
            when(crewMapper.toResponse(crewMember)).thenReturn(response);

            Page<CrewResponse> result = crewService.getAll(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {

        @Test
        @DisplayName("Existing crew update → successful")
        void update_whenExists_shouldUpdate() {
            when(repository.findByEmployeeId("CPT-001")).thenReturn(Optional.of(crewMember));
            when(crewMapper.toResponse(crewMember)).thenReturn(response);

            CrewResponse result = crewService.update("CPT-001", request);

            assertThat(result).isNotNull();
            verify(crewMapper).updateEntity(request, crewMember);
            verify(repository).save(crewMember);
        }

        @Test
        @DisplayName("Non-existing crew update → ResourceNotFoundException")
        void update_whenNotExists_shouldThrow() {
            when(repository.findByEmployeeId("NONE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crewService.update("NONE", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Delete → status INACTIVE, CrewStatusChangedEvent is published")
        void delete_whenExists_shouldSetInactiveAndPublishEvent() {
            crewMember.setStatus(CrewStatus.AVAILABLE);

            when(repository.findByEmployeeId("CPT-001")).thenReturn(Optional.of(crewMember));

            crewService.delete("CPT-001");

            verify(repository).save(crewCaptor.capture());

            assertThat(crewCaptor.getValue().getStatus()).isEqualTo(CrewStatus.INACTIVE);

            verify(eventPublisher).publishEvent(eventCaptor.capture());

            CrewStatusChangedEvent event = eventCaptor.getValue();

            assertThat(event.crewMemberId()).isEqualTo(crewId);
            assertThat(event.employeeId()).isEqualTo("CPT-001");
            assertThat(event.oldStatus()).isEqualTo(CrewStatus.AVAILABLE);
            assertThat(event.newStatus()).isEqualTo(CrewStatus.INACTIVE);
            assertThat(event.changedBy()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("Non-existing crew delete → ResourceNotFoundException")
        void delete_whenNotExists_shouldThrow() {
            when(repository.findByEmployeeId("NONE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crewService.delete("NONE"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("recordFlightCompletion Operations")
    class RecordFlightCompletionTests {

        @Test
        @DisplayName("Flight completion → lastFlightEnd and totalFlightMinutes are updated")
        void recordFlightCompletion_shouldUpdateCrewStats() {
            crewMember.setTotalFlightMinutes(120);

            Flight flight = new Flight();

            flight.setDepartureTime(LocalDateTime.of(2026, 7, 15, 10, 0));
            flight.setArrivalTime(LocalDateTime.of(2026, 7, 15, 13, 0));
            flight.setActualArrivalTime(LocalDateTime.of(2026, 7, 15, 13, 0));

            crewService.recordFlightCompletion(crewMember, flight);

            verify(repository).save(crewCaptor.capture());

            CrewMember captured = crewCaptor.getValue();

            assertThat(captured.getLastFlightEnd()).isNotNull();

            long expectedMinutes = Duration.between(flight.getDepartureTime(), flight.getActualArrivalTime()).toMinutes();

            assertThat(captured.getTotalFlightMinutes()).isEqualTo(120 + (int) expectedMinutes);
        }

        @Test
        @DisplayName("When actualDepartureTime exists, it is used")
        void recordFlightCompletion_whenActualDeparture_shouldUseIt() {
            crewMember.setTotalFlightMinutes(0);

            Flight flight = new Flight();

            flight.setDepartureTime(LocalDateTime.of(2026, 7, 15, 10, 0));
            flight.setActualDepartureTime(LocalDateTime.of(2026, 7, 15, 10, 30));
            flight.setArrivalTime(LocalDateTime.of(2026, 7, 15, 13, 0));
            flight.setActualArrivalTime(LocalDateTime.of(2026, 7, 15, 13, 0));

            crewService.recordFlightCompletion(crewMember, flight);

            verify(repository).save(crewCaptor.capture());

            long expected = Duration.between(flight.getActualDepartureTime(), flight.getActualArrivalTime()).toMinutes();

            assertThat(crewCaptor.getValue().getTotalFlightMinutes()).isEqualTo((int) expected);
        }
    }
}