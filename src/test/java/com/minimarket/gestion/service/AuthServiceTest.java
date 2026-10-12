package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.JwtResponse;
import com.minimarket.gestion.dto.LoginRequest;
import com.minimarket.gestion.dto.RegistroRequest;
import com.minimarket.gestion.dto.UsuarioResponse;
import com.minimarket.gestion.entity.Rol;
import com.minimarket.gestion.entity.Usuario;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private com.minimarket.gestion.repository.ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegistrarClienteExitoso() {
        com.minimarket.gestion.dto.RegistroClienteRequest request = com.minimarket.gestion.dto.RegistroClienteRequest.builder()
                .nombres("Ana")
                .apellidos("García")
                .email("ana@gmail.com")
                .password("123456")
                .tipoDocumento("DNI")
                .numeroDocumento("74567890")
                .telefono("999888777")
                .direccion("Av. Sol 456")
                .build();

        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);
        when(clienteRepository.existsByNumeroDocumento("74567890")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encodedPass");

        Usuario usuarioGuardado = Usuario.builder()
                .id(10L)
                .nombre("Ana García")
                .email("ana@gmail.com")
                .rol(Rol.CLIENTE)
                .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(jwtUtil.generarToken("ana@gmail.com", "CLIENTE")).thenReturn("jwt.token.cliente");

        JwtResponse response = authService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("jwt.token.cliente", response.getToken());
        assertEquals("Ana García", response.getNombre());
        assertEquals("CLIENTE", response.getRol());
        verify(clienteRepository, times(1)).save(any(com.minimarket.gestion.entity.Cliente.class));
    }

    @Test
    void testRegistrarUsuarioExitoso() {
        RegistroRequest request = new RegistroRequest("Admin User", "admin@minimarket.com", "123456", Rol.ADMIN);

        when(usuarioRepository.existsByEmail("admin@minimarket.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encodedPassword");
        Usuario usuarioGuardado = Usuario.builder()
            .id(1L)
            .nombre("Admin User")
            .email("admin@minimarket.com")
            .rol(Rol.VENDEDOR)
            .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponse response = authService.registrar(request);

        assertEquals(1L, response.getId());
        assertEquals("Admin User", response.getNombre());
        assertEquals(Rol.VENDEDOR, response.getRol());
    }

    @Test
    void testLoginExitosoGeneraToken() {
        LoginRequest request = new LoginRequest("admin@minimarket.com", "123456");
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Admin User")
                .email("admin@minimarket.com")
                .password("$2a$10$encoded")
                .rol(Rol.ADMIN)
                .build();

        when(usuarioRepository.findByEmail("admin@minimarket.com")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generarToken("admin@minimarket.com", "ADMIN")).thenReturn("jwt.token.mock");

        JwtResponse response = authService.login(request);

        assertEquals("jwt.token.mock", response.getToken());
        assertEquals("Admin User", response.getNombre());
        assertEquals("ADMIN", response.getRol());
    }
}
