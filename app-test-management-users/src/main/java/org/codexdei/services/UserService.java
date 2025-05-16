package org.codexdei.services;

import org.codexdei.models.User;

import java.util.List;

public interface UserService {

    User createUser(User user);
    User getUserById(Long id);
    List<User> getAllUsers();
    User updateUser(Long id, User userDetails);
    void deleteUser(Long id);
    boolean activateUser(Long id);
    List<User> findActiveUsers();
}
