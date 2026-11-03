package com.example.demo.service;

import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.ReservationStatus;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    /** RG-01 : une réservation dure exactement 7 jours. */
    private static final Duration RESERVATION_DURATION = Duration.ofDays(7);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ReservationResponse findById(UUID id) {
        return toResponse(findReservation(id));
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Reservation reservation = new Reservation();
        applyRequest(reservation, request, null);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    @Transactional
    public ReservationResponse update(UUID id, ReservationRequest request) {
        Reservation reservation = findReservation(id);
        applyRequest(reservation, request, id);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    @Transactional
    public void delete(UUID id) {
        Reservation reservation = findReservation(id);
        reservationRepository.delete(reservation);
        reservationRepository.flush();
    }

    private Reservation findReservation(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
    }

    private void applyRequest(Reservation reservation, ReservationRequest request, UUID currentId) {
        Instant startAt = request.startAt();
        if (startAt.isBefore(Instant.now())) {
            throw new InvalidRequestException("La date de début ne peut pas être dans le passé.");
        }
        Instant endAt = startAt.plus(RESERVATION_DURATION);

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + request.roomId()));
        if (!room.isActive()) {
            throw new InvalidRequestException("Cette salle est désactivée et n'accepte plus de réservation.");
        }
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + request.userId()));
        if (!user.isActive()) {
            throw new InvalidRequestException("Ce compte est désactivé et ne peut pas réserver.");
        }

        ReservationStatus status = request.status() == null ? ReservationStatus.CONFIRMED : request.status();
        if (status == ReservationStatus.CONFIRMED
                && reservationRepository.existsOverlap(room.getId(), ReservationStatus.CONFIRMED,
                        startAt, endAt, currentId)) {
            throw new ConflictException("Cette salle est déjà réservée sur une période qui se chevauche.");
        }

        reservation.setRoom(room);
        reservation.setUser(user);
        reservation.setStartAt(startAt);
        reservation.setEndAt(endAt);
        reservation.setStatus(status);
        applyStatusTransitions(reservation, request, status);
    }

    private void applyStatusTransitions(Reservation reservation, ReservationRequest request,
                                        ReservationStatus status) {
        switch (status) {
            case CONFIRMED -> {
                reservation.setCancelledAt(null);
                reservation.setRevokedBy(null);
                reservation.setRevokedAt(null);
                reservation.setRevocationReason(null);
            }
            case CANCELLED -> {
                reservation.setCancelledAt(Instant.now());
                reservation.setRevokedBy(null);
                reservation.setRevokedAt(null);
                reservation.setRevocationReason(null);
            }
            case REVOKED -> {
                if (request.revokedBy() == null) {
                    throw new InvalidRequestException("revokedBy est obligatoire pour une révocation.");
                }
                User admin = userRepository.findById(request.revokedBy())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Administrateur introuvable : " + request.revokedBy()));
                reservation.setRevokedBy(admin);
                reservation.setRevokedAt(Instant.now());
                reservation.setRevocationReason(
                        request.revocationReason() == null ? null : request.revocationReason().trim());
            }
        }
    }

    private ReservationResponse toResponse(Reservation reservation) {
        UUID revokedById = reservation.getRevokedBy() == null ? null : reservation.getRevokedBy().getId();
        return new ReservationResponse(reservation.getId(), reservation.getRoom().getId(),
                reservation.getUser().getId(), reservation.getStartAt(), reservation.getEndAt(),
                reservation.getStatus(), reservation.getCreatedAt(), reservation.getCancelledAt(),
                revokedById, reservation.getRevokedAt(), reservation.getRevocationReason());
    }
}
