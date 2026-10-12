package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.LoteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoteInventarioRepository extends JpaRepository<LoteInventario, Long> {

    // Lotes con stock disponible de un producto, ordenados por fecha de vencimiento ascendente (FEFO)
    List<LoteInventario> findByProducto_IdAndCantidadGreaterThanOrderByFechaVencimientoAsc(
            Long productoId, Integer cantidadMinima);

    // Lotes con stock disponible no vencidos para venta comercial
    List<LoteInventario> findByProducto_IdAndCantidadGreaterThanAndFechaVencimientoGreaterThanEqualOrderByFechaVencimientoAsc(
            Long productoId, Integer cantidadMinima, LocalDate fechaActual);

    // Para reporte de lotes que vencen antes de una fecha límite
    @Query("SELECT l FROM LoteInventario l WHERE l.cantidad > 0 AND l.fechaVencimiento <= :fechaLimite ORDER BY l.fechaVencimiento ASC")
    List<LoteInventario> findLotesProximosAVencer(@Param("fechaLimite") LocalDate fechaLimite);

    // Suma el stock total disponible de un producto específico
    @Query("SELECT COALESCE(SUM(l.cantidad), 0) FROM LoteInventario l WHERE l.producto.id = :productoId AND l.cantidad > 0")
    Integer sumarStockPorProducto(@Param("productoId") Long productoId);

    // Verifica si un producto tiene lotes registrados
    boolean existsByProducto_Id(Long productoId);
}
