package com.futuretech.service.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.futuretech.config.SecurityProperties;
import com.futuretech.entity.User;

/** Anyone can read a JWT's claims (only the signature is protected), so it never holds secrets. */
@Service
public class JwtService {

    static final String ISSUER = "futuretech";

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties props;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, SecurityProperties props, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenLifetime()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long accessTokenLifetimeSeconds() {
        return props.accessTokenLifetime().toSeconds();
    }
}
