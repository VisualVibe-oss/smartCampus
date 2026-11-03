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
import java.util.UUID;
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

    public RoomResponse findById(UUID id) {
        return toResponse(findRoom(id));
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Room room = new Room();
        applyRequest(room, request, null);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public RoomResponse update(UUID id, RoomRequest request) {
        Room room = findRoom(id);
        applyRequest(room, request, id);
        return toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public void delete(UUID id) {
        Room room = findRoom(id);
        if (reservationRepository.existsByRoom_Id(id)) {
            throw new ConflictException("Cette salle est encore référencée par une réservation.");
        }
        roomRepository.delete(room);
        roomRepository.flush();
    }

    private Room findRoom(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + id));
    }

    private void applyRequest(Room room, RoomRequest request, UUID currentId) {
        String code = request.code().trim();
        String building = request.building().trim();
        boolean codeTaken = currentId == null
                ? roomRepository.existsByBuildingAndCode(building, code)
                : roomRepository.existsByBuildingAndCodeAndIdNot(building, code, currentId);
        if (codeTaken) {
            throw new ConflictException("Une salle utilise déjà ce code dans ce bâtiment.");
        }
        Set<Equipment> equipment = new HashSet<>();
        for (UUID equipmentId : request.equipmentIds()) {
            equipment.add(equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + equipmentId)));
        }
        room.setCode(code);
        room.setName(request.name().trim());
        room.setBuilding(building);
        room.setCapacity(request.capacity());
        if (request.active() != null) {
            room.setActive(request.active());
        }
        room.getEquipment().clear();
        room.getEquipment().addAll(equipment);
    }

    private RoomResponse toResponse(Room room) {
        Set<UUID> equipmentIds = new TreeSet<>();
        for (Equipment equipment : room.getEquipment()) {
            equipmentIds.add(equipment.getId());
        }
        return new RoomResponse(room.getId(), room.getCode(), room.getName(), room.getBuilding(),
                room.getCapacity(), room.isActive(), equipmentIds);
    }
}
