package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.EntradaInventarioRequest;
import com.minimarket.gestion.dto.LoteInventarioResponse;
import com.minimarket.gestion.dto.MermaInventarioRequest;
import com.minimarket.gestion.entity.LoteInventario;
import com.minimarket.gestion.entity.MovimientoInventario;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.repository.LoteInventarioRepository;
import com.minimarket.gestion.repository.MovimientoInventarioRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class InventarioServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private LoteInventarioRepository loteRepository;

    @Mock
    private MovimientoInventarioRepository movimientoRepository;

    @InjectMocks
    private InventarioService inventarioService;

    private Producto productoPrueba;

    @BeforeEach
    void setUp() {
        productoPrueba = Producto.builder()
                .id(1L)
                .nombre("Gaseosa Inka Cola 500ml")
                .sku("775012345678")
                .stockMinimo(10)
                .build();
    }

    @Test
    void testRegistrarEntradaExitoso() {
        EntradaInventarioRequest request = EntradaInventarioRequest.builder()
                .productoId(1L)
                .cantidad(50)
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoPrueba));
        LoteInventario loteGuardado = LoteInventario.builder()
                .id(100L)
                .producto(productoPrueba)
                .cantidad(50)
                .fechaIngreso(LocalDate.now())
                .fechaVencimiento(request.getFechaVencimiento())
                .build();
        when(loteRepository.save(any(LoteInventario.class))).thenReturn(loteGuardado);

        LoteInventarioResponse response = inventarioService.registrarEntrada(request);

        assertEquals(50, response.getCantidad());
        assertEquals("Gaseosa Inka Cola 500ml", response.getProductoNombre());
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }

    @Test
    void testDescontarStockPorVencimientoPrioridad() {
        LoteInventario lote1 = LoteInventario.builder()
                .id(10L)
                .producto(productoPrueba)
                .cantidad(10)
                .fechaIngreso(LocalDate.now().minusDays(5))
                .fechaVencimiento(LocalDate.now().plusDays(5)) // Vence primero
                .build();

        LoteInventario lote2 = LoteInventario.builder()
                .id(11L)
                .producto(productoPrueba)
                .cantidad(20)
                .fechaIngreso(LocalDate.now().minusDays(2))
                .fechaVencimiento(LocalDate.now().plusDays(25)) // Vence después
                .build();

        List<LoteInventario> lotes = List.of(lote1, lote2);
        when(loteRepository.findByProducto_IdAndCantidadGreaterThanAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(
                1L, 0, LocalDate.now()))
                .thenReturn(lotes);

        // Descontar 15 unidades: debe vaciar Lote 1 (10 u) y descontar 5 de Lote 2
        inventarioService.descontarStockPorVencimiento(1L, 15);

        assertEquals(0, lote1.getCantidad());
        assertEquals(15, lote2.getCantidad());
        verify(loteRepository, times(2)).save(any(LoteInventario.class));
        verify(movimientoRepository, times(2)).save(any(MovimientoInventario.class));
    }

    @Test
    void testDescontarStockInsuficienteLanzaExcepcion() {
        LoteInventario lote1 = LoteInventario.builder()
                .id(10L)
                .producto(productoPrueba)
                .cantidad(5)
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .build();

        when(loteRepository.findByProducto_IdAndCantidadGreaterThanAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(
                1L, 0, LocalDate.now()))
                .thenReturn(List.of(lote1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            inventarioService.descontarStockPorVencimiento(1L, 20); // Solicita 20 pero solo hay 5
        });

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    void testRegistrarMermaExitoso() {
        LoteInventario lote = LoteInventario.builder()
                .id(20L)
                .producto(productoPrueba)
                .cantidad(15)
                .fechaVencimiento(LocalDate.now().minusDays(1)) // Vencido
                .build();

        when(loteRepository.findById(20L)).thenReturn(Optional.of(lote));

        MermaInventarioRequest request = MermaInventarioRequest.builder()
                .loteId(20L)
                .cantidad(5)
                .motivo("Producto vencido")
                .build();

        inventarioService.registrarMerma(request);

        assertEquals(10, lote.getCantidad());
        verify(loteRepository, times(1)).save(lote);
        verify(movimientoRepository).save(any(MovimientoInventario.class));
    }
}
