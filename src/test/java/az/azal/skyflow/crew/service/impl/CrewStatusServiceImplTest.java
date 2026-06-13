package az.azal.skyflow.crew.service.impl;

import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.crew.dto.CrewResponse;
import az.azal.skyflow.crew.event.CrewStatusChangedEvent;
import az.azal.skyflow.crew.mapper.CrewMapper;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.CrewRole;
import az.azal.skyflow.crew.model.CrewStatus;
import az.azal.skyflow.crew.repository.CrewMemberRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrewStatusService Unit Tests")
class CrewStatusServiceImplTest {

    @Mock private CrewMemberRepository crewMemberRepository;
    @Mock private CrewMapper crewMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CrewStatusServiceImpl crewStatusService;

    @Captor private ArgumentCaptor<CrewMember> crewCaptor;
    @Captor private ArgumentCaptor<CrewStatusChangedEvent> eventCaptor;

    private CrewMember crewMember;
    private UUID crewId;

    @BeforeEach
    void setUp() {
        crewId = UUID.randomUUID();
        crewMember = new CrewMember();
        crewMember.setId(crewId);
        crewMember.setEmployeeId("CPT-001");
        crewMember.setFirstName("XXX");
        crewMember.setLastName("YYY");
        crewMember.setRole(CrewRole.CAPTAIN);
        crewMember.setStatus(CrewStatus.AVAILABLE);
    }

    @Nested
    @DisplayName("Successful Status Change")
    class SuccessfulStatusChange {

        @Test
        @DisplayName("AVAILABLE → SICK: status changes, event is published, response is returned")
        void updateStatus_availableToSick_shouldSucceed() {

            CrewResponse expectedResponse = new CrewResponse(crewId, "CPT-001", "XXX", "YYY",
                    CrewRole.CAPTAIN, null, null, CrewStatus.SICK, 0, null);

            when(crewMemberRepository.findById(crewId)).thenReturn(Optional.of(crewMember));
            when(crewMapper.toResponse(crewMember)).thenReturn(expectedResponse);

            CrewResponse result = crewStatusService.updateCrewStatus(crewId, CrewStatus.SICK, "admin");

            verify(crewMemberRepository).save(crewCaptor.capture());

            assertThat(crewCaptor.getValue().getStatus()).isEqualTo(CrewStatus.SICK);

            verify(eventPublisher).publishEvent(eventCaptor.capture());

            CrewStatusChangedEvent event = eventCaptor.getValue();

            assertThat(event.crewMemberId()).isEqualTo(crewId);
            assertThat(event.employeeId()).isEqualTo("CPT-001");
            assertThat(event.oldStatus()).isEqualTo(CrewStatus.AVAILABLE);
            assertThat(event.newStatus()).isEqualTo(CrewStatus.SICK);
            assertThat(event.changedBy()).isEqualTo("admin");
            assertThat(result.status()).isEqualTo(CrewStatus.SICK);
        }

        @Test
        @DisplayName("SICK → AVAILABLE: status is restored")
        void updateStatus_sickToAvailable_shouldSucceed() {
            crewMember.setStatus(CrewStatus.SICK);

            CrewResponse expectedResponse = new CrewResponse(crewId, "CPT-001", "XXX", "YYY",
                    CrewRole.CAPTAIN, null, null, CrewStatus.AVAILABLE, 0, null);

            when(crewMemberRepository.findById(crewId)).thenReturn(Optional.of(crewMember));
            when(crewMapper.toResponse(crewMember)).thenReturn(expectedResponse);

            CrewResponse result = crewStatusService.updateCrewStatus(crewId, CrewStatus.AVAILABLE, "admin");

            verify(crewMemberRepository).save(crewCaptor.capture());

            assertThat(crewCaptor.getValue().getStatus()).isEqualTo(CrewStatus.AVAILABLE);

            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue().oldStatus()).isEqualTo(CrewStatus.SICK);
            assertThat(eventCaptor.getValue().newStatus()).isEqualTo(CrewStatus.AVAILABLE);
        }
    }

    @Nested
    @DisplayName("Failed Status Change")
    class FailedStatusChange {

        @Test
        @DisplayName("Non-existing crew → ResourceNotFoundException")
        void updateStatus_whenNotFound_shouldThrow() {
            UUID fakeId = UUID.randomUUID();

            when(crewMemberRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crewStatusService.updateCrewStatus(fakeId, CrewStatus.SICK, "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(crewMemberRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}