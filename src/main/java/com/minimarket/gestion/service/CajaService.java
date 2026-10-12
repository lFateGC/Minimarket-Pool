package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.AperturaCajaRequest;
import com.minimarket.gestion.dto.CierreCajaRequest;
import com.minimarket.gestion.dto.ResumenCajaResponse;
import com.minimarket.gestion.dto.TurnoCajaResponse;
import com.minimarket.gestion.entity.EstadoCaja;
import com.minimarket.gestion.entity.MetodoPago;
import com.minimarket.gestion.entity.TurnoCaja;
import com.minimarket.gestion.entity.Usuario;
import com.minimarket.gestion.entity.Venta;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.exception.ResourceNotFoundException;
import com.minimarket.gestion.repository.TurnoCajaRepository;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;

    @Transactional
    public TurnoCajaResponse abrirTurno(AperturaCajaRequest request, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + emailUsuario));

        if (turnoCajaRepository.existsByUsuarioIdAndEstado(usuario.getId(), EstadoCaja.ABIERTA)) {
            throw new BusinessException("Ya cuenta con un turno de caja abierto actualmente.");
        }

        BigDecimal montoApertura = request.getMontoApertura() != null
                ? request.getMontoApertura().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        TurnoCaja turno = TurnoCaja.builder()
                .usuario(usuario)
                .fechaApertura(LocalDateTime.now())
                .montoApertura(montoApertura)
                .totalVentasEfectivo(BigDecimal.ZERO)
                .totalVentasDigital(BigDecimal.ZERO)
                .totalVentasTarjeta(BigDecimal.ZERO)
                .totalVendido(BigDecimal.ZERO)
                .estado(EstadoCaja.ABIERTA)
                .observaciones(request.getObservaciones())
                .build();

        turno = turnoCajaRepository.save(turno);
        return aResponse(turno, 0L);
    }

    @Transactional(readOnly = true)
    public TurnoCajaResponse obtenerTurnoActivo(String emailUsuario) {
        TurnoCaja turno = turnoCajaRepository.findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc(emailUsuario, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No tiene ningún turno de caja abierto actualmente."));

        List<Venta> ventas = ventaRepository.findByTurnoCajaId(turno.getId());
        return aResponse(turno, (long) ventas.size());
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumenActual(String emailUsuario) {
        TurnoCaja turno = turnoCajaRepository.findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc(emailUsuario, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No tiene ningún turno de caja abierto actualmente."));

        List<Venta> ventas = ventaRepository.findByTurnoCajaId(turno.getId());

        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal yape = BigDecimal.ZERO;
        BigDecimal plin = BigDecimal.ZERO;
        BigDecimal debito = BigDecimal.ZERO;
        BigDecimal credito = BigDecimal.ZERO;
        BigDecimal transferencia = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Venta v : ventas) {
            BigDecimal monto = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            total = total.add(monto);

            MetodoPago metodo = v.getMetodoPago() != null ? v.getMetodoPago() : MetodoPago.EFECTIVO;
            switch (metodo) {
                case EFECTIVO -> efectivo = efectivo.add(monto);
                case YAPE -> yape = yape.add(monto);
                case PLIN -> plin = plin.add(monto);
                case TARJETA_DEBITO -> debito = debito.add(monto);
                case TARJETA_CREDITO -> credito = credito.add(monto);
                case TRANSFERENCIA -> transferencia = transferencia.add(monto);
            }
        }

        BigDecimal efectivoEsperado = turno.getMontoApertura().add(efectivo).setScale(2, RoundingMode.HALF_UP);

        return ResumenCajaResponse.builder()
                .turnoId(turno.getId())
                .estado(turno.getEstado())
                .usuarioId(turno.getUsuario().getId())
                .usuarioNombre(turno.getUsuario().getNombre())
                .fechaApertura(turno.getFechaApertura())
                .montoApertura(turno.getMontoApertura())
                .ventasEfectivo(efectivo.setScale(2, RoundingMode.HALF_UP))
                .ventasYape(yape.setScale(2, RoundingMode.HALF_UP))
                .ventasPlin(plin.setScale(2, RoundingMode.HALF_UP))
                .ventasTarjetaDebito(debito.setScale(2, RoundingMode.HALF_UP))
                .ventasTarjetaCredito(credito.setScale(2, RoundingMode.HALF_UP))
                .ventasTransferencia(transferencia.setScale(2, RoundingMode.HALF_UP))
                .totalVendido(total.setScale(2, RoundingMode.HALF_UP))
                .efectivoEsperadoEnCaja(efectivoEsperado)
                .cantidadVentas((long) ventas.size())
                .build();
    }

    @Transactional
    public TurnoCajaResponse cerrarTurno(CierreCajaRequest request, String emailUsuario) {
        TurnoCaja turno = turnoCajaRepository.findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc(emailUsuario, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException("No tiene ningún turno de caja abierto para cerrar."));

        List<Venta> ventas = ventaRepository.findByTurnoCajaId(turno.getId());

        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal digital = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (Venta v : ventas) {
            BigDecimal monto = v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO;
            total = total.add(monto);

            MetodoPago metodo = v.getMetodoPago() != null ? v.getMetodoPago() : MetodoPago.EFECTIVO;
            switch (metodo) {
                case EFECTIVO -> efectivo = efectivo.add(monto);
                case YAPE, PLIN, TRANSFERENCIA -> digital = digital.add(monto);
                case TARJETA_DEBITO, TARJETA_CREDITO -> tarjeta = tarjeta.add(monto);
            }
        }

        BigDecimal montoCierreEsperado = turno.getMontoApertura().add(efectivo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoCierreReal = request.getMontoCierreReal() != null
                ? request.getMontoCierreReal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal diferencia = montoCierreReal.subtract(montoCierreEsperado).setScale(2, RoundingMode.HALF_UP);

        turno.setFechaCierre(LocalDateTime.now());
        turno.setTotalVentasEfectivo(efectivo.setScale(2, RoundingMode.HALF_UP));
        turno.setTotalVentasDigital(digital.setScale(2, RoundingMode.HALF_UP));
        turno.setTotalVentasTarjeta(tarjeta.setScale(2, RoundingMode.HALF_UP));
        turno.setTotalVendido(total.setScale(2, RoundingMode.HALF_UP));
        turno.setMontoCierreEsperado(montoCierreEsperado);
        turno.setMontoCierreReal(montoCierreReal);
        turno.setDiferencia(diferencia);
        turno.setEstado(EstadoCaja.CERRADA);

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            turno.setObservaciones(request.getObservaciones().trim());
        }

        turno = turnoCajaRepository.save(turno);
        return aResponse(turno, (long) ventas.size());
    }

    @Transactional(readOnly = true)
    public List<TurnoCajaResponse> listarHistorial(String emailUsuario, boolean esAdmin) {
        List<TurnoCaja> lista;
        if (esAdmin) {
            lista = turnoCajaRepository.findAllByOrderByFechaAperturaDesc();
        } else {
            Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + emailUsuario));
            lista = turnoCajaRepository.findByUsuarioIdOrderByFechaAperturaDesc(usuario.getId());
        }

        return lista.stream()
                .map(t -> aResponse(t, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TurnoCaja> buscarTurnoActivoPorUsuario(Long usuarioId) {
        return turnoCajaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoCaja.ABIERTA);
    }

    private TurnoCajaResponse aResponse(TurnoCaja turno, Long cantidadVentas) {
        return TurnoCajaResponse.builder()
                .id(turno.getId())
                .usuarioId(turno.getUsuario().getId())
                .usuarioNombre(turno.getUsuario().getNombre())
                .fechaApertura(turno.getFechaApertura())
                .fechaCierre(turno.getFechaCierre())
                .montoApertura(turno.getMontoApertura())
                .totalVentasEfectivo(turno.getTotalVentasEfectivo())
                .totalVentasDigital(turno.getTotalVentasDigital())
                .totalVentasTarjeta(turno.getTotalVentasTarjeta())
                .totalVendido(turno.getTotalVendido())
                .montoCierreEsperado(turno.getMontoCierreEsperado())
                .montoCierreReal(turno.getMontoCierreReal())
                .diferencia(turno.getDiferencia())
                .estado(turno.getEstado())
                .observaciones(turno.getObservaciones())
                .cantidadVentas(cantidadVentas)
                .build();
    }
}
