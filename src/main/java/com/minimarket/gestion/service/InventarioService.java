package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.EntradaInventarioRequest;
import com.minimarket.gestion.dto.LoteInventarioResponse;
import com.minimarket.gestion.dto.MermaInventarioRequest;
import com.minimarket.gestion.dto.MovimientoInventarioResponse;
import com.minimarket.gestion.entity.LoteInventario;
import com.minimarket.gestion.entity.MovimientoInventario;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.TipoMovimiento;
import com.minimarket.gestion.repository.LoteInventarioRepository;
import com.minimarket.gestion.repository.MovimientoInventarioRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final LoteInventarioRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    // RF04: registrar la llegada de un lote nuevo (compra a proveedor)
    @Transactional
    public LoteInventarioResponse registrarEntrada(EntradaInventarioRequest request) {
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad ingresada debe ser mayor a 0");
        }

        if (request.getFechaVencimiento() == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }

        if (request.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha actual");
        }

        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));

        LoteInventario lote = LoteInventario.builder()
                .producto(producto)
                .cantidad(request.getCantidad())
                .fechaIngreso(LocalDate.now())
                .fechaVencimiento(request.getFechaVencimiento())
                .build();
        lote = loteRepository.save(lote);

        movimientoRepository.save(MovimientoInventario.builder()
                .lote(lote)
                .tipo(TipoMovimiento.ENTRADA)
                .cantidad(request.getCantidad())
                .fecha(LocalDateTime.now())
                .motivo("Compra a proveedor / Ingreso de lote #" + lote.getId())
                .build());

        return aLoteResponse(lote);
    }

    @Transactional
    public void registrarMerma(MermaInventarioRequest request) {
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad de merma debe ser mayor a 0");
        }

        LoteInventario lote = loteRepository.findById(request.getLoteId())
                .orElseThrow(() -> new EntityNotFoundException("Lote no encontrado con ID: " + request.getLoteId()));

        if (lote.getCantidad() < request.getCantidad()) {
            throw new IllegalStateException("La cantidad de merma (" + request.getCantidad()
                    + ") supera el stock disponible en el lote (Stock actual: " + lote.getCantidad() + ")");
        }

        lote.setCantidad(lote.getCantidad() - request.getCantidad());
        loteRepository.save(lote);

        movimientoRepository.save(MovimientoInventario.builder()
                .lote(lote)
                .tipo(TipoMovimiento.MERMA)
                .cantidad(request.getCantidad())
                .fecha(LocalDateTime.now())
                .motivo(request.getMotivo().trim())
                .build());
    }

    public List<LoteInventarioResponse> listarLotesActivos() {
        return loteRepository.findAll().stream()
                .filter(l -> l.getCantidad() != null && l.getCantidad() > 0)
                .map(this::aLoteResponse)
                .toList();
    }

    public List<LoteInventarioResponse> listarLotesPorProducto(Long productoId) {
        if (!productoRepository.existsById(productoId)) {
            throw new EntityNotFoundException("Producto no encontrado con ID: " + productoId);
        }
        return loteRepository.findByProducto_IdAndCantidadGreaterThanOrderByFechaVencimientoAsc(productoId, 0).stream()
                .map(this::aLoteResponse)
                .toList();
    }

    public List<MovimientoInventarioResponse> listarMovimientos() {
        return movimientoRepository.findAll().stream()
                .map(m -> MovimientoInventarioResponse.builder()
                        .id(m.getId())
                        .loteId(m.getLote().getId())
                        .productoId(m.getLote().getProducto().getId())
                        .productoNombre(m.getLote().getProducto().getNombre())
                        .tipo(m.getTipo())
                        .cantidad(m.getCantidad())
                        .fecha(m.getFecha())
                        .motivo(m.getMotivo())
                        .build())
                .toList();
    }

    public List<Producto> productosConStockBajo() {
        return productoRepository.findAll().stream()
                .filter(p -> {
                    Integer stock = loteRepository.sumarStockPorProducto(p.getId());
                    return stock < p.getStockMinimo();
                })
                .toList();
    }

    public List<LoteInventario> lotesProximosAVencer(int diasHaciaAdelante) {
        return loteRepository.findLotesProximosAVencer(LocalDate.now().plusDays(diasHaciaAdelante));
    }

    @Transactional
    public void descontarStockPorVencimiento(Long productoId, int cantidadRequerida) {
        if (cantidadRequerida <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
        }

        List<LoteInventario> lotesDisponibles = loteRepository
            .findByProducto_IdAndCantidadGreaterThanAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(
                productoId, 0, LocalDate.now());

        int stockTotal = lotesDisponibles.stream().mapToInt(LoteInventario::getCantidad).sum();
        if (stockTotal < cantidadRequerida) {
            throw new IllegalStateException("Stock insuficiente para el producto (ID=" + productoId
                    + "). Solicitado: " + cantidadRequerida + ", Disponible: " + stockTotal);
        }

        int restante = cantidadRequerida;

        for (LoteInventario lote : lotesDisponibles) {
            if (restante <= 0) break;

            int aDescontar = Math.min(lote.getCantidad(), restante);
            lote.setCantidad(lote.getCantidad() - aDescontar);
            loteRepository.save(lote);

            movimientoRepository.save(MovimientoInventario.builder()
                    .lote(lote)
                    .tipo(TipoMovimiento.SALIDA)
                    .cantidad(aDescontar)
                    .fecha(LocalDateTime.now())
                    .motivo("Venta comercial")
                    .build());

            restante -= aDescontar;
        }
    }

    private LoteInventarioResponse aLoteResponse(LoteInventario lote) {
        return LoteInventarioResponse.builder()
                .id(lote.getId())
                .productoId(lote.getProducto().getId())
                .productoNombre(lote.getProducto().getNombre())
                .sku(lote.getProducto().getSku())
                .cantidad(lote.getCantidad())
                .fechaIngreso(lote.getFechaIngreso())
                .fechaVencimiento(lote.getFechaVencimiento())
                .build();
    }
}
