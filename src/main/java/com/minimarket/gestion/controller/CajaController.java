package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.AperturaCajaRequest;
import com.minimarket.gestion.dto.CierreCajaRequest;
import com.minimarket.gestion.dto.ResumenCajaResponse;
import com.minimarket.gestion.dto.TurnoCajaResponse;
import com.minimarket.gestion.service.CajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/abrir")
    public ResponseEntity<TurnoCajaResponse> abrir(@Valid @RequestBody AperturaCajaRequest request,
                                                   Authentication authentication) {
        TurnoCajaResponse response = cajaService.abrirTurno(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/estado-actual")
    public ResponseEntity<TurnoCajaResponse> obtenerTurnoActivo(Authentication authentication) {
        return ResponseEntity.ok(cajaService.obtenerTurnoActivo(authentication.getName()));
    }

    @GetMapping("/resumen-actual")
    public ResponseEntity<ResumenCajaResponse> obtenerResumenActual(Authentication authentication) {
        return ResponseEntity.ok(cajaService.obtenerResumenActual(authentication.getName()));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<TurnoCajaResponse> cerrar(@Valid @RequestBody CierreCajaRequest request,
                                                    Authentication authentication) {
        TurnoCajaResponse response = cajaService.cerrarTurno(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<TurnoCajaResponse>> listarHistorial(Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ResponseEntity.ok(cajaService.listarHistorial(authentication.getName(), esAdmin));
    }
}
