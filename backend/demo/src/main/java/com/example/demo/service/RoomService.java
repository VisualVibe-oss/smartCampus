package com.example.demo.service;

import com.example.demo.dto.RoomRequest;
import com.example.demo.dto.RoomResponse;
import com.example.demo.entity.Equipment;
import com.example.demo.entity.Room;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.EquipmentRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;

    public RoomService(RoomRepository roomRepository, EquipmentRepository equipmentRepository,
                       ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<RoomResponse> findAll() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RoomResponse findById(Long id) {
        return toResponse(findRoom(id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Room room = new Room();
        applyRequest(room, request);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = findRoom(id);
        applyRequest(room, request);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public void delete(Long id) {
        Room room = findRoom(id);
        if (reservationRepository.existsByRoom_Id(id)) {
            throw new ConflictException("Cette salle est encore référencée par une réservation.");
        }
        roomRepository.delete(room);
        roomRepository.flush();
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + id));
    }

    private void applyRequest(Room room, RoomRequest request) {
        Set<Equipment> equipment = new HashSet<>();
        for (Long equipmentId : request.equipmentIds()) {
            equipment.add(equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + equipmentId)));
        }
        room.setName(request.name().trim());
        room.setBuilding(request.building().trim());
        room.setCapacity(request.capacity());
        room.setDescription(request.description());
        if (request.active() != null) {
            room.setActive(request.active());
        }
        room.getEquipment().clear();
        room.getEquipment().addAll(equipment);
    }

    private RoomResponse toResponse(Room room) {
        Set<Long> equipmentIds = new TreeSet<>();
        for (Equipment equipment : room.getEquipment()) {
            equipmentIds.add(equipment.getId());
        }
        return new RoomResponse(room.getId(), room.getName(), room.getBuilding(), room.getCapacity(),
                room.getDescription(), room.isActive(), equipmentIds);
    }
}
