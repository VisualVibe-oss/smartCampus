package com.example.demo.service;

import com.example.demo.dto.EquipmentRequest;
import com.example.demo.dto.EquipmentResponse;
import com.example.demo.entity.Equipment;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.EquipmentRepository;
import com.example.demo.repository.RoomRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final RoomRepository roomRepository;

    public EquipmentService(EquipmentRepository equipmentRepository, RoomRepository roomRepository) {
        this.equipmentRepository = equipmentRepository;
        this.roomRepository = roomRepository;
    }

    public List<EquipmentResponse> findAll() {
        return equipmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EquipmentResponse findById(Long id) {
        return toResponse(findEquipment(id));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        String name = request.name().trim();
        if (equipmentRepository.existsByName(name)) {
            throw new ConflictException("Un équipement porte déjà ce nom.");
        }
        Equipment equipment = new Equipment();
        equipment.setName(name);
        return toResponse(equipmentRepository.saveAndFlush(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = findEquipment(id);
        String name = request.name().trim();
        if (equipmentRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Un équipement porte déjà ce nom.");
        }
        equipment.setName(name);
        return toResponse(equipmentRepository.saveAndFlush(equipment));
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = findEquipment(id);
        if (roomRepository.existsByEquipment_Id(id)) {
            throw new ConflictException("Cet équipement est encore associé à une salle.");
        }
        equipmentRepository.delete(equipment);
        equipmentRepository.flush();
    }

    private Equipment findEquipment(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + id));
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getName());
    }
}
