package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.ClienteRequest;
import com.minimarket.gestion.dto.ClienteResponse;
import com.minimarket.gestion.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Obtener los datos de perfil del cliente actualmente autenticado")
    @GetMapping("/mi-perfil")
    public ClienteResponse obtenerMiPerfil(Authentication authentication) {
        return clienteService.obtenerMiPerfil(authentication.getName());
    }

    @Operation(summary = "Actualizar datos de contacto y entrega del cliente autenticado")
    @PutMapping("/mi-perfil")
    public ClienteResponse actualizarMiPerfil(@Valid @RequestBody ClienteRequest request,
                                              Authentication authentication) {
        return clienteService.actualizarMiPerfil(authentication.getName(), request);
    }

    @Operation(summary = "Listar todos los clientes registrados")
    @GetMapping
    public List<ClienteResponse> listarTodos() {
        return clienteService.listarTodos();
    }

    @Operation(summary = "Listar solo clientes activos")
    @GetMapping("/activos")
    public List<ClienteResponse> listarActivos() {
        return clienteService.listarActivos();
    }

    @Operation(summary = "Obtener un cliente por su ID")
    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable Long id) {
        return clienteService.obtenerPorId(id);
    }

    @Operation(summary = "Buscar cliente por su número de documento (DNI/RUC)")
    @GetMapping("/documento/{numeroDocumento}")
    public ClienteResponse obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return clienteService.obtenerPorDocumento(numeroDocumento);
    }

    @Operation(summary = "Filtrar clientes por nombre, apellido o número de documento")
    @GetMapping("/buscar")
    public List<ClienteResponse> buscar(@RequestParam(required = false) String q) {
        return clienteService.buscar(q);
    }

    @Operation(summary = "Crear un nuevo cliente")
    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Actualizar datos de un cliente existente")
    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @Operation(summary = "Activar o desactivar un cliente")
    @PatchMapping("/{id}/estado")
    public ClienteResponse cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        return clienteService.cambiarEstado(id, activo);
    }

    @Operation(summary = "Eliminar cliente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
