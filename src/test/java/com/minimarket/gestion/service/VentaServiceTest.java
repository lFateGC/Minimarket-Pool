package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ItemVentaRequest;
import com.minimarket.gestion.dto.VentaRequest;
import com.minimarket.gestion.dto.VentaResponse;
import com.minimarket.gestion.entity.Cliente;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.Rol;
import com.minimarket.gestion.entity.Usuario;
import com.minimarket.gestion.entity.Venta;
import com.minimarket.gestion.repository.ClienteRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import com.minimarket.gestion.repository.TurnoCajaRepository;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @InjectMocks
    private VentaService ventaService;

    private Usuario vendedorPrueba;
    private Producto producto1;
    private Producto producto2;

    @BeforeEach
    void setUp() {
        vendedorPrueba = Usuario.builder()
                .id(1L)
                .nombre("Carlos Vendedor")
                .email("carlos@minimarket.com")
                .rol(Rol.VENDEDOR)
                .build();

        producto1 = Producto.builder()
                .id(10L)
                .nombre("Galletas Oreo 100g")
                .precioUnitario(new BigDecimal("2.50"))
                .build();

        producto2 = Producto.builder()
                .id(20L)
                .nombre("Agua Mineral 600ml")
                .precioUnitario(new BigDecimal("1.80"))
                .build();
    }

    @Test
        @SuppressWarnings("null")
    void testRegistrarVentaExitoso() {
        VentaRequest request = VentaRequest.builder()
                .items(List.of(
                        new ItemVentaRequest(10L, 2),  // 2 x 2.50 = 5.00
                        new ItemVentaRequest(20L, 3)   // 3 x 1.80 = 5.40 -> Total = 10.40
                ))
                .build();

        when(usuarioRepository.findByEmail("carlos@minimarket.com")).thenReturn(Optional.of(vendedorPrueba));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto1));
        when(productoRepository.findById(20L)).thenReturn(Optional.of(producto2));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta v = invocation.getArgument(0);
            v.setId(500L);
            return v;
        });

        VentaResponse response = ventaService.registrarVenta(request, "carlos@minimarket.com");

        assertEquals(500L, response.getId());
        assertEquals("Carlos Vendedor", response.getVendedor());
        assertEquals(new BigDecimal("10.40"), response.getTotal());
        assertEquals(2, response.getDetalles().size());

        // Verificar que se invocó la deducción por orden de vencimiento para cada producto
        verify(inventarioService).descontarStockPorVencimiento(10L, 2);
        verify(inventarioService).descontarStockPorVencimiento(20L, 3);
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    void testRegistrarCompraClienteExitoso() {
        Usuario clienteUsuario = Usuario.builder()
                .id(99L)
                .nombre("Maria Cliente")
                .email("maria@gmail.com")
                .rol(Rol.CLIENTE)
                .build();

        Cliente clienteFicha = Cliente.builder()
                .id(88L)
                .nombres("Maria")
                .apellidos("Lopez")
                .numeroDocumento("44556677")
                .email("maria@gmail.com")
                .build();

        VentaRequest request = VentaRequest.builder()
                .items(List.of(new ItemVentaRequest(10L, 2))) // 2 x 2.50 = 5.00
                .build();

        when(usuarioRepository.findByEmail("maria@gmail.com")).thenReturn(Optional.of(clienteUsuario));
        when(clienteRepository.findByUsuario_Email("maria@gmail.com")).thenReturn(Optional.of(clienteFicha));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto1));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setId(900L);
            return v;
        });

        VentaResponse response = ventaService.registrarCompraCliente(request, "maria@gmail.com");

        assertNotNull(response);
        assertEquals(900L, response.getId());
        assertEquals("Maria Cliente", response.getVendedor());
        assertEquals(88L, response.getClienteId());
        assertEquals("Maria Lopez", response.getClienteNombre());
        assertEquals(new BigDecimal("5.00"), response.getTotal());
        verify(inventarioService).descontarStockPorVencimiento(10L, 2);
    }

    @Test
    void testListarMisComprasExitoso() {
        Venta v = Venta.builder()
                .id(101L)
                .usuario(vendedorPrueba)
                .fecha(java.time.LocalDateTime.now())
                .total(new BigDecimal("15.00"))
                .detalles(List.of())
                .build();

        when(ventaRepository.buscarHistorialCliente("maria@gmail.com")).thenReturn(List.of(v));

        List<VentaResponse> compras = ventaService.listarMisCompras("maria@gmail.com");

        assertNotNull(compras);
        assertEquals(1, compras.size());
        assertEquals(101L, compras.get(0).getId());
    }
}
