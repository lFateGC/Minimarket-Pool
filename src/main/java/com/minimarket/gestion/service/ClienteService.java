package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ClienteRequest;
import com.minimarket.gestion.dto.ClienteResponse;
import com.minimarket.gestion.entity.Cliente;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.exception.ResourceNotFoundException;
import com.minimarket.gestion.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::aResponse)
                .toList();
    }

    public List<ClienteResponse> listarActivos() {
        return clienteRepository.findByActivoTrueOrderByNombresAsc().stream()
                .map(this::aResponse)
                .toList();
    }

    public ClienteResponse obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return aResponse(cliente);
    }

    public ClienteResponse obtenerPorDocumento(String numeroDocumento) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con documento: " + numeroDocumento));
        return aResponse(cliente);
    }

    public ClienteResponse obtenerMiPerfil(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        Cliente cliente = clienteRepository.findByUsuario_Email(emailNormalizado)
                .or(() -> clienteRepository.findByEmail(emailNormalizado))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el perfil de cliente para la cuenta: " + email));
        return aResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizarMiPerfil(String email, ClienteRequest request) {
        String emailNormalizado = email.trim().toLowerCase();
        Cliente cliente = clienteRepository.findByUsuario_Email(emailNormalizado)
                .or(() -> clienteRepository.findByEmail(emailNormalizado))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el perfil de cliente para la cuenta: " + email));

        if (request.getNombres() != null && !request.getNombres().isBlank()) {
            cliente.setNombres(request.getNombres().trim());
        }
        if (request.getApellidos() != null) {
            cliente.setApellidos(request.getApellidos().trim());
        }
        if (request.getTelefono() != null) {
            cliente.setTelefono(request.getTelefono().trim());
        }
        if (request.getDireccion() != null) {
            cliente.setDireccion(request.getDireccion().trim());
        }

        cliente = clienteRepository.save(cliente);
        return aResponse(cliente);
    }

    public List<ClienteResponse> buscar(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return listarActivos();
        }
        return clienteRepository.buscarPorFiltro(filtro.trim()).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String numDoc = request.getNumeroDocumento().trim();
        if (clienteRepository.existsByNumeroDocumento(numDoc)) {
            throw new BusinessException("Ya existe un cliente registrado con el documento: " + numDoc);
        }

        Cliente cliente = Cliente.builder()
                .tipoDocumento(request.getTipoDocumento().toUpperCase().trim())
                .numeroDocumento(numDoc)
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos() != null ? request.getApellidos().trim() : null)
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null)
                .telefono(request.getTelefono() != null && !request.getTelefono().isBlank() ? request.getTelefono().trim() : null)
                .direccion(request.getDireccion() != null && !request.getDireccion().isBlank() ? request.getDireccion().trim() : null)
                .activo(true)
                .build();

        cliente = clienteRepository.save(cliente);
        return aResponse(cliente);
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        String numDoc = request.getNumeroDocumento().trim();
        if (clienteRepository.existsByNumeroDocumentoAndIdNot(numDoc, id)) {
            throw new BusinessException("El documento '" + numDoc + "' ya está registrado por otro cliente.");
        }

        cliente.setTipoDocumento(request.getTipoDocumento().toUpperCase().trim());
        cliente.setNumeroDocumento(numDoc);
        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(request.getApellidos() != null ? request.getApellidos().trim() : null);
        cliente.setEmail(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null);
        cliente.setTelefono(request.getTelefono() != null && !request.getTelefono().isBlank() ? request.getTelefono().trim() : null);
        cliente.setDireccion(request.getDireccion() != null && !request.getDireccion().isBlank() ? request.getDireccion().trim() : null);

        cliente = clienteRepository.save(cliente);
        return aResponse(cliente);
    }

    @Transactional
    public ClienteResponse cambiarEstado(Long id, boolean activo) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        cliente.setActivo(activo);
        cliente = clienteRepository.save(cliente);
        return aResponse(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));

        long ventas = clienteRepository.contarComprasPorClienteId(id);
        if (ventas > 0) {
            cliente.setActivo(false);
            clienteRepository.save(cliente);
        } else {
            clienteRepository.delete(cliente);
        }
    }

    private ClienteResponse aResponse(Cliente cliente) {
        long compras = clienteRepository.contarComprasPorClienteId(cliente.getId());
        return ClienteResponse.builder()
                .id(cliente.getId())
                .tipoDocumento(cliente.getTipoDocumento())
                .numeroDocumento(cliente.getNumeroDocumento())
                .nombres(cliente.getNombres())
                .apellidos(cliente.getApellidos())
                .nombreCompleto(cliente.getNombreCompleto())
                .email(cliente.getEmail())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getFechaRegistro())
                .totalCompras(compras)
                .build();
    }
}
