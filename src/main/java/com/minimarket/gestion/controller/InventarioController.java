package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.EntradaInventarioRequest;
import com.minimarket.gestion.dto.LoteInventarioResponse;
import com.minimarket.gestion.dto.MermaInventarioRequest;
import com.minimarket.gestion.dto.MovimientoInventarioResponse;
import com.minimarket.gestion.entity.LoteInventario;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de inventario, compras de lotes, mermas y Kardex.
 */
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    /**
     * Registra el ingreso de un nuevo lote de compras a almacén.
     */
    @PostMapping("/entradas")
    public ResponseEntity<LoteInventarioResponse> registrarEntrada(@Valid @RequestBody EntradaInventarioRequest request) {
        LoteInventarioResponse response = inventarioService.registrarEntrada(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Registra la salida por merma (baja de producto deteriorado o caducado).
     */
    @PostMapping("/mermas")
    public ResponseEntity<Void> registrarMerma(@Valid @RequestBody MermaInventarioRequest request) {
        inventarioService.registrarMerma(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista todos los lotes activos actualmente en almacén con existencias.
     */
    @GetMapping("/lotes/activos")
    public List<LoteInventarioResponse> listarLotesActivos() {
        return inventarioService.listarLotesActivos();
    }

    /**
     * Lista los lotes disponibles de un producto ordenados por fecha de vencimiento.
     */
    @GetMapping("/lotes/producto/{productoId}")
    public List<LoteInventarioResponse> listarLotesPorProducto(@PathVariable Long productoId) {
        return inventarioService.listarLotesPorProducto(productoId);
    }

    /**
     * Historial de auditoría general de movimientos (Kardex).
     */
    @GetMapping("/movimientos")
    public List<MovimientoInventarioResponse> listarMovimientos() {
        return inventarioService.listarMovimientos();
    }

    /**
     * Reporte de productos que se encuentran por debajo del stock mínimo.
     */
    @GetMapping("/reportes/stock-bajo")
    public List<Producto> stockBajo() {
        return inventarioService.productosConStockBajo();
    }

    /**
     * Reporte de lotes próximos a vencer en los siguientes N días.
     */
    @GetMapping("/reportes/por-vencer")
    public List<LoteInventario> porVencer(@RequestParam(defaultValue = "7") int dias) {
        return inventarioService.lotesProximosAVencer(dias);
    }
}
