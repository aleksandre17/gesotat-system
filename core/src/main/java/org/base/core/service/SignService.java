package org.base.core.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.User;
import org.base.core.model.request.RegisterRequest;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.entity.Role;
import org.base.core.model.response.TokenResponse;
import org.base.core.repository.RoleRepository;
import org.base.core.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class SignService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;          // ← დაემატა
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignResponse register(RegisterRequest request) {

        Role role = roleRepository.findByName("GENERAL")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("GENERAL");
                    newRole.setPermissions(Collections.emptySet());
                    return roleRepository.save(newRole);
                });


        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Collections.singleton(role))
                .build();

        userRepository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        return SignResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public SignResponse createSignToken(SignRequest signRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signRequest.getUsername(),
                        signRequest.getPassword()
                )
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(signRequest.getUsername());
        final String jwt = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        return SignResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenResponse refresh(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        final String refreshToken = authHeader.substring(7);   // ← ერთხელ strip, შემდეგ გამოყენება
        final String username = jwtTokenUtil.extractUsername(refreshToken);  // ← fix: stripped token

        if (username == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String newAccessToken = jwtTokenUtil.refreshToken(userDetails, refreshToken);  // ← fix
        final String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        return new TokenResponse(
                newAccessToken,
                newRefreshToken,
                jwtTokenUtil.extractExpiration(newAccessToken).getTime()
        );
    }


}

