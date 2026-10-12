package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.CompraRequest;
import com.minimarket.gestion.dto.CompraResponse;
import com.minimarket.gestion.dto.ItemCompraRequest;
import com.minimarket.gestion.entity.*;
import com.minimarket.gestion.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LoteInventarioRepository loteRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private CompraService compraService;

    private Usuario admin;
    private Proveedor proveedor;
    private Producto producto;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder()
                .id(1L)
                .nombre("Admin General")
                .email("admin@minimarket.com")
                .rol(Rol.ADMIN)
                .build();

        proveedor = Proveedor.builder()
                .id(10L)
                .nombre("Distribuidora Gloria S.A.")
                .ruc("20100012345")
                .contacto("ventas@gloria.com")
                .build();

        producto = Producto.builder()
                .id(100L)
                .sku("LEC-GLO-01")
                .nombre("Leche Gloria Azul 400g")
                .precioUnitario(new BigDecimal("4.20"))
                .build();
    }

    @Test
    void testRegistrarCompraExitoso() {
        ItemCompraRequest item = ItemCompraRequest.builder()
                .productoId(100L)
                .cantidad(24)
                .costoUnitario(new BigDecimal("3.10"))
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .build();

        CompraRequest request = CompraRequest.builder()
                .proveedorId(10L)
                .numeroComprobante("F001-000456")
                .tipoComprobante("FACTURA")
                .observaciones("Compra mensual de lácteos")
                .items(List.of(item))
                .build();

        when(usuarioRepository.findByEmail("admin@minimarket.com")).thenReturn(Optional.of(admin));
        when(proveedorRepository.findById(10L)).thenReturn(Optional.of(proveedor));
        when(compraRepository.existsByNumeroComprobante("F001-000456")).thenReturn(false);
        when(productoRepository.findById(100L)).thenReturn(Optional.of(producto));

        LoteInventario loteMock = LoteInventario.builder()
                .id(50L)
                .producto(producto)
                .cantidad(24)
                .fechaIngreso(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .build();

        when(loteRepository.save(any(LoteInventario.class))).thenReturn(loteMock);
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CompraResponse response = compraService.registrarCompra(request, "admin@minimarket.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("F001-000456", response.getNumeroComprobante());
        assertEquals(new BigDecimal("74.40"), response.getTotal()); // 24 * 3.10 = 74.40
        assertEquals(1, response.getDetalles().size());
        assertEquals(EstadoCompra.REGISTRADA, response.getEstado());

        verify(loteRepository).save(any(LoteInventario.class));
        verify(movimientoRepository).save(any(MovimientoInventario.class));
        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void testRegistrarCompraComprobanteDuplicadoLanzaError() {
        CompraRequest request = CompraRequest.builder()
                .proveedorId(10L)
                .numeroComprobante("F001-000456")
                .tipoComprobante("FACTURA")
                .items(List.of(ItemCompraRequest.builder().productoId(100L).cantidad(5).costoUnitario(BigDecimal.ONE).fechaVencimiento(LocalDate.now().plusDays(10)).build()))
                .build();

        when(usuarioRepository.findByEmail("admin@minimarket.com")).thenReturn(Optional.of(admin));
        when(proveedorRepository.findById(10L)).thenReturn(Optional.of(proveedor));
        when(compraRepository.existsByNumeroComprobante("F001-000456")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> compraService.registrarCompra(request, "admin@minimarket.com"));
        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void testRegistrarCompraSinItemsLanzaError() {
        CompraRequest request = CompraRequest.builder()
                .proveedorId(10L)
                .numeroComprobante("F001-000456")
                .tipoComprobante("FACTURA")
                .items(List.of())
                .build();

        assertThrows(IllegalArgumentException.class, () -> compraService.registrarCompra(request, "admin@minimarket.com"));
    }
}
