package com.minimarket.gestion.service;

import com.minimarket.gestion.entity.Proveedor;
import com.minimarket.gestion.repository.ProductoRepository;
import com.minimarket.gestion.repository.ProveedorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    public Proveedor obtenerPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    @Transactional
    public Proveedor crear(Proveedor proveedor) {
        if (proveedorRepository.existsByRuc(proveedor.getRuc().trim())) {
            throw new IllegalArgumentException("Ya existe un proveedor registrado con el RUC: " + proveedor.getRuc().trim());
        }
        proveedor.setNombre(proveedor.getNombre().trim());
        proveedor.setRuc(proveedor.getRuc().trim());
        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public Proveedor actualizar(Long id, Proveedor datos) {
        Proveedor proveedor = obtenerPorId(id);

        if (proveedorRepository.existsByRucAndIdNot(datos.getRuc().trim(), id)) {
            throw new IllegalArgumentException("Ya existe otro proveedor con el RUC: " + datos.getRuc().trim());
        }

        proveedor.setNombre(datos.getNombre().trim());
        proveedor.setRuc(datos.getRuc().trim());
        proveedor.setContacto(datos.getContacto() != null ? datos.getContacto().trim() : null);

        return proveedorRepository.save(proveedor);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!proveedorRepository.existsById(id)) {
            throw new EntityNotFoundException("Proveedor no encontrado con ID: " + id);
        }

        if (productoRepository.existsByProveedor_Id(id)) {
            throw new IllegalStateException("No se puede eliminar el proveedor porque tiene productos asociados en el catálogo.");
        }

        proveedorRepository.deleteById(id);
    }
}
