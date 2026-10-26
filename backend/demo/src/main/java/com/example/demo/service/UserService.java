package com.example.demo.service;

import com.example.demo.dto.UserCreateRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.dto.UserUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Cette adresse email est déjà utilisée.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(hashPassword(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        String email = request.email().trim();
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Cette adresse email est déjà utilisée.");
        }
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.password() != null) {
            user.setPassword(hashPassword(request.password()));
        }
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        if (reservationRepository.existsByUser_Id(id)) {
            throw new ConflictException("Cet utilisateur est encore référencé par une réservation.");
        }
        userRepository.delete(user);
        userRepository.flush();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    private String hashPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8) {
            throw new InvalidRequestException("Le mot de passe doit comporter au moins huit caractères.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Le mot de passe ne doit pas dépasser 72 octets UTF-8 (limite BCrypt).");
        }
        return passwordEncoder.encode(password);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }
    public void testErreur() {
    erreur volontaire
}
}
