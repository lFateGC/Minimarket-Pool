package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaDesc();

    Optional<Compra> findByNumeroComprobante(String numeroComprobante);

    boolean existsByNumeroComprobante(String numeroComprobante);

    List<Compra> findByProveedorIdOrderByFechaDesc(Long proveedorId);
}
