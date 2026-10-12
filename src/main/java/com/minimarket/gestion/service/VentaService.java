package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ItemVentaRequest;
import com.minimarket.gestion.dto.VentaRequest;
import com.minimarket.gestion.dto.VentaResponse;
import com.minimarket.gestion.entity.Cliente;
import com.minimarket.gestion.entity.DetalleVenta;
import com.minimarket.gestion.entity.EstadoCaja;
import com.minimarket.gestion.entity.MetodoPago;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.TurnoCaja;
import com.minimarket.gestion.entity.Usuario;
import com.minimarket.gestion.entity.Venta;
import com.minimarket.gestion.repository.ClienteRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import com.minimarket.gestion.repository.TurnoCajaRepository;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.repository.VentaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final InventarioService inventarioService;
    private final TurnoCajaRepository turnoCajaRepository;

    @Transactional
    public VentaResponse registrarVenta(VentaRequest request, String emailVendedor) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un producto.");
        }

        Usuario vendedor = usuarioRepository.findByEmail(emailVendedor)
                .orElseThrow(() -> new EntityNotFoundException("Usuario vendedor no encontrado con email: " + emailVendedor));

        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId()).orElse(null);
        }

        Map<Long, Integer> itemsConsolidados = new LinkedHashMap<>();
        for (ItemVentaRequest item : request.getItems()) {
            if (item.getProductoId() == null) {
                throw new IllegalArgumentException("El ID del producto es obligatorio en cada ítem.");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0 para cada producto.");
            }
            itemsConsolidados.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }

        MetodoPago metodoPago = request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPago.EFECTIVO;
        TurnoCaja turnoActivo = turnoCajaRepository
                .findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc(emailVendedor, EstadoCaja.ABIERTA)
                .orElse(null);

        Venta venta = Venta.builder()
                .usuario(vendedor)
                .cliente(cliente)
                .fecha(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .metodoPago(metodoPago)
                .turnoCaja(turnoActivo)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : itemsConsolidados.entrySet()) {
            Long productoId = entry.getKey();
            Integer cantidad = entry.getValue();

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));

            inventarioService.descontarStockPorVencimiento(producto.getId(), cantidad);

            BigDecimal subtotal = producto.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);

            total = total.add(subtotal);

            detalles.add(DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecioUnitario())
                    .build());
        }

        venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        venta.setDetalles(detalles);
        venta = ventaRepository.save(venta);

        return aResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventaRepository.findAll().stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + id));
        return aResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarMisVentas(String emailVendedor) {
        return ventaRepository.findByUsuario_EmailOrderByFechaDesc(emailVendedor).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public VentaResponse registrarCompraCliente(VentaRequest request, String emailCliente) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito debe contener al menos un producto.");
        }

        String emailNormalizado = emailCliente.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new EntityNotFoundException("Usuario cliente no encontrado con email: " + emailCliente));

        Cliente cliente = clienteRepository.findByUsuario_Email(emailNormalizado)
                .or(() -> clienteRepository.findByEmail(emailNormalizado))
                .orElse(null);

        Map<Long, Integer> itemsConsolidados = new LinkedHashMap<>();
        for (ItemVentaRequest item : request.getItems()) {
            if (item.getProductoId() == null) {
                throw new IllegalArgumentException("El ID del producto es obligatorio en cada ítem.");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0 para cada producto.");
            }
            itemsConsolidados.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }

        MetodoPago metodoPago = request.getMetodoPago() != null ? request.getMetodoPago() : MetodoPago.TARJETA_DEBITO;

        Venta venta = Venta.builder()
                .usuario(usuario)
                .cliente(cliente)
                .fecha(LocalDateTime.now())
                .total(BigDecimal.ZERO)
                .metodoPago(metodoPago)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : itemsConsolidados.entrySet()) {
            Long productoId = entry.getKey();
            Integer cantidad = entry.getValue();

            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));

            inventarioService.descontarStockPorVencimiento(producto.getId(), cantidad);

            BigDecimal subtotal = producto.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);

            total = total.add(subtotal);

            detalles.add(DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecioUnitario())
                    .build());
        }

        venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        venta.setDetalles(detalles);
        venta = ventaRepository.save(venta);

        return aResponse(venta);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarMisCompras(String emailCliente) {
        return ventaRepository.buscarHistorialCliente(emailCliente.trim().toLowerCase()).stream()
                .map(this::aResponse)
                .toList();
    }

    private VentaResponse aResponse(Venta venta) {
        List<VentaResponse.DetalleVentaResponse> detalles = venta.getDetalles().stream()
                .map(d -> {
                    BigDecimal subtotal = d.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(d.getCantidad()))
                            .setScale(2, RoundingMode.HALF_UP);

                    return VentaResponse.DetalleVentaResponse.builder()
                            .productoId(d.getProducto().getId())
                            .sku(d.getProducto().getSku())
                            .productoNombre(d.getProducto().getNombre())
                            .imagenUrl(d.getProducto().getImagenUrl())
                            .categoriaNombre(d.getProducto().getCategoria() != null ? d.getProducto().getCategoria().getNombre() : "General")
                            .cantidad(d.getCantidad())
                            .precioUnitario(d.getPrecioUnitario())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        return VentaResponse.builder()
                .id(venta.getId())
                .vendedor(venta.getUsuario().getNombre())
                .clienteId(venta.getCliente() != null ? venta.getCliente().getId() : null)
                .clienteNombre(venta.getCliente() != null ? venta.getCliente().getNombreCompleto() : "Público General")
                .clienteDocumento(venta.getCliente() != null ? venta.getCliente().getNumeroDocumento() : "-")
                .fecha(venta.getFecha())
                .total(venta.getTotal())
                .metodoPago(venta.getMetodoPago() != null ? venta.getMetodoPago() : MetodoPago.EFECTIVO)
                .turnoCajaId(venta.getTurnoCaja() != null ? venta.getTurnoCaja().getId() : null)
                .detalles(detalles)
                .build();
    }
}
