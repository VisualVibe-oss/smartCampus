package com.example.demo.repository;

import com.example.demo.entity.Room;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByEquipment_Id(UUID equipmentId);

    boolean existsByBuildingAndCode(String building, String code);

    boolean existsByBuildingAndCodeAndIdNot(String building, String code, UUID id);
}
