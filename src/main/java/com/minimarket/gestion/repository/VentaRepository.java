package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByUsuario_EmailOrderByFechaDesc(String email);

    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Venta> findByTurnoCajaId(Long turnoCajaId);

    @Query("SELECT DISTINCT v FROM Venta v " +
           "LEFT JOIN FETCH v.detalles d " +
           "LEFT JOIN FETCH d.producto p " +
           "WHERE v.usuario.email = :email " +
           "OR (v.cliente IS NOT NULL AND (v.cliente.usuario.email = :email OR v.cliente.email = :email)) " +
           "ORDER BY v.fecha DESC")
    List<Venta> buscarHistorialCliente(@Param("email") String email);
}
