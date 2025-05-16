package org.codexdei.services;

import org.codexdei.exceptions.UserNotFoundException;
import org.codexdei.models.User;
import org.codexdei.repositories.UserRepository;

import java.util.List;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Constructor con inyección de dependencias para facilitar el mockeo
     */
    public UserServiceImpl(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public User createUser(User user) {
        // Validaciones de negocio
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new IllegalArgumentException("The user's email is required");
        }

        // Verificar si ya existe un usuario con ese email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A user with that email already exists");
        }

        // Guardar el usuario
        User savedUser = userRepository.save(user);

        // Enviar email de bienvenida
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());

        return savedUser;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User existingUser = getUserById(id);

        // Actualizar propiedades
        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setActive(userDetails.isActive());

        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(Long id) {
        // Verificar que el usuario existe
        User user = getUserById(id);

        // Eliminar el usuario
        userRepository.delete(user);

        // Notificar por email la eliminación de la cuenta
        emailService.sendAccountDeletedEmail(user.getEmail(), user.getName());
    }

    @Override
    public boolean activateUser(Long id) {
        User user = getUserById(id);

        if (user.isActive()) {
            return false; // Usuario ya está activo
        }

        user.setActive(true);
        userRepository.save(user);
        emailService.sendActivationEmail(user.getEmail(), user.getName());

        return true;
    }

    @Override
    public List<User> findActiveUsers() {
        return userRepository.findByActive(true);
    }

}
