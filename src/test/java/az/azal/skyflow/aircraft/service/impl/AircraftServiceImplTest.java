package az.azal.skyflow.aircraft.service.impl;

import az.azal.skyflow.aircraft.dto.AircraftRequest;
import az.azal.skyflow.aircraft.dto.AircraftResponse;
import az.azal.skyflow.aircraft.mapper.AircraftMapper;
import az.azal.skyflow.aircraft.model.Aircraft;
import az.azal.skyflow.aircraft.model.AircraftStatus;
import az.azal.skyflow.aircraft.repository.AircraftRepository;
import az.azal.skyflow.common.exception.custom.DuplicateResourceException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftServiceImplTest {

    @Mock
    private AircraftRepository aircraftRepository;

    @Mock
    private AircraftMapper aircraftMapper;

    @InjectMocks
    private AircraftServiceImpl aircraftService;

    @Captor
    private ArgumentCaptor<Aircraft> aircraftCaptor;

    private AircraftRequest request;
    private Aircraft aircraft;
    private AircraftResponse response;
    private UUID aircraftId;

    @BeforeEach
    void setUp() {
        aircraftId = UUID.randomUUID();
        request = new AircraftRequest("4K-AZ01", "A320", 180, AircraftStatus.ACTIVE, null, null, 0L);
        aircraft = new Aircraft();
        aircraft.setId(aircraftId);
        aircraft.setRegistrationNumber("4K-AZ01");
        aircraft.setModel("A320");
        aircraft.setCapacity(180);
        aircraft.setStatus(AircraftStatus.ACTIVE);
        response = new AircraftResponse(
                aircraftId, "4K-AZ01", "A320", 180,
                AircraftStatus.ACTIVE, null, null, 0L, null);
    }

    @Nested
    @DisplayName("Create Aircraft Tests")
    class CreateAircraftTests {

        @Test
        @DisplayName("Valid request → successfully created and response returned")
        void create_whenValidRequest_shouldReturnResponse() {

            when(aircraftRepository.existsByRegistrationNumber("4K-AZ01")).thenReturn(false);
            when(aircraftMapper.toEntity(request)).thenReturn(aircraft);
            when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
            when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

            AircraftResponse result = aircraftService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.registrationNumber()).isEqualTo("4K-AZ01");
            assertThat(result.status()).isEqualTo(AircraftStatus.ACTIVE);
            assertThat(result.model()).isEqualTo("A320");
            assertThat(result.capacity()).isEqualTo(180);

            verify(aircraftRepository, times(1)).save(any(Aircraft.class));
        }

        @Test
        @DisplayName("Duplicate registrationNumber → throws DuplicateResourceException")
        void create_whenDuplicate_shouldThrowException() {

            when(aircraftRepository.existsByRegistrationNumber("4K-AZ01")).thenReturn(true);

            assertThatThrownBy(() -> aircraftService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("4K-AZ01");

            verify(aircraftRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Operations")
    class GetTests {

        @Test
        @DisplayName("Existing regNumber → returns AircraftResponse")
        void getByRegNumber_whenExists_shouldReturnResponse() {

            when(aircraftRepository.findByRegistrationNumber("4K-AZ01"))
                    .thenReturn(Optional.of(aircraft));
            when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

            AircraftResponse result = aircraftService.getByRegistrationNumber("4K-AZ01");

            assertThat(result).isNotNull();
            assertThat(result.registrationNumber()).isEqualTo("4K-AZ01");

            verify(aircraftRepository).findByRegistrationNumber("4K-AZ01");
        }

        @Test
        @DisplayName("Non-existent regNumber → throws ResourceNotFoundException")
        void getByRegNumber_whenNotExists_shouldThrowNotFoundException() {
            when(aircraftRepository.findByRegistrationNumber("4K-NONE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> aircraftService.getByRegistrationNumber("4K-NONE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("4K-NONE");
        }

        @Test
        @DisplayName("getAll -> Pageable works correctly")
        void getAll_whenCalled_shouldReturnPageOfResponses() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Aircraft> aircraftPage = new PageImpl<>(List.of(aircraft));

            when(aircraftRepository.findAll(pageable)).thenReturn(aircraftPage);

            when(aircraftMapper.toResponse(aircraft)).thenReturn(response);

            Page<AircraftResponse> responses = aircraftService.getAll(pageable);

            assertThat(responses).isNotNull();
            assertThat(responses.getContent().getFirst().registrationNumber()).isEqualTo("4K-AZ01");
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateTests {
        @Test
        @DisplayName("Existing aircraft update → returns updated response")
        void update_whenExists_shouldUpdateAndReturnResponse() {

            AircraftRequest updateRequest = new AircraftRequest("4K-AZ01", "A321", 220, AircraftStatus.ACTIVE, null, null, 100L);
            AircraftResponse updatedResponse = new AircraftResponse(
                    aircraftId, "4K-AZ01", "A321", 220,
                    AircraftStatus.ACTIVE, null, null, 100L, null);

            when(aircraftRepository.findByRegistrationNumber("4K-AZ01"))
                    .thenReturn(Optional.of(aircraft));
            when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
            when(aircraftMapper.toResponse(aircraft)).thenReturn(updatedResponse);

            AircraftResponse result = aircraftService.update("4K-AZ01", updateRequest);

            assertThat(result.model()).isEqualTo("A321");
            assertThat(result.capacity()).isEqualTo(220);

            verify(aircraftMapper).updateEntity(updateRequest, aircraft);
            verify(aircraftRepository).save(aircraft);
        }

        @Test
        @DisplayName("Non-existent aircraft update → throws ResourceNotFoundException")
        void update_whenNotExists_shouldThrowNotFoundException() {
            when(aircraftRepository.findByRegistrationNumber("4K-NONE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> aircraftService.update("4K-NONE", request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(aircraftRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Existing aircraft delete → status set to RETIRED (soft delete)")
        void delete_whenExists_shouldSetStatusToRetired() {

            when(aircraftRepository.findByRegistrationNumber("4K-AZ01"))
                    .thenReturn(Optional.of(aircraft));

            aircraftService.delete("4K-AZ01");

            verify(aircraftRepository).save(aircraftCaptor.capture());

            Aircraft savedAircraft = aircraftCaptor.getValue();

            assertThat(savedAircraft.getStatus()).isEqualTo(AircraftStatus.RETIRED);
            assertThat(savedAircraft.getRegistrationNumber()).isEqualTo("4K-AZ01");
        }

        @Test
        @DisplayName("Non-existent aircraft delete → throws ResourceNotFoundException")        void delete_whenNotExists_shouldThrowNotFoundException() {
            when(aircraftRepository.findByRegistrationNumber("4K-NONE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> aircraftService.delete("4K-NONE"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(aircraftRepository, never()).save(any());
        }
    }
}