package az.azal.skyflow.crew.repository;

import az.azal.skyflow.crew.model.CrewMember;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrewMemberRepository extends JpaRepository<CrewMember, UUID> {
	Optional<CrewMember> findByEmployeeId(String employeeId);

	boolean existsByEmployeeId(String employeeId);
}