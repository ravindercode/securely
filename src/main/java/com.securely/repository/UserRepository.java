package com.securely.repository;

import com.securely.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByApiKey(String apiKey);
    Optional<User> findByOauth2ProviderAndOauth2Id(String provider, String oauth2Id);
    boolean existsByUsername(String username);
    boolean existsByApiKey(String apiKey);
}