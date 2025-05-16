package org.codexdei.services;

import org.codexdei.models.User;
import org.codexdei.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private UserServiceImpl userService;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User userTest;
    private List<User> userList;

    @BeforeEach
    void setUp() {

        //Configuramos datos de prueba
        userTest = new User(1L, "new_user", "new@email.com", false);
        User user1 = new User(2L, "Marye", "marye@email.com", false);
        User user2 = new User(3L, "Samy", "samy@email.com", true);
        userList = Arrays.asList(user1, user2);
    }

    //test para verificar un usuario creado exitosamente
    @Test
    void testCreateUser_success() {
        //Arrange:
        User newUser = new User(null, "new_user", "new@email.com", false);
        User saveUser = new User(1L, "new_user", "new@email.com", false);

        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saveUser);
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());
        //Act:
        User result = userService.createUser(newUser);
        //Assert:
        //verificamos que no sea nulo
        assertNotNull(result);
        //verificamos que el usuario se creo correctamente
        assertEquals(1L, result.getId());
        //verificamos que el correo guardado es el correcto
        assertEquals("new@email.com", result.getEmail());
        //verificamos que el usuario no estaba
        verify(userRepository, times(1)).findByEmail("new@email.com");
        //verificamos que se llamo a save
        verify(userRepository, times(1)).save(any(User.class));
        //Verificamos que se envie el email correcto
        verify(emailService, times(1)).sendWelcomeEmail(eq("new@email.com"), eq("new_user"));
    }
    //verifica un usuario duplicado

    @Test
    void testCreateUser_DuplicateEmail() {

        User newuser = new User(null,"new_user","existing@email.com",false);
        //Arrange:
        when(userRepository.findByEmail("existing@email.com")).thenReturn(Optional.of(userTest));
        //Act and Assert:
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(newuser));
        //verificamos que el mensaje de la excepcion corresponda
        assertEquals("A user with that email already exists",exception.getMessage());
        //Verify
        //Verificar que devuelve el mismo email
        verify(userRepository).findByEmail("existing@email.com");
        //Nunca debe guardar el usuario si esta en la base de datos
        verify(userRepository, never()).save(any(User.class));
        //Nunca debe enviar un email de bienvenida pues esta ya registrado
        verify(emailService, never()).sendWelcomeEmail(anyString(),anyString());
    }
    //test correo vacio

    @Test
    void testCreateUser_EmailEmpty() {

        //Arrange:
        User userEmpty = new User(null,"new_user","", false);
        //Act and Assert:
        Exception exception = assertThrows(IllegalArgumentException.class,() ->
                userService.createUser(userEmpty));
        //Verficiar mensaje de la excepcion
        assertEquals("The user's email is required",exception.getMessage());
        //Verify
        //
        verify(userRepository, never()).findByEmail("");
        verify(userRepository,never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(),anyString());
    }

    @Test
    void testGetUserById_Success() {
        //Arrange:
        when(userRepository.findById(1L)).thenReturn(Optional.of(userTest));
        //Act:
        User result = userService.getUserById(1L);
        //Assert:
        assertNotNull(result);
        assertEquals(1L,result.getId());
        assertEquals("new@email.com",result.getEmail());
        //Verify:
        verify(userRepository).findById(1L);
    }
}
