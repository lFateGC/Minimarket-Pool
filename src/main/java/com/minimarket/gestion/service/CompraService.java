package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.CompraRequest;
import com.minimarket.gestion.dto.CompraResponse;
import com.minimarket.gestion.dto.DetalleCompraResponse;
import com.minimarket.gestion.dto.ItemCompraRequest;
import com.minimarket.gestion.entity.*;
import com.minimarket.gestion.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteInventarioRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Transactional
    public CompraResponse registrarCompra(CompraRequest request, String emailUsuario) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La compra debe incluir al menos un producto.");
        }

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con email: " + emailUsuario));

        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + request.getProveedorId()));

        if (compraRepository.existsByNumeroComprobante(request.getNumeroComprobante().trim())) {
            throw new IllegalArgumentException("Ya existe una compra registrada con el comprobante: " + request.getNumeroComprobante());
        }

        Compra compra = Compra.builder()
                .numeroComprobante(request.getNumeroComprobante().trim())
                .tipoComprobante(request.getTipoComprobante().trim().toUpperCase())
                .proveedor(proveedor)
                .usuario(usuario)
                .fecha(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .estado(EstadoCompra.REGISTRADA)
                .observaciones(request.getObservaciones())
                .detalles(new ArrayList<>())
                .build();

        BigDecimal totalCompra = BigDecimal.ZERO;

        for (ItemCompraRequest item : request.getItems()) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
            }
            if (item.getCostoUnitario() == null || item.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El costo unitario debe ser mayor a 0.");
            }
            if (item.getFechaVencimiento() == null) {
                throw new IllegalArgumentException("La fecha de vencimiento es obligatoria.");
            }
            if (item.getFechaVencimiento().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a hoy.");
            }

            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + item.getProductoId()));

            // Crea lote en inventario
            LoteInventario nuevoLote = LoteInventario.builder()
                    .producto(producto)
                    .cantidad(item.getCantidad())
                    .fechaIngreso(LocalDate.now())
                    .fechaVencimiento(item.getFechaVencimiento())
                    .build();
            nuevoLote = loteRepository.save(nuevoLote);

            // Registra movimiento Kardex
            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .lote(nuevoLote)
                    .tipo(TipoMovimiento.ENTRADA)
                    .cantidad(item.getCantidad())
                    .fecha(LocalDateTime.now())
                    .motivo("Ingreso por compra a proveedor - " + compra.getTipoComprobante() + " " + compra.getNumeroComprobante())
                    .build();
            movimientoRepository.save(movimiento);

            BigDecimal costoUnitario = item.getCostoUnitario().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = costoUnitario.multiply(BigDecimal.valueOf(item.getCantidad())).setScale(2, RoundingMode.HALF_UP);
            totalCompra = totalCompra.add(subtotal);

            DetalleCompra detalle = DetalleCompra.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(item.getCantidad())
                    .costoUnitario(costoUnitario)
                    .subtotal(subtotal)
                    .fechaVencimiento(item.getFechaVencimiento())
                    .lote(nuevoLote)
                    .build();

            compra.getDetalles().add(detalle);
        }

        compra.setTotal(totalCompra.setScale(2, RoundingMode.HALF_UP));
        compra = compraRepository.save(compra);

        return aResponse(compra);
    }

    @Transactional(readOnly = true)
    public List<CompraResponse> listarCompras() {
        return compraRepository.findAllByOrderByFechaDesc().stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompraResponse obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrada con ID: " + id));
        return aResponse(compra);
    }

    private CompraResponse aResponse(Compra compra) {
        List<DetalleCompraResponse> detalles = compra.getDetalles().stream()
                .map(d -> DetalleCompraResponse.builder()
                        .id(d.getId())
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .productoSku(d.getProducto().getSku())
                        .cantidad(d.getCantidad())
                        .costoUnitario(d.getCostoUnitario())
                        .subtotal(d.getSubtotal())
                        .fechaVencimiento(d.getFechaVencimiento())
                        .loteId(d.getLote() != null ? d.getLote().getId() : null)
                        .build())
                .toList();

        return CompraResponse.builder()
                .id(compra.getId())
                .numeroComprobante(compra.getNumeroComprobante())
                .tipoComprobante(compra.getTipoComprobante())
                .proveedorId(compra.getProveedor().getId())
                .proveedorRazonSocial(compra.getProveedor().getNombre())
                .proveedorRuc(compra.getProveedor().getRuc())
                .usuarioId(compra.getUsuario().getId())
                .usuarioNombre(compra.getUsuario().getNombre())
                .fecha(compra.getFecha())
                .total(compra.getTotal())
                .estado(compra.getEstado())
                .observaciones(compra.getObservaciones())
                .detalles(detalles)
                .build();
    }
}
