package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ProductoRequest;
import com.minimarket.gestion.dto.ProductoResponse;
import com.minimarket.gestion.entity.CategoriaProducto;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.Proveedor;
import com.minimarket.gestion.repository.CategoriaProductoRepository;
import com.minimarket.gestion.repository.LoteInventarioRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import com.minimarket.gestion.repository.ProveedorRepository;
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
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaProductoRepository categoriaRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private LoteInventarioRepository loteRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private ProductoService productoService;

    private CategoriaProducto categoria;
    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        categoria = CategoriaProducto.builder().id(1L).nombre("Snacks").build();
        proveedor = Proveedor.builder().id(1L).nombre("Alicorp").ruc("20100000001").build();
    }

    @Test
    void testCrearProductoConSupabaseLink() {
        ProductoRequest req = new ProductoRequest();
        req.setNombre("Galleta Casino");
        req.setSku("775000001");
        req.setPrecioUnitario(new BigDecimal("1.50"));
        req.setStockMinimo(10);
        req.setCategoriaId(1L);
        req.setProveedorId(1L);
        req.setImagenUrl("casino.jpg");

        when(productoRepository.existsBySku("775000001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(supabaseStorageService.normalizarUrlImagen("casino.jpg"))
                .thenReturn("https://xyz.supabase.co/storage/v1/object/public/productos-minimarket/casino.jpg");
        when(loteRepository.sumarStockPorProducto(any())).thenReturn(0);

        Producto productoGuardado = Producto.builder()
            .id(99L)
            .nombre("Galleta Casino")
            .sku("775000001")
            .imagenUrl("https://xyz.supabase.co/storage/v1/object/public/productos-minimarket/casino.jpg")
            .precioUnitario(new BigDecimal("1.50"))
            .stockMinimo(10)
            .categoria(categoria)
            .proveedor(proveedor)
            .build();
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);

        ProductoResponse resp = productoService.crear(req);

        assertEquals(99L, resp.getId());
        assertEquals("Galleta Casino", resp.getNombre());
        assertEquals("https://xyz.supabase.co/storage/v1/object/public/productos-minimarket/casino.jpg", resp.getImagenUrl());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void testBuscarProductosPorTermino() {
        Producto p = Producto.builder()
                .id(1L)
                .nombre("Inka Kola 500ml")
                .sku("775111111")
                .precioUnitario(new BigDecimal("3.00"))
                .stockMinimo(5)
                .categoria(categoria)
                .proveedor(proveedor)
                .build();

        when(productoRepository.buscarPorTermino("Inka")).thenReturn(List.of(p));
        when(loteRepository.sumarStockPorProducto(1L)).thenReturn(15);

        List<ProductoResponse> resultado = productoService.buscar("Inka");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Inka Kola 500ml", resultado.get(0).getNombre());
        assertEquals(15, resultado.get(0).getStockActual());
    }
}
