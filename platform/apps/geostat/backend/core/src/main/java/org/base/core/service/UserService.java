package org.base.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.Profile;
import org.base.core.entity.Role;
import org.base.core.entity.User;
import org.base.core.model.ProfileModel;
import org.base.core.model.request.ChangePasswordRequest;
import org.base.core.model.request.UpdateProfileRequest;
import org.base.core.model.request.UserRequest;
import org.base.core.model.response.UserResponse;
import org.base.core.repository.ProfileRepository;
import org.base.core.repository.RoleRepository;
import org.base.core.repository.UserRepository;
import org.base.core.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ProfileRepository profileRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithAuth(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserPrincipal(user);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @Transactional
    public UserResponse create(UserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Username already taken: " + req.username());
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRoles(resolveRoles(req));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));

        user.setUsername(req.username());

        // Only re-hash if a new password was actually provided
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }

        user.setRoles(resolveRoles(req));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        userRepository.deleteById(id);
    }


    // ── Helpers ────────────────────────────────────────────────────────────

    private Set<Role> resolveRoles(UserRequest req) {
        if (req.roles() == null || req.roles().isEmpty()) return Set.of();

        Set<Long> ids = req.roles().stream()
                .map(UserRequest.RoleRef::id)
                .collect(Collectors.toSet());

        Set<Role> found = new HashSet<>(roleRepository.findAllById(ids));;

        if (found.size() != ids.size()) {
            List<Long> missing = ids.stream()
                    .filter(rid -> found.stream().noneMatch(r -> r.getId().equals(rid)))
                    .toList();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Roles not found: " + missing);
        }

        return found;
    }


    // ── Add these methods to UserService ──────────────────────────────────────────

    public ProfileModel getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> profileRepository.save(new Profile(user)));

        return new ProfileModel(user.getUsername(), profile.getDisplayName(), profile.getAvatar());
    }

    @Transactional
    public ProfileModel updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> profileRepository.save(new Profile(user)));

        if (req.getDisplayName() != null) {
            profile.setDisplayName(req.getDisplayName().isBlank() ? null : req.getDisplayName());
        }
        if (req.getAvatar() != null) {
            profile.setAvatar(req.getAvatar().isBlank() ? null : req.getAvatar());
        }

        profileRepository.save(profile);
        return new ProfileModel(user.getUsername(), profile.getDisplayName(), profile.getAvatar());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
    }
}
