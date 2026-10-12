package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.CategoriaRequest;
import com.minimarket.gestion.dto.CategoriaResponse;
import com.minimarket.gestion.entity.CategoriaProducto;
import com.minimarket.gestion.repository.CategoriaProductoRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CategoriaServiceTest {

    @Mock
    private CategoriaProductoRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void testCrearCategoriaExitoso() {
        CategoriaRequest request = new CategoriaRequest("Snacks");

        when(categoriaRepository.existsByNombreIgnoreCase("Snacks")).thenReturn(false);
        CategoriaProducto categoriaGuardada = CategoriaProducto.builder()
            .id(1L)
            .nombre("Snacks")
            .build();
        when(categoriaRepository.save(any(CategoriaProducto.class))).thenReturn(categoriaGuardada);

        CategoriaResponse response = categoriaService.crear(request);

        assertEquals(1L, response.getId());
        assertEquals("Snacks", response.getNombre());
    }

    @Test
    void testCrearCategoriaDuplicadaLanzaExcepcion() {
        CategoriaRequest request = new CategoriaRequest("Bebidas");
        when(categoriaRepository.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoriaService.crear(request));
    }

    @Test
    void testEliminarCategoriaConProductosLanzaExcepcion() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);
        when(productoRepository.existsByCategoria_Id(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> categoriaService.eliminar(1L));
    }
}
