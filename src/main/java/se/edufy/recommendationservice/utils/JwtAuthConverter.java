package se.edufy.recommendationservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Value("${jwt.auth.converter.resource-id.name}")
    private String resourceIdName;

    @Value("${jwt.auth.converter.principal-attribute}")
    private String principalAttribute;

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        System.out.println("JWT claims: " + source.getClaims());

        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(source).stream(),
                extractResourceRoles(source).stream()
        ).collect(Collectors.toSet());

        System.out.println("Extracted roles: " + authorities);

        return new JwtAuthenticationToken(
                source,
                authorities,
                getPrincipalClaimName(source)
        );
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {

        if (!jwt.hasClaim("resource_access")) {
            return Set.of();
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

        if (!resourceAccess.containsKey(resourceIdName)) {
            return Set.of();
        }

        Map<String, Object> resource = (Map<String, Object>) resourceAccess.get(resourceIdName);

        if (!resource.containsKey("roles")) {
            return Set.of();
        }

        Collection<String> roles = (Collection<String>) resource.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    private String getPrincipalClaimName(Jwt jwt) {
        if (principalAttribute != null && !principalAttribute.isEmpty()) {
            return jwt.getClaim(principalAttribute);
        }
        return jwt.getClaim(JwtClaimNames.SUB);
    }
}
