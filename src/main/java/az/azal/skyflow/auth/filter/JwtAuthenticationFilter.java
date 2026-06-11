package az.azal.skyflow.auth.filter;

import az.azal.skyflow.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final StringRedisTemplate redisTemplate;

	private boolean checkIfTokenIsValid(String token) {
		return jwtService.isTokenValid(token);
	}

	private boolean checkIfTokenIsAccessToken(String token) {
		String type = jwtService.extractClaim(token, "typ");
		return type.equals("ACCESS");
	}

	private void authenticateRequest(HttpServletRequest request, String token) {
		String username = jwtService.extractUsername(token);
		String role = jwtService.extractClaim(token, "role");

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
				username,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));

		authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		SecurityContextHolder.getContext().setAuthentication(authenticationToken);

	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth/") ||
				path.startsWith("/ws") ||
				path.startsWith("/swagger-ui") ||
				path.startsWith("/v3/api-docs");
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);

		String jti = jwtService.extractClaim(token, "jti");
		if (Boolean.TRUE.equals( redisTemplate.hasKey("blacklist:"+jti))) {
			reject(response, "Token has been revoked");
			return;
		}

		if (!checkIfTokenIsValid(token) || !checkIfTokenIsAccessToken(token)) {
			reject(response, "Invalid or expired token");
			return;
		}

		authenticateRequest(request, token);
		filterChain.doFilter(request, response);
	}

	private void reject(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\": \"" + message + "\"}");
	}
}
