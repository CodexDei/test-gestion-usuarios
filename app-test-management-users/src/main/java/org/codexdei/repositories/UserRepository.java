package org.codexdei.repositories;

import org.codexdei.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void delete(User user);
    List<User> findByActive(boolean active);
}
