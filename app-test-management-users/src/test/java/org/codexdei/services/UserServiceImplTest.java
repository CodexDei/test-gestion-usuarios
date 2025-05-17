package org.codexdei.services;

import org.codexdei.exceptions.UserNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    //Consideraciones: Nunca usar matchers en thenReturn, solo en el when(o el metodo que llame) y verify

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private UserServiceImpl userService;
    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<String> emailCaptor;

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
    //Cuando no encuentra el usuario en la busqueda por id

    @Test
    void testGetUserById_NotFound() {
        //Arrange:
        //Usamos thenThrow para que soporte la excepcion
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        //Act and Assert:
        Exception exception = assertThrows(UserNotFoundException.class, () -> {

            userService.getUserById(2L);
        });
        assertTrue(exception.getMessage().contains("User not found with ID: 2"));
        //Verify
        //Verificamos que la busqueda solo se haga una vez
        verify(userRepository,times(1)).findById(2L);
    }

    @Test
    void testGetAllUsers_success() {
        //Arrange:
        when(userRepository.findAll()).thenReturn(userList);
        //Act:
        List<User> resultUserList = userService.getAllUsers();
        //Assert:
        //no sea una lista nula
        assertNotNull(resultUserList);
        //tengan el mismo tamaño
        assertEquals(userList.size(),resultUserList.size());
        //los datos coincidan
        assertEquals(userList,resultUserList);
        //Verify
        //Se llame al metodo finAll una sola vez
        verify(userRepository, times(1)).findAll();
        //no hallan mas interacciones
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testGetAllUsers_empty() {
        //Arrange:
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        //Act:
        List<User> resultUserList = userService.getAllUsers();
        //Assert
        assertNotNull(resultUserList);
        assertTrue(resultUserList.isEmpty());
        //Verify
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testUpdateUser_sucess() {
        //Arrange:
        //Para verificar un metodo de actualizacion lo recomendable es usar 3 objetos:
        //el usuario que existe en la BD, el usuario que se actualiza y el usuario guardado
        User userExisting = new User(1L, "user_existing", "user_existing@email.com", true);
        User userUpdated = new User(1L, "user_updated", "user_updated@email.com", false);
        User userSaved = new User(1L, "user_updated", "user_updated@email.com", false);
        //No se usa getUserById porque NUNCA se prueban metodos del objeto a evaludar, es decir
        //no se prueba el objeto de inyeccion el cual es userService, por tanto nunguno de sus metodos
        when(userRepository.findById(1L)).thenReturn(Optional.of(userExisting));
        when(userRepository.save(any(User.class))).thenReturn(userSaved);
        //Act
        User resultUser = userService.updateUser(1L,userUpdated);
        //Assert:
        assertNotNull(resultUser);
        assertEquals("user_updated",resultUser.getName());
        assertEquals("user_updated@email.com",resultUser.getEmail());
        assertFalse(resultUser.isActive());
        //Verify:
        verify(userRepository,times(1)).findById(1L);
        verify(userRepository).save(userCaptor.capture());
        User captureUser = userCaptor.getValue();
        assertEquals(1L,captureUser.getId());
        assertEquals("user_updated",captureUser.getName());
        assertEquals("user_updated@email.com",captureUser.getEmail());
        assertFalse(captureUser.isActive());
    }
    //Cuando el usuario a actualizar no existe en la BD

    @Test
    void testUpdateUser_userFindNotExist() {
        //Arrange:
        User userUpdated = new User(1L, "user_updated", "user_updated@email.com", false);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        //Act y Assert:
        Exception exception = assertThrows(UserNotFoundException.class, () -> {
           userService.updateUser(1L,userUpdated);
        });
        assertTrue(exception.getMessage().contains("User not found with ID: 1"));
        //Verify
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(userUpdated);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testDeleteUser_Success() {
        //Arrange:
        when(userRepository.findById(1L)).thenReturn(Optional.of(userTest));
        doNothing().when(userRepository).delete(any(User.class));
        doNothing().when(emailService).sendAccountDeletedEmail(anyString(),anyString());
        //Act:
        userService.deleteUser(1L);
        //Assert
        //Verify:
        verify(userRepository,times(1)).findById(1L);
        //verificamos que se envie el correo correcto y se envie al email correcto
        verify(emailService, times(1)).sendAccountDeletedEmail(emailCaptor.capture(),anyString());
        assertEquals("new@email.com",emailCaptor.getValue());
        //Verificamos que los datos a eliminar correspondan a los del usuario buscado y encontrado
        verify(userRepository, times(1)).delete(userCaptor.capture());
        User captorUser = userCaptor.getValue();
        assertEquals("new_user",userTest.getName());
        assertEquals("new@email.com",userTest.getEmail());
        assertFalse(userTest.isActive());
    }
    //Activacion del usuario exitosa
    @Test
    void testActivateUser_Success() {
        //Arrange:
        User inactiveUser = new User(1L, "new_user", "new@email.com", false);
        User activeUser = new User(1L, "new_user", "new@email.com", true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        doNothing().when(emailService).sendActivationEmail(anyString(),anyString());
        //Act:
        boolean resultActive = userService.activateUser(1L);
        //Assert:
        assertTrue(resultActive);
        //Se encontro el usuario a activar
        verify(userRepository, times(1)).findById(1L);
        //Verificamos que el usuario guardado quedo activo
        verify(userRepository,times(1)).save(userCaptor.capture());
        User captureUser = userCaptor.getValue();
        assertEquals("new@email.com", captureUser.getEmail());
        assertTrue(captureUser.isActive());
        //Verificamos se envie el correo de activacion
        verify(emailService, times(1)).sendActivationEmail(emailCaptor.capture(),anyString());
        assertEquals("new@email.com",emailCaptor.getValue());
    }
    //Cuando el usuario que se pretende activar ya esta activo

    @Test
    void testActivateUser_AlreadyActive() {
        //Arrange:
        User activeUser = new User(1L,"user","email@gmail.com",true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        //Act:
        boolean resultActive = userService.activateUser(1L);
        //Assert:
        assertFalse(resultActive);
        //Verify:
        //verificamos se haga la búsqueda
        verify(userRepository,times(1)).findById(1L);
        //verificamos que nunca se guarde el usuario
        verify(userRepository, never()).save(any(User.class));
        //verificamos nunca se envie el correo de activacion
        verify(emailService, never()).sendActivationEmail(anyString(),anyString());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testFindActiveUser() {
        //Arrange:
        User activeUser1 = new User(1L, "new_user", "new@email.com", true);
        User activeUser2 = new User(2L, "Marye", "marye@email.com", true);
        User activeUser3 = new User(3L, "Samy", "samy@email.com", true);
        List<User> usersListActives = Arrays.asList(activeUser1,activeUser2,activeUser3);

        when(userRepository.findByActive(true)).thenReturn(usersListActives);
        //Act:
        List<User> resultUsers = userService.findActiveUsers();
        //Assert:
        //No puede devolver una lista nula
        assertNotNull(resultUsers);
        assertEquals(3L,resultUsers.size());
        assertEquals(usersListActives,resultUsers);
        //Verify:
        verify(userRepository, times(1)).findByActive(true);
    }
}
