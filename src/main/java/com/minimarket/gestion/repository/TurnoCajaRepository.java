package com.minimarket.gestion.repository;

import com.minimarket.gestion.entity.EstadoCaja;
import com.minimarket.gestion.entity.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    Optional<TurnoCaja> findByUsuarioIdAndEstado(Long usuarioId, EstadoCaja estado);

    Optional<TurnoCaja> findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc(String email, EstadoCaja estado);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoCaja estado);

    List<TurnoCaja> findAllByOrderByFechaAperturaDesc();

    List<TurnoCaja> findByUsuarioIdOrderByFechaAperturaDesc(Long usuarioId);
}
