package org.base.core.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.User;
import org.base.core.model.request.RegisterRequest;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.entity.Role;
import org.base.core.model.response.TokenResponse;
import org.base.core.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SignService {

    private final UserRepository repository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    public SignResponse register(RegisterRequest request) {


        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setName("GENERAL");
        roles.add(role);

        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();
        repository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        var jwtToken = jwtTokenUtil.generateToken(userDetails);

        return SignResponse.builder()
                .token(jwtToken)
                .roles(user.getRoles().stream()
                        .map(rolee -> Map.of(
                                "name", role.getName(),
                                "permissions", role.getPermissions().stream()
                                        .map(permission -> permission.getName())
                                        .collect(Collectors.toList())
                        ))
                        .collect(Collectors.toList()))
                .build();
    }

    public SignResponse createSignToken(
            @RequestBody @Valid SignRequest signRequest
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signRequest.getUsername(),
                        signRequest.getPassword()
                )
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(signRequest.getUsername());
        final String jwt = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        //jwtTokenUtil.generateTokens(userDetails, new User());

        return SignResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .build();

    }


    public TokenResponse refresh(String refreshToken) {

        if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Not valid refresh token format");
        }

        String username = jwtTokenUtil.extractUsername(refreshToken);
        if (username == null) {
            throw new IllegalArgumentException("Not valid refresh token");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String jwt = jwtTokenUtil.refreshToken(userDetails, refreshToken.substring(7));
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        //if (!jwtTokenUtil.isTokenValid(newRefreshToken)) {
            //throw new IllegalArgumentException("Not valid refresh token");
        //}
        //jwtTokenUtil.revokeAllUserTokens(new User());

        return new TokenResponse(jwt, newRefreshToken, jwtTokenUtil.extractExpiration(jwt).getTime());

    }

}
