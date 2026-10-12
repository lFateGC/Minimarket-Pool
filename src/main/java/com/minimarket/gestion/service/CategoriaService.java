package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.CategoriaRequest;
import com.minimarket.gestion.dto.CategoriaResponse;
import com.minimarket.gestion.entity.CategoriaProducto;
import com.minimarket.gestion.repository.CategoriaProductoRepository;
import com.minimarket.gestion.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CategoriaService {

    private final CategoriaProductoRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre()))
                .toList();
    }

    public CategoriaResponse obtenerPorId(Long id) {
        CategoriaProducto categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + request.getNombre().trim());
        }

        CategoriaProducto categoria = CategoriaProducto.builder()
                .nombre(request.getNombre().trim())
                .build();

        CategoriaProducto guardada = categoriaRepository.save(categoria);
        return new CategoriaResponse(guardada.getId(), guardada.getNombre());
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        CategoriaProducto categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new IllegalArgumentException("Ya existe otra categoría con el nombre: " + request.getNombre().trim());
        }

        categoria.setNombre(request.getNombre().trim());
        CategoriaProducto actualizada = categoriaRepository.save(categoria);
        return new CategoriaResponse(actualizada.getId(), actualizada.getNombre());
    }

    @Transactional
    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new EntityNotFoundException("Categoría no encontrada con ID: " + id);
        }

        if (productoRepository.existsByCategoria_Id(id)) {
            throw new IllegalStateException("No se puede eliminar la categoría porque existen productos asociados a ella.");
        }

        categoriaRepository.deleteById(id);
    }
}
