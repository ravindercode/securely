package com.securely.service;

import com.securely.entity.User;
import com.securely.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        
        return userRepository.save(user);
    }

    public User createApiKey(String username, String password) {
        User user = authenticateUser(username, password);
        
        String apiKey = generateApiKey();
        while (userRepository.existsByApiKey(apiKey)) {
            apiKey = generateApiKey();
        }
        
        user.setApiKey(apiKey);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByApiKey(String apiKey) {
        return userRepository.findByApiKey(apiKey);
    }

    public Optional<User> findByOauth2(String provider, String oauth2Id) {
        return userRepository.findByOauth2ProviderAndOauth2Id(provider, oauth2Id);
    }

    public User createOrUpdateOAuth2User(String provider, String oauth2Id, String username) {
        Optional<User> existingUser = userRepository.findByOauth2ProviderAndOauth2Id(provider, oauth2Id);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setOauth2Provider(provider);
        user.setOauth2Id(oauth2Id);
        
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private User authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found: " + username);
        }
        
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password for user: " + username);
        }
        
        return user;
    }

    private String generateApiKey() {
        return "sk-" + UUID.randomUUID().toString().replace("-", "");
    }
}
