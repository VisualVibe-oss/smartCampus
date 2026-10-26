package com.example.demo.service;

import com.example.demo.dto.ReservationRequest;
import com.example.demo.dto.ReservationResponse;
import com.example.demo.entity.Reservation;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {
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

    public ReservationResponse findById(Long id) {
        return toResponse(findReservation(id));
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Reservation reservation = new Reservation();
        applyRequest(reservation, request);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = findReservation(id);
        applyRequest(reservation, request);
        return toResponse(reservationRepository.saveAndFlush(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = findReservation(id);
        reservationRepository.delete(reservation);
        reservationRepository.flush();
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
    }

    private void applyRequest(Reservation reservation, ReservationRequest request) {
        if (request.startsAt() == null || request.endsAt() == null
                || !request.startsAt().isBefore(request.endsAt())) {
            throw new InvalidRequestException("startsAt doit être strictement antérieur à endsAt.");
        }
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + request.roomId()));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + request.userId()));
        reservation.setRoom(room);
        reservation.setUser(user);
        reservation.setTitle(request.title().trim());
        reservation.setStartsAt(request.startsAt());
        reservation.setEndsAt(request.endsAt());
        if (request.status() != null) {
            reservation.setStatus(request.status());
        }
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getRoom().getId(),
                reservation.getUser().getId(), reservation.getTitle(), reservation.getStartsAt(),
                reservation.getEndsAt(), reservation.getStatus(), reservation.getCreatedAt());
    }
}
