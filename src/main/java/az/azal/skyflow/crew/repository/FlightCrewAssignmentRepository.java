package az.azal.skyflow.crew.repository;

import az.azal.skyflow.crew.model.AssignmentStatus;
import az.azal.skyflow.crew.model.CrewMember;
import az.azal.skyflow.crew.model.FlightCrewAssignment;
import az.azal.skyflow.flight.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlightCrewAssignmentRepository extends JpaRepository<FlightCrewAssignment, UUID> {

	boolean existsByFlightAndCrewMember(Flight flight, CrewMember crewMember);

	@Query("""
	SELECT COUNT(fca) > 0  FROM FlightCrewAssignment fca
	WHERE fca.crewMember = :crewMember
	 AND fca.assignmentStatus = 'ASSIGNED'
	 AND fca.flight.departureTime < :arrivalTime
	 AND fca.flight.arrivalTime > :departureTime
	""")
	boolean hasTimeConflicts(@Param("crewMember") CrewMember crewMember,
							 @Param("departureTime") LocalDateTime departureTime,
							 @Param("arrivalTime") LocalDateTime arrivalTime
	);

	@Query(value = """
		SELECT COALESCE(SUM(
			EXTRACT(EPOCH FROM (
				LEAST(f.arrival_time, :windowEnd)
				-
				GREATEST(f.departure_time, :windowStart)
			)) / 60
		), 0)
		FROM flight_crew_assignment fca
		JOIN flight f ON fca.flight_id = f.id
		WHERE fca.crew_member_id = :crewMemberId
		  AND fca.assignment_status IN ('ASSIGNED', 'CONFIRMED')
		  AND f.departure_time < :windowEnd
		  AND f.arrival_time > :windowStart
""", nativeQuery = true)
	long sumFlightMinutesInWindow(@Param("crewMemberId") UUID crewMemberId,
							 @Param("windowStart") LocalDateTime windowStart,
							 @Param("windowEnd") LocalDateTime windowEnd
	);

	@Query("""
	SELECT fca FROM FlightCrewAssignment fca
	JOIN FETCH fca.crewMember
	WHERE fca.flight = :flight
	AND fca.assignmentStatus = :status
	""")
	List<FlightCrewAssignment> findByFlightAndAssignmentStatus(@Param("flight") Flight flight, @Param("status") AssignmentStatus assignmentStatus);


	@Query("""
	SELECT fca FROM FlightCrewAssignment fca
	JOIN FETCH fca.flight f
	WHERE fca.crewMember = :crewMember
	AND fca.assignmentStatus = 'ASSIGNED'
	AND f.departureTime > :now
	ORDER BY f.departureTime ASC
	""")
	List <FlightCrewAssignment> findFutureAssignmentsByCrewMember(
			@Param("crewMember") CrewMember crewMember,
			@Param("now") LocalDateTime now);

	@Modifying(clearAutomatically = true)
	@Query("""
	UPDATE FlightCrewAssignment fca
	SET fca.assignmentStatus = :newStatus
	WHERE fca.crewMember = :crewMember
	AND fca.assignmentStatus = 'ASSIGNED'
	AND fca.flight.departureTime > :now
	""")
	int updateFutureAssignmentStatuses(@Param("crewMember") CrewMember crewMember,
									  @Param("newStatus") AssignmentStatus newStatus,
									  @Param("now") LocalDateTime now);
}
