package com.fluxora.service;

import com.fluxora.entity.User;
import com.fluxora.exception.EmailAlreadyRegisteredException;
import com.fluxora.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
    }
    @Transactional
    public User register(String name, String email, String password){
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)){
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        String passwordHash = passwordEncoder.encode(password);

        User user = new User(name, normalizedEmail, passwordHash);

        return userRepository.save(user);
    }
}
