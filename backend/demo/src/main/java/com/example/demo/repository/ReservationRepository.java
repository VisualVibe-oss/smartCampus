package com.example.demo.repository;

import com.example.demo.entity.Reservation;
import com.example.demo.entity.ReservationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    boolean existsByRoom_Id(UUID roomId);

    boolean existsByUser_Id(UUID userId);

    @Query("""
            select (count(r) > 0) from Reservation r
            where r.room.id = :roomId
              and r.status = :status
              and r.startAt < :end
              and r.endAt > :start
              and (:excludeId is null or r.id <> :excludeId)
            """)
    boolean existsOverlap(@Param("roomId") UUID roomId,
                          @Param("status") ReservationStatus status,
                          @Param("start") Instant start,
                          @Param("end") Instant end,
                          @Param("excludeId") UUID excludeId);
}
