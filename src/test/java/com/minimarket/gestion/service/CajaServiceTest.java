package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.AperturaCajaRequest;
import com.minimarket.gestion.dto.CierreCajaRequest;
import com.minimarket.gestion.dto.ResumenCajaResponse;
import com.minimarket.gestion.dto.TurnoCajaResponse;
import com.minimarket.gestion.entity.*;
import com.minimarket.gestion.exception.BusinessException;
import com.minimarket.gestion.repository.TurnoCajaRepository;
import com.minimarket.gestion.repository.UsuarioRepository;
import com.minimarket.gestion.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CajaServiceTest {

    @Mock
    private TurnoCajaRepository turnoCajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private CajaService cajaService;

    private Usuario cajero;
    private TurnoCaja turnoAbierto;

    @BeforeEach
    void setUp() {
        cajero = Usuario.builder()
                .id(2L)
                .nombre("Juan Cajero")
                .email("juan@minimarket.com")
                .rol(Rol.VENDEDOR)
                .build();

        turnoAbierto = TurnoCaja.builder()
                .id(10L)
                .usuario(cajero)
                .fechaApertura(LocalDateTime.now())
                .montoApertura(new BigDecimal("100.00"))
                .totalVentasEfectivo(BigDecimal.ZERO)
                .totalVentasDigital(BigDecimal.ZERO)
                .totalVentasTarjeta(BigDecimal.ZERO)
                .totalVendido(BigDecimal.ZERO)
                .estado(EstadoCaja.ABIERTA)
                .observaciones("Apertura turno mañana")
                .build();
    }

    @Test
    void testAbrirTurnoExitoso() {
        AperturaCajaRequest request = AperturaCajaRequest.builder()
                .montoApertura(new BigDecimal("100.00"))
                .observaciones("Apertura turno mañana")
                .build();

        when(usuarioRepository.findByEmail("juan@minimarket.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.existsByUsuarioIdAndEstado(2L, EstadoCaja.ABIERTA)).thenReturn(false);
        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(inv -> {
            TurnoCaja t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TurnoCajaResponse response = cajaService.abrirTurno(request, "juan@minimarket.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(EstadoCaja.ABIERTA, response.getEstado());
        assertEquals(new BigDecimal("100.00"), response.getMontoApertura());
        verify(turnoCajaRepository).save(any(TurnoCaja.class));
    }

    @Test
    void testAbrirTurnoYaAbiertoLanzaError() {
        AperturaCajaRequest request = AperturaCajaRequest.builder()
                .montoApertura(new BigDecimal("100.00"))
                .build();

        when(usuarioRepository.findByEmail("juan@minimarket.com")).thenReturn(Optional.of(cajero));
        when(turnoCajaRepository.existsByUsuarioIdAndEstado(2L, EstadoCaja.ABIERTA)).thenReturn(true);

        assertThrows(BusinessException.class, () -> cajaService.abrirTurno(request, "juan@minimarket.com"));
        verify(turnoCajaRepository, never()).save(any(TurnoCaja.class));
    }

    @Test
    void testObtenerResumenActual() {
        when(turnoCajaRepository.findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc("juan@minimarket.com", EstadoCaja.ABIERTA))
                .thenReturn(Optional.of(turnoAbierto));

        Venta venta1 = Venta.builder().id(1L).total(new BigDecimal("50.00")).metodoPago(MetodoPago.EFECTIVO).build();
        Venta venta2 = Venta.builder().id(2L).total(new BigDecimal("30.00")).metodoPago(MetodoPago.YAPE).build();
        Venta venta3 = Venta.builder().id(3L).total(new BigDecimal("20.00")).metodoPago(MetodoPago.TARJETA_DEBITO).build();

        when(ventaRepository.findByTurnoCajaId(10L)).thenReturn(List.of(venta1, venta2, venta3));

        ResumenCajaResponse resumen = cajaService.obtenerResumenActual("juan@minimarket.com");

        assertNotNull(resumen);
        assertEquals(new BigDecimal("100.00"), resumen.getMontoApertura());
        assertEquals(new BigDecimal("50.00"), resumen.getVentasEfectivo());
        assertEquals(new BigDecimal("30.00"), resumen.getVentasYape());
        assertEquals(new BigDecimal("20.00"), resumen.getVentasTarjetaDebito());
        assertEquals(new BigDecimal("100.00"), resumen.getTotalVendido()); // 50 + 30 + 20
        assertEquals(new BigDecimal("150.00"), resumen.getEfectivoEsperadoEnCaja()); // 100 apertura + 50 efectivo
        assertEquals(3L, resumen.getCantidadVentas());
    }

    @Test
    void testCerrarTurnoExitoso() {
        CierreCajaRequest request = CierreCajaRequest.builder()
                .montoCierreReal(new BigDecimal("150.00")) // Coincide exacto con 100 apertura + 50 efectivo
                .observaciones("Cuadre perfecto")
                .build();

        when(turnoCajaRepository.findFirstByUsuarioEmailAndEstadoOrderByFechaAperturaDesc("juan@minimarket.com", EstadoCaja.ABIERTA))
                .thenReturn(Optional.of(turnoAbierto));

        Venta venta1 = Venta.builder().id(1L).total(new BigDecimal("50.00")).metodoPago(MetodoPago.EFECTIVO).build();
        when(ventaRepository.findByTurnoCajaId(10L)).thenReturn(List.of(venta1));

        when(turnoCajaRepository.save(any(TurnoCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        TurnoCajaResponse response = cajaService.cerrarTurno(request, "juan@minimarket.com");

        assertNotNull(response);
        assertEquals(EstadoCaja.CERRADA, response.getEstado());
        assertEquals(new BigDecimal("150.00"), response.getMontoCierreEsperado());
        assertEquals(new BigDecimal("150.00"), response.getMontoCierreReal());
        assertEquals(new BigDecimal("0.00"), response.getDiferencia());
        assertNotNull(response.getFechaCierre());
        verify(turnoCajaRepository).save(any(TurnoCaja.class));
    }
}
