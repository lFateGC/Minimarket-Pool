package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ClienteRequest;
import com.minimarket.gestion.dto.ClienteResponse;
import com.minimarket.gestion.entity.Cliente;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("Debe registrar un nuevo cliente exitosamente cuando el documento no existe")
    void testCrearClienteExitoso() {
        // Arrange
        ClienteRequest request = ClienteRequest.builder()
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .apellidos("Mendoza")
                .telefono("987654321")
                .email("carlos@gmail.com")
                .direccion("Av. Brasil 123")
                .build();

        when(clienteRepository.existsByNumeroDocumento("72345678")).thenReturn(false);

        Cliente clienteGuardado = Cliente.builder()
                .id(1L)
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .apellidos("Mendoza")
                .telefono("987654321")
                .email("carlos@gmail.com")
                .direccion("Av. Brasil 123")
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .build();

        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteGuardado);
        when(clienteRepository.contarComprasPorClienteId(1L)).thenReturn(0L);

        // Act
        ClienteResponse response = clienteService.crear(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("72345678", response.getNumeroDocumento());
        assertEquals("Carlos Mendoza", response.getNombreCompleto());
        assertTrue(response.getActivo());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Debe lanzar BusinessException si el documento ya está registrado")
    void testCrearClienteDocumentoDuplicado() {
        // Arrange
        ClienteRequest request = ClienteRequest.builder()
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .build();

        when(clienteRepository.existsByNumeroDocumento("72345678")).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> clienteService.crear(request));
        assertTrue(exception.getMessage().contains("Ya existe un cliente"));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe aplicar baja lógica (desactivar) si el cliente tiene compras registradas")
    void testEliminarClienteConComprasAplicaBajaLogica() {
        // Arrange
        Cliente cliente = Cliente.builder()
                .id(1L)
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .activo(true)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.contarComprasPorClienteId(1L)).thenReturn(3L);

        // Act
        clienteService.eliminar(1L);

        // Assert
        assertFalse(cliente.getActivo());
        verify(clienteRepository, times(1)).save(cliente);
        verify(clienteRepository, never()).delete(cliente);
    }

    @Test
    @DisplayName("Debe eliminar físicamente si el cliente no tiene ventas registradas")
    void testEliminarClienteSinComprasEliminaFisico() {
        // Arrange
        Cliente cliente = Cliente.builder()
                .id(2L)
                .numeroDocumento("12345678")
                .nombres("Cliente Nuevo")
                .activo(true)
                .build();

        when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.contarComprasPorClienteId(2L)).thenReturn(0L);

        // Act
        clienteService.eliminar(2L);

        // Assert
        verify(clienteRepository, times(1)).delete(cliente);
    }

    @Test
    @DisplayName("Debe obtener el perfil del cliente por email de usuario")
    void testObtenerMiPerfilExitoso() {
        // Arrange
        Cliente cliente = Cliente.builder()
                .id(1L)
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .apellidos("Mendoza")
                .email("carlos@gmail.com")
                .telefono("987654321")
                .direccion("Av. Brasil 123")
                .activo(true)
                .build();

        when(clienteRepository.findByUsuario_Email("carlos@gmail.com")).thenReturn(Optional.of(cliente));
        when(clienteRepository.contarComprasPorClienteId(1L)).thenReturn(2L);

        // Act
        ClienteResponse response = clienteService.obtenerMiPerfil("carlos@gmail.com");

        // Assert
        assertNotNull(response);
        assertEquals("72345678", response.getNumeroDocumento());
        assertEquals("Carlos Mendoza", response.getNombreCompleto());
        assertEquals(2L, response.getTotalCompras());
    }

    @Test
    @DisplayName("Debe actualizar datos de perfil del cliente autenticado")
    void testActualizarMiPerfilExitoso() {
        // Arrange
        Cliente cliente = Cliente.builder()
                .id(1L)
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos")
                .apellidos("Mendoza")
                .email("carlos@gmail.com")
                .telefono("987654321")
                .direccion("Av. Brasil 123")
                .activo(true)
                .build();

        ClienteRequest updateRequest = ClienteRequest.builder()
                .tipoDocumento("DNI")
                .numeroDocumento("72345678")
                .nombres("Carlos Eduardo")
                .apellidos("Mendoza Soto")
                .telefono("911222333")
                .direccion("Calle Las Begonias 789")
                .build();

        when(clienteRepository.findByUsuario_Email("carlos@gmail.com")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(clienteRepository.contarComprasPorClienteId(1L)).thenReturn(2L);

        // Act
        ClienteResponse response = clienteService.actualizarMiPerfil("carlos@gmail.com", updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Carlos Eduardo", response.getNombres());
        assertEquals("Mendoza Soto", response.getApellidos());
        assertEquals("911222333", response.getTelefono());
        assertEquals("Calle Las Begonias 789", response.getDireccion());
        verify(clienteRepository).save(cliente);
    }
}
