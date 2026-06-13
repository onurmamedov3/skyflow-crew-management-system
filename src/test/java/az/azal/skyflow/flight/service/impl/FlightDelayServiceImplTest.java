package az.azal.skyflow.flight.service.impl;

import az.azal.skyflow.auth.model.AppUser;
import az.azal.skyflow.auth.repository.AppUserRepository;
import az.azal.skyflow.common.exception.custom.BusinessRuleViolationException;
import az.azal.skyflow.common.exception.custom.ResourceNotFoundException;
import az.azal.skyflow.flight.dto.DelayRequest;
import az.azal.skyflow.flight.dto.DelayResponse;
import az.azal.skyflow.flight.event.FlightDelayedEvent;
import az.azal.skyflow.flight.mapper.DelayMapper;
import az.azal.skyflow.flight.model.DelayReason;
import az.azal.skyflow.flight.model.Flight;
import az.azal.skyflow.flight.model.FlightDelay;
import az.azal.skyflow.flight.model.FlightStatus;
import az.azal.skyflow.flight.repository.FlightDelayRepository;
import az.azal.skyflow.flight.repository.FlightRepository;
import az.azal.skyflow.flight.service.FlightStatusService;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightDelayService Unit Tests")
class FlightDelayServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @Mock private FlightDelayRepository flightDelayRepository;
    @Mock private FlightStatusService flightStatusService;
    @Mock private DelayMapper delayMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private FlightDelayServiceImpl flightDelayService;

    @Captor private ArgumentCaptor<FlightDelay> delayCaptor;
    @Captor private ArgumentCaptor<FlightDelayedEvent> eventCaptor;

    private Flight flight;
    private UUID flightId;
    private LocalDateTime originalDeparture;
    private LocalDateTime originalArrival;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();

        originalDeparture = LocalDateTime.of(2026, 7, 15, 10, 0);
        originalArrival = LocalDateTime.of(2026, 7, 15, 13, 0);

        flight = new Flight();
        flight.setId(flightId);
        flight.setFlightNumber("J2-101");
        flight.setDepartureTime(originalDeparture);
        flight.setArrivalTime(originalArrival);
        flight.setStatus(FlightStatus.SCHEDULED);

        AppUser appUser = new AppUser();
        appUser.setUsername("admin");
        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(appUser));
    }

    @Nested
    @DisplayName("delayFlight method")
    class DelayFlightTests {

        @Test
        @DisplayName("Valid request → delay created and times updated")
        void delayFlight_whenValid_shouldCreateDelayAndUpdateTimes(){

            LocalDateTime newDeparture = originalDeparture.plusHours(1);
            DelayRequest request = new DelayRequest(DelayReason.WEATHER, "Heavy rain", newDeparture);
            DelayResponse delayResponse = new DelayResponse(UUID.randomUUID(), flightId, DelayReason.WEATHER, 60, false, null);

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(delayMapper.toResponse(any(FlightDelay.class))).thenReturn(delayResponse);

            DelayResponse result = flightDelayService.delayFlight(flightId, request, "admin");

            assertThat(result).isNotNull();
            assertThat(result.reason()).isEqualTo(DelayReason.WEATHER);

            verify(flightDelayRepository).save(delayCaptor.capture());

            FlightDelay capturedDelay = delayCaptor.getValue();

            assertThat(capturedDelay.getDelayReason()).isEqualTo(DelayReason.WEATHER);
            assertThat(capturedDelay.getDelayReasonDetail()).isEqualTo("Heavy rain");
            assertThat(capturedDelay.getDelayMinutes()).isEqualTo(60);
            assertThat(capturedDelay.getOriginalDepartureTime()).isEqualTo(originalDeparture);
            assertThat(capturedDelay.getNewDepartureTime()).isEqualTo(newDeparture);
            assertThat(capturedDelay.isHighRisk()).isFalse();

            assertThat(flight.getDepartureTime()).isEqualTo(newDeparture);

            Duration originalFlightDuration = Duration.between(originalDeparture, originalArrival);
            LocalDateTime expectedNewArrival = newDeparture.plus(originalFlightDuration);

            assertThat(flight.getArrivalTime()).isEqualTo(expectedNewArrival);

            verify(flightStatusService).changeFlightStatus(
                    eq(flight), eq(FlightStatus.DELAYED), eq("admin"), contains("WEATHER"));

            verify(flightRepository).save(flight);
        }

        @Test
        @DisplayName("2+ saat delay → highRisk = true")
        void delayFlight_whenOver2Hours_shouldBeHighRisk() {

            LocalDateTime newDeparture = originalDeparture.plusHours(3);
            DelayRequest request = new DelayRequest(DelayReason.TECHNICAL, "Engine problem", newDeparture);
            DelayResponse delayResponse = new DelayResponse(UUID.randomUUID(), flightId, DelayReason.TECHNICAL, 180, true, null);

            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            when(delayMapper.toResponse(any(FlightDelay.class))).thenReturn(delayResponse);

            flightDelayService.delayFlight(flightId, request, "admin");

            verify(flightDelayRepository).save(delayCaptor.capture());

            assertThat(delayCaptor.getValue().isHighRisk()).isTrue();
            assertThat(delayCaptor.getValue().getDelayMinutes()).isEqualTo(180);
        }
    }

    @Test
    @DisplayName("Event is published with correct fields")
    void delayFlight_shouldPublishFlightDelayedEvent() {
        LocalDateTime newDeparture = originalDeparture.plusHours(2);
        DelayRequest request = new DelayRequest(DelayReason.ATC, "ATC clearance", newDeparture);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(delayMapper.toResponse(any())).thenReturn(
                new DelayResponse(UUID.randomUUID(), flightId, DelayReason.ATC, 120, true, null));

        flightDelayService.delayFlight(flightId, request, "dispatcher");

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        FlightDelayedEvent event = eventCaptor.getValue();

        assertThat(event.flightId()).isEqualTo(flightId);
        assertThat(event.flightNumber()).isEqualTo("J2-101");
        assertThat(event.reason()).isEqualTo(DelayReason.ATC);
        assertThat(event.delayMinutes()).isEqualTo(120);
        assertThat(event.highRisk()).isTrue();
        assertThat(event.newDepartureTime()).isEqualTo(newDeparture);
        assertThat(event.delayedBy()).isEqualTo("dispatcher");
    }

    @Test
    @DisplayName("Flight in DELAYED status can be delayed again")
    void delayFlight_whenAlreadyDelayed_shouldSucceed() {
        flight.setStatus(FlightStatus.DELAYED);

        LocalDateTime newDeparture = originalDeparture.plusHours(1);
        DelayRequest request = new DelayRequest(DelayReason.CREW, "Crew issue", newDeparture);

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
        when(delayMapper.toResponse(any())).thenReturn(
                new DelayResponse(UUID.randomUUID(), flightId, DelayReason.CREW, 60, false, null));

        DelayResponse result = flightDelayService.delayFlight(flightId, request, "admin");

        assertThat(result).isNotNull();
    }

    @Nested
    @DisplayName("Failed Delay Scenarios")
    class FailedDelay {

        @Test
        @DisplayName("When flight not found → ResourceNotFoundException")
        void delayFlight_whenFlightNotFound_shouldThrow() {
            UUID fakeId = UUID.randomUUID();
            DelayRequest request = new DelayRequest(DelayReason.WEATHER, null, originalDeparture.plusHours(1));

            when(flightRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> flightDelayService.delayFlight(fakeId, request, "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(flightDelayRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @ParameterizedTest(name = "Status {0} → delay not allowed")
        @EnumSource(value = FlightStatus.class, names = {"SCHEDULED", "DELAYED"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Statuses that do not allow delay")
        void delayFlight_whenInvalidStatus_shouldThrow(FlightStatus invalidStatus) {
            flight.setStatus(invalidStatus);
            DelayRequest request = new DelayRequest(DelayReason.WEATHER, null, originalDeparture.plusHours(1));
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            assertThatThrownBy(() -> flightDelayService.delayFlight(flightId, request, "admin"))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(flightDelayRepository, never()).save(any());
        }

        @Test
        @DisplayName("When new departure time is before current departure → exception")
        void delayFlight_whenNewTimeBeforeCurrentDeparture_shouldThrow() {

            LocalDateTime newDeparture = originalDeparture.minusHours(1);
            DelayRequest request = new DelayRequest(DelayReason.WEATHER, null, newDeparture);
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            assertThatThrownBy(() -> flightDelayService.delayFlight(flightId, request, "admin"))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(flightDelayRepository, never()).save(any());
        }

        @Test
        @DisplayName("When new departure time equals current departure → exception")
        void delayFlight_whenNewTimeSameAsCurrent_shouldThrow() {

            DelayRequest request = new DelayRequest(DelayReason.WEATHER, null, originalDeparture);
            when(flightRepository.findById(flightId)).thenReturn(Optional.of(flight));
            assertThatThrownBy(() -> flightDelayService.delayFlight(flightId, request, "admin"))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}