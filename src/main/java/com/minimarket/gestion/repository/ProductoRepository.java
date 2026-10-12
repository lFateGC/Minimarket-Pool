package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByCategoria_Id(Long categoriaId);

    boolean existsByProveedor_Id(Long proveedorId);

    List<Producto> findByCategoria_Id(Long categoriaId);

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria JOIN FETCH p.proveedor")
    List<Producto> findAllWithRelations();

    @Query("SELECT p FROM Producto p JOIN FETCH p.categoria JOIN FETCH p.proveedor WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.categoria.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Producto> buscarPorTermino(@org.springframework.data.repository.query.Param("termino") String termino);
}
