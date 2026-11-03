package com.example.demo.repository;

import com.example.demo.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);
}
