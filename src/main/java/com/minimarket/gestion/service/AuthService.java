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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public VerificacionDocumentoResponse verificarDocumento(String numeroDocumento) {
        String numDoc = numeroDocumento != null ? numeroDocumento.trim() : "";
        if (numDoc.isBlank()) {
            return VerificacionDocumentoResponse.builder()
                    .registradoEnTienda(false)
                    .tieneCuentaWeb(false)
                    .mensaje("Documento vacío")
                    .build();
        }

        return clienteRepository.findByNumeroDocumento(numDoc)
                .map(cliente -> {
                    boolean tieneWeb = cliente.getUsuario() != null;
                    String mensaje;
                    if (tieneWeb) {
                        mensaje = "Ya cuentas con una cuenta web activa con este documento. Por favor inicia sesión.";
                    } else {
                        mensaje = "¡Hola, " + cliente.getNombres() + "! Detectamos que ya estás registrado en nuestra tienda física. Para activar tus compras y entregas online, solo completa tus datos faltantes (correo, contraseña y dirección).";
                    }

                    return VerificacionDocumentoResponse.builder()
                            .registradoEnTienda(true)
                            .tieneCuentaWeb(tieneWeb)
                            .tipoDocumento(cliente.getTipoDocumento())
                            .numeroDocumento(cliente.getNumeroDocumento())
                            .nombres(cliente.getNombres())
                            .apellidos(cliente.getApellidos())
                            .email(cliente.getEmail())
                            .telefono(cliente.getTelefono())
                            .direccion(cliente.getDireccion())
                            .mensaje(mensaje)
                            .build();
                })
                .orElseGet(() -> VerificacionDocumentoResponse.builder()
                        .registradoEnTienda(false)
                        .tieneCuentaWeb(false)
                        .numeroDocumento(numDoc)
                        .mensaje("Cliente nuevo. Completa tus datos para crear tu cuenta.")
                        .build());
    }

    @Transactional
    public JwtResponse registrarCliente(RegistroClienteRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("Ya existe una cuenta registrada con el correo: " + email);
        }

        String numDoc = request.getNumeroDocumento().trim();
        Optional<Cliente> clienteExistenteOpt = clienteRepository.findByNumeroDocumento(numDoc);

        Usuario usuario;
        Cliente cliente;

        if (clienteExistenteOpt.isPresent()) {
            cliente = clienteExistenteOpt.get();
            if (cliente.getUsuario() != null) {
                throw new BusinessException("El documento " + numDoc + " ya cuenta con un usuario web registrado. Inicia sesión con tu correo.");
            }

            // El cliente ya fue registrado en tienda física (caja/POS). Ahora completa sus datos y crea su usuario web.
            String nombreFinal = (request.getNombres() != null && !request.getNombres().isBlank())
                    ? request.getNombres().trim()
                    : cliente.getNombres();
            String apellidosFinal = (request.getApellidos() != null && !request.getApellidos().isBlank())
                    ? request.getApellidos().trim()
                    : cliente.getApellidos();
            String nombreCompleto = nombreFinal + (apellidosFinal != null && !apellidosFinal.isBlank() ? " " + apellidosFinal : "");

            usuario = Usuario.builder()
                    .nombre(nombreCompleto.trim())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .rol(Rol.CLIENTE)
                    .build();
            usuario = usuarioRepository.save(usuario);

            // Vinculamos y completamos datos faltantes en el cliente existente
            cliente.setUsuario(usuario);
            cliente.setEmail(email);
            if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
                cliente.setTelefono(request.getTelefono().trim());
            }
            if (request.getDireccion() != null && !request.getDireccion().isBlank()) {
                cliente.setDireccion(request.getDireccion().trim());
            }
            if (request.getNombres() != null && !request.getNombres().isBlank()) {
                cliente.setNombres(nombreFinal);
            }
            if (request.getApellidos() != null && !request.getApellidos().isBlank()) {
                cliente.setApellidos(apellidosFinal);
            }
            cliente.setActivo(true);
            cliente = clienteRepository.save(cliente);
        } else {
            // Cliente nuevo en tienda y en web
            String nombreCompleto = request.getNombres().trim() + (request.getApellidos() != null && !request.getApellidos().isBlank() ? " " + request.getApellidos().trim() : "");
            usuario = Usuario.builder()
                    .nombre(nombreCompleto.trim())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .rol(Rol.CLIENTE)
                    .build();
            usuario = usuarioRepository.save(usuario);

            cliente = Cliente.builder()
                    .tipoDocumento(request.getTipoDocumento().toUpperCase().trim())
                    .numeroDocumento(numDoc)
                    .nombres(request.getNombres().trim())
                    .apellidos(request.getApellidos() != null ? request.getApellidos().trim() : null)
                    .email(email)
                    .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                    .direccion(request.getDireccion() != null ? request.getDireccion().trim() : null)
                    .activo(true)
                    .fechaRegistro(LocalDateTime.now())
                    .usuario(usuario)
                    .build();
            cliente = clienteRepository.save(cliente);
        }

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());
        return JwtResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .build();
    }

    public JwtResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol().name());

        return JwtResponse.builder()
                .token(token)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .build();
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
