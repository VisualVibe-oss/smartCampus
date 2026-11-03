package com.example.demo.repository;

import com.example.demo.entity.AuthSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    Optional<AuthSession> findByTokenHash(String tokenHash);

    long deleteByUser_Id(Long userId);

    long deleteByExpiresAtBefore(Instant cutoff);
}
