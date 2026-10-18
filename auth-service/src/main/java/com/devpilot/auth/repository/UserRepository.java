package com.devpilot.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devpilot.auth.entity.AppUser;
public interface UserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByUsername(String username);
}
