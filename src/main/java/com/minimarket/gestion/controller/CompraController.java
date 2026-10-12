package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.CompraRequest;
import com.minimarket.gestion.dto.CompraResponse;
import com.minimarket.gestion.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request,
                                                    Authentication authentication) {
        String emailUsuario = authentication.getName();
        CompraResponse response = compraService.registrarCompra(request, emailUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CompraResponse> listar() {
        return compraService.listarCompras();
    }

    @GetMapping("/{id}")
    public CompraResponse obtenerPorId(@PathVariable Long id) {
        return compraService.obtenerPorId(id);
    }
}
