package org.base.core.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.User;
import org.base.core.model.request.RegisterRequest;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.entity.Role;
import org.base.core.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class SignService {

    private final UserRepository repository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    public SignResponse register(RegisterRequest request) {
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        var jwtToken = jwtTokenUtil.generateToken(userDetails);

        return SignResponse.builder()
                .token(jwtToken)
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

        return SignResponse.builder()
                .token(jwt)
                .build();

    }

}
