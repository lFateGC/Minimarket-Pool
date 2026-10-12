package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.VentaRequest;
import com.minimarket.gestion.dto.VentaResponse;
import com.minimarket.gestion.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request,
                                                   Authentication authentication) {
        String emailVendedor = authentication.getName();
        VentaResponse response = ventaService.registrarVenta(request, emailVendedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return ventaService.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtenerPorId(@PathVariable Long id) {
        return ventaService.obtenerPorId(id);
    }

    @GetMapping("/mis-ventas")
    public List<VentaResponse> misVentas(Authentication authentication) {
        return ventaService.listarMisVentas(authentication.getName());
    }

    @PostMapping("/mi-compra")
    public ResponseEntity<VentaResponse> registrarMiCompra(@Valid @RequestBody VentaRequest request,
                                                           Authentication authentication) {
        String emailCliente = authentication.getName();
        VentaResponse response = ventaService.registrarCompraCliente(request, emailCliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-compras")
    public List<VentaResponse> misCompras(Authentication authentication) {
        return ventaService.listarMisCompras(authentication.getName());
    }
}
