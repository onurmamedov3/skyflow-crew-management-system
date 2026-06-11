package az.azal.skyflow.auth.service;

import az.azal.skyflow.auth.model.AppUser;

import java.util.Date;
import java.util.UUID;

public interface JwtService {

	String generateAccessToken(AppUser user);

	String generateRefreshToken(AppUser user, UUID familyId);

	String extractUsername(String token);

	String extractClaim(String token, String claimName);

	boolean isTokenValid(String token);

	Date extractExpiration(String token);

}
