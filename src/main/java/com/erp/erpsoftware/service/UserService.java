package com.erp.erpsoftware.service;

import com.erp.erpsoftware.entity.User;
import com.erp.erpsoftware.repository.UserRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;
    @Setter
    @Getter
    @Autowired
    private PasswordEncoder passwordEncoder;
;
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User saveUser(User user) {

        Date now = new Date();

        if (user.getUserId() == null || user.getUserId() == 0) {

            // New User
            user.setEntryDate(now);
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

        } else {

            // Existing User
            User existing = repository.findById(user.getUserId()).orElseThrow();

            user.setEntryDate(existing.getEntryDate());
            
            // Handle Password: if not provided or empty, keep existing.
            // If provided and changed (and not already hashed), encode it.
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(existing.getPassword());
            } else if (!user.getPassword().equals(existing.getPassword())) {
                String rawPassword = user.getPassword();
                if (!rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$") && !rawPassword.startsWith("$2y$")) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                }
            }
        }

        // Always update modified date
        user.setModifiedDate(now);

        return repository.save(user);
    }

    public User getUserById(Integer id) {
        return repository.findById(id).orElseThrow();
    }

    public void deleteUser(Integer id) {
        repository.deleteById(id);
    }

    public Object getTypeById() {
            return null;
    }

    public User saveType(User user) {
        return repository.save(user);
    }

    public void deleteType(Integer id) {
        repository.deleteById(id);
    }
}