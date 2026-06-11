package az.azal.skyflow.auth.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skyflow.security.jwt")
public record JwtProperties(
		String secretKey,
		long accessTokenExpiration,
		long refreshTokenExpiration
) {
}
