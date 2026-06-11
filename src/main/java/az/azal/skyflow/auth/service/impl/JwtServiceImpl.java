package az.azal.skyflow.auth.service.impl;

import az.azal.skyflow.auth.dto.JwtProperties;
import az.azal.skyflow.auth.model.AppUser;
import az.azal.skyflow.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

	private final JwtProperties jwtProperties;

	public String generateAccessToken(AppUser user) {
		return buildToken(
				Map.of("typ", "ACCESS",
				"role", user.getRole().name(),
				"jti", UUID.randomUUID().toString()),
				user.getUsername(), jwtProperties.accessTokenExpiration());
	}

	public String generateRefreshToken(AppUser user, UUID familyId) {
		return buildToken(
				Map.of("typ", "REFRESH",
				"fid", familyId.toString(),
				"jti", UUID.randomUUID().toString()),
				user.getUsername(), jwtProperties.refreshTokenExpiration());
	}

	@Override
	public String extractUsername(String token) {
		Claims claims = extractAllClaims(token);
		return claims.getSubject();
	}


	@Override
	public String extractClaim(String token, String claimName) {
		Claims claims = extractAllClaims(token);
		return claims.get(claimName, String.class);
	}

	@Override
	public boolean isTokenValid(String token) {
		try {
			extractAllClaims(token);
			return true;
		} catch (JwtException e){
			return false;
		}
	}

	@Override
	public Date extractExpiration(String token) {
		Claims claims = extractAllClaims(token);
		return claims.getExpiration();
	}

	 private String buildToken(Map<String,Object> extraClaims, String subject, long expiration){

		return Jwts.builder().claims(extraClaims)
				.subject(subject)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSigningKey())
				.compact();
	 }

	 private Claims extractAllClaims(String token){

		 return Jwts.parser()
				 .verifyWith(getSigningKey())
				 .build().parseSignedClaims(token)
				 .getPayload();
	 }

	 private SecretKey getSigningKey(){

		byte[] bytes = Decoders.BASE64.decode(jwtProperties.secretKey());

		 return Keys.hmacShaKeyFor(bytes);
	 }

}
