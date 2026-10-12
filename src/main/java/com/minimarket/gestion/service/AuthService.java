package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.*;
import com.minimarket.gestion.entity.Cliente;
import com.minimarket.gestion.entity.Rol;
import com.minimarket.gestion.entity.Usuario;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.repository.ClienteRepository;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.security.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UsuarioResponse registrar(RegistroRequest request) {
        String emailNormalizado = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .email(emailNormalizado)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.VENDEDOR)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return aResponse(guardado);
    }

    @Transactional
    public JwtResponse registrarCliente(RegistroClienteRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("Ya existe una cuenta registrada con el correo: " + email);
        }

        String numDoc = request.getNumeroDocumento().trim();
        if (clienteRepository.existsByNumeroDocumento(numDoc)) {
            throw new BusinessException("Ya existe un cliente registrado con el documento: " + numDoc);
        }

        String nombreCompleto = request.getNombres().trim() + (request.getApellidos() != null && !request.getApellidos().isBlank() ? " " + request.getApellidos().trim() : "");
        Usuario usuario = Usuario.builder()
                .nombre(nombreCompleto.trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .build();
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = Cliente.builder()
                .tipoDocumento(request.getTipoDocumento().toUpperCase().trim())
                .numeroDocumento(numDoc)
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos() != null ? request.getApellidos().trim() : null)
                .email(email)
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .direccion(request.getDireccion() != null ? request.getDireccion().trim() : null)
                .activo(true)
                .usuario(usuario)
                .build();
        clienteRepository.save(cliente);

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new JwtResponse(token, usuario.getNombre(), usuario.getRol().name());
    }

    public JwtResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());

        return new JwtResponse(token, usuario.getNombre(), usuario.getRol().name());
    }

    public UsuarioResponse obtenerPerfil(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return aResponse(usuario);
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        String nuevoEmail = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmailAndIdNot(nuevoEmail, id)) {
            throw new BusinessException("Ya existe otro usuario registrado con el email: " + nuevoEmail);
        }

        usuario.setNombre(request.getNombre().trim());
        usuario.setEmail(nuevoEmail);
        usuario.setRol(request.getRol());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        return aResponse(actualizado);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        usuarioRepository.delete(usuario);
    }

    private UsuarioResponse aResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();
    }
}
