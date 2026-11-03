package com.example.demo.repository;

import com.example.demo.entity.Equipment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
