package cl.duoc.ejemplo.ms.administracion.archivos.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new ArrayList<>(
				defaultConverter.convert(jwt) != null ? defaultConverter.convert(jwt) : Collections.emptyList());

		authorities.addAll(extractRoles(jwt.getClaim("roles")));
		authorities.addAll(extractRoles(jwt.getClaim("extension_roles")));

		return new JwtAuthenticationToken(jwt, authorities);
	}

	private Collection<GrantedAuthority> extractRoles(Object rolesClaim) {
		if (rolesClaim == null) {
			return Collections.emptyList();
		}

		List<String> roles = new ArrayList<>();
		if (rolesClaim instanceof Collection<?> collection) {
			collection.forEach(role -> roles.add(role.toString()));
		} else {
			roles.add(rolesClaim.toString());
		}

		return roles.stream()
				.map(this::toAuthority)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());
	}

	private String toAuthority(String role) {
		return role.startsWith("ROLE_") ? role : "ROLE_" + role;
	}
}
