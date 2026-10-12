package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByUsuario_Email(String email);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

    boolean existsByEmail(String email);

    List<Cliente> findByActivoTrueOrderByNombresAsc();

    @Query("SELECT c FROM Cliente c WHERE c.activo = true AND (" +
           "LOWER(c.nombres) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "(c.apellidos IS NOT NULL AND LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%'))) OR " +
           "c.numeroDocumento LIKE CONCAT('%', :filtro, '%')) " +
           "ORDER BY c.nombres ASC")
    List<Cliente> buscarPorFiltro(@Param("filtro") String filtro);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.cliente.id = :clienteId")
    long contarComprasPorClienteId(@Param("clienteId") Long clienteId);
}
