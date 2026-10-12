package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.ProductoRequest;
import com.minimarket.gestion.dto.ProductoResponse;
import com.minimarket.gestion.entity.CategoriaProducto;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.Proveedor;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.exception.ResourceNotFoundException;
import com.minimarket.gestion.repository.CategoriaProductoRepository;
import com.minimarket.gestion.repository.LoteInventarioRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import com.minimarket.gestion.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final LoteInventarioRepository loteRepository;
    private final SupabaseStorageService supabaseStorageService;

    public List<ProductoResponse> listar() {
        return productoRepository.findAllWithRelations().stream()
                .map(this::aResponse)
                .toList();
    }

    public ProductoResponse obtenerPorId(Long id) {
        return aResponse(buscarEntidad(id));
    }

    public List<ProductoResponse> listarPorCategoria(Long categoriaId) {
        return productoRepository.findByCategoria_Id(categoriaId).stream()
                .map(this::aResponse)
                .toList();
    }

    public List<ProductoResponse> buscar(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return listar();
        }
        return productoRepository.buscarPorTermino(termino.trim()).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String sku = request.getSku().trim();
        if (productoRepository.existsBySku(sku)) {
            throw new BusinessException("Ya existe un producto con el SKU: " + sku);
        }

        if (request.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El precio unitario debe ser mayor a 0");
        }

        if (request.getStockMinimo() < 0) {
            throw new BusinessException("El stock mínimo no puede ser negativo");
        }

        validarPrecioCaja(request);

        CategoriaProducto categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + request.getProveedorId()));

        String urlNormalizada = supabaseStorageService.normalizarUrlImagen(request.getImagenUrl());

        Producto producto = Producto.builder()
                .nombre(request.getNombre().trim())
                .sku(sku)
                .imagenUrl(urlNormalizada)
                .precioUnitario(request.getPrecioUnitario())
                .precioCaja(request.getPrecioCaja())
                .unidadesPorCaja(request.getUnidadesPorCaja())
                .stockMinimo(request.getStockMinimo())
                .categoria(categoria)
                .proveedor(proveedor)
                .build();

        return aResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidad(id);
        String sku = request.getSku().trim();

        if (productoRepository.existsBySkuAndIdNot(sku, id)) {
            throw new BusinessException("Ya existe otro producto con el SKU: " + sku);
        }

        if (request.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El precio unitario debe ser mayor a 0");
        }

        if (request.getStockMinimo() < 0) {
            throw new BusinessException("El stock mínimo no puede ser negativo");
        }

        validarPrecioCaja(request);

        CategoriaProducto categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + request.getProveedorId()));

        String urlNormalizada = supabaseStorageService.normalizarUrlImagen(request.getImagenUrl());

        producto.setNombre(request.getNombre().trim());
        producto.setSku(sku);
        producto.setImagenUrl(urlNormalizada);
        producto.setPrecioUnitario(request.getPrecioUnitario());
        producto.setPrecioCaja(request.getPrecioCaja());
        producto.setUnidadesPorCaja(request.getUnidadesPorCaja());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setCategoria(categoria);
        producto.setProveedor(proveedor);

        return aResponse(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }

        if (loteRepository.existsByProducto_Id(id)) {
            throw new BusinessException("No se puede eliminar el producto porque tiene lotes de inventario asociados. Se debe mantener para auditoría de compras/ventas.");
        }

        productoRepository.deleteById(id);
    }

    private Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    private void validarPrecioCaja(ProductoRequest request) {
        if (request.getPrecioCaja() != null && request.getUnidadesPorCaja() == null) {
            throw new BusinessException("Las unidades por caja son obligatorias cuando se indica un precio de caja");
        }
    }

    private ProductoResponse aResponse(Producto p) {
        Integer stockActual = loteRepository.sumarStockPorProducto(p.getId());

        return ProductoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .sku(p.getSku())
                .imagenUrl(p.getImagenUrl())
                .precioUnitario(p.getPrecioUnitario())
                .precioCaja(p.getPrecioCaja())
                .unidadesPorCaja(p.getUnidadesPorCaja())
                .stockMinimo(p.getStockMinimo())
                .stockActual(stockActual)
                .categoriaId(p.getCategoria() != null ? p.getCategoria().getId() : null)
                .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .proveedorId(p.getProveedor() != null ? p.getProveedor().getId() : null)
                .proveedorNombre(p.getProveedor() != null ? p.getProveedor().getNombre() : null)
                .build();
    }
}
