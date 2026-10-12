package com.minimarket.gestion.service;

import com.minimarket.gestion.dto.*;
import com.minimarket.gestion.entity.DetalleVenta;
import com.minimarket.gestion.entity.LoteInventario;
import com.minimarket.gestion.entity.Producto;
import com.minimarket.gestion.entity.Venta;
import com.minimarket.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class ReporteService {

    private final ProductoRepository productoRepository;
    private final CategoriaProductoRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final LoteInventarioRepository loteRepository;
    private final VentaRepository ventaRepository;

    // Reporte de productos con stock actual menor al stock mínimo configurado
    public List<ReporteStockBajoResponse> reporteStockBajo() {
        List<Producto> productos = productoRepository.findAll();
        List<ReporteStockBajoResponse> alertas = new ArrayList<>();

        for (Producto p : productos) {
            Integer stockActual = loteRepository.sumarStockPorProducto(p.getId());
            if (stockActual < p.getStockMinimo()) {
                alertas.add(ReporteStockBajoResponse.builder()
                        .productoId(p.getId())
                        .nombre(p.getNombre())
                        .sku(p.getSku())
                        .stockActual(stockActual)
                        .stockMinimo(p.getStockMinimo())
                        .deficit(p.getStockMinimo() - stockActual)
                        .categoriaNombre(p.getCategoria().getNombre())
                        .proveedorNombre(p.getProveedor().getNombre())
                        .build());
            }
        }

        return alertas;
    }

    // Reporte de lotes con stock disponibles próximos a vencer en los siguientes N días
    public List<LotePorVencerResponse> reportesPorVencer(int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("La cantidad de días no puede ser negativa");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(dias);
        List<LoteInventario> lotes = loteRepository.findLotesProximosAVencer(fechaLimite);

        return lotes.stream().map(lote -> {
            long diasRestantes = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());
            String estado;
            if (diasRestantes < 0) {
                estado = "VENCIDO (" + Math.abs(diasRestantes) + " días atrás)";
            } else if (diasRestantes <= 3) {
                estado = "ALERTA CRÍTICA (" + diasRestantes + " días restantes)";
            } else {
                estado = "PRÓXIMO A VENCER (" + diasRestantes + " días restantes)";
            }

            return LotePorVencerResponse.builder()
                    .loteId(lote.getId())
                    .productoId(lote.getProducto().getId())
                    .productoNombre(lote.getProducto().getNombre())
                    .sku(lote.getProducto().getSku())
                    .cantidad(lote.getCantidad())
                    .fechaIngreso(lote.getFechaIngreso())
                    .fechaVencimiento(lote.getFechaVencimiento())
                    .diasParaVencer(diasRestantes)
                    .estadoVencimiento(estado)
                    .build();
        }).toList();
    }

    // Ranking de productos más vendidos y análisis para toma de decisiones
    public List<TopProductoVendidoResponse> topProductosVendidos() {
        List<Venta> todasLasVentas = ventaRepository.findAll();
        Map<Long, Integer> cantidadesVendidas = new HashMap<>();
        Map<Long, BigDecimal> totalesRecaudados = new HashMap<>();
        Map<Long, Producto> productosMap = new HashMap<>();

        BigDecimal totalIngresosGlobal = BigDecimal.ZERO;

        for (Venta v : todasLasVentas) {
            for (DetalleVenta d : v.getDetalles()) {
                Producto prod = d.getProducto();
                Long prodId = prod.getId();
                productosMap.put(prodId, prod);

                cantidadesVendidas.merge(prodId, d.getCantidad(), Integer::sum);

                BigDecimal subtotal = d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()));
                totalesRecaudados.merge(prodId, subtotal, BigDecimal::add);
                totalIngresosGlobal = totalIngresosGlobal.add(subtotal);
            }
        }

        final BigDecimal granTotal = totalIngresosGlobal;

        return cantidadesVendidas.entrySet().stream()
                .map(entry -> {
                    Long prodId = entry.getKey();
                    Producto prod = productosMap.get(prodId);
                    Integer unidades = entry.getValue();
                    BigDecimal recaudado = totalesRecaudados.getOrDefault(prodId, BigDecimal.ZERO);

                    double porcentaje = (granTotal.compareTo(BigDecimal.ZERO) > 0)
                            ? recaudado.divide(granTotal, 4, RoundingMode.HALF_UP).doubleValue() * 100
                            : 0.0;

                    String sugerencia;
                    if (unidades >= 10 || porcentaje >= 25.0) {
                        sugerencia = "🔥 Producto Estrella: Negociar mejores precios por volumen con proveedor y aumentar stock de seguridad.";
                    } else if (unidades >= 5) {
                        sugerencia = "⭐ Demanda Moderada: Mantener reposición periódica constante según flujo de venta.";
                    } else {
                        sugerencia = "💡 Baja Rotación: Evaluar promociones cruzadas o combos para acelerar rotación.";
                    }

                    return TopProductoVendidoResponse.builder()
                            .productoId(prodId)
                            .nombre(prod.getNombre())
                            .sku(prod.getSku())
                            .categoriaNombre(prod.getCategoria().getNombre())
                            .unidadesVendidas(unidades)
                            .totalRecaudado(recaudado.setScale(2, RoundingMode.HALF_UP))
                            .porcentajeVentas(Math.round(porcentaje * 100.0) / 100.0)
                            .sugerenciaEstrategica(sugerencia)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getUnidadesVendidas(), a.getUnidadesVendidas()))
                .toList();
    }

    // Resumen consolidado de ventas en un rango de fechas
    public ResumenVentasResponse resumenVentas(LocalDateTime inicio, LocalDateTime fin) {
        if ((inicio == null) != (fin == null)) {
            throw new IllegalArgumentException("Debe indicar fecha de inicio y fecha de fin juntas");
        }
        if (inicio != null && fin != null && fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        List<Venta> ventas = (inicio != null && fin != null)
                ? ventaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin)
                : ventaRepository.findAll();

        long totalVentas = ventas.size();
        BigDecimal totalRecaudado = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketPromedio = totalVentas > 0
                ? totalRecaudado.divide(BigDecimal.valueOf(totalVentas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResumenVentasResponse.builder()
                .fechaInicio(inicio)
                .fechaFin(fin)
                .totalVentas(totalVentas)
                .totalRecaudado(totalRecaudado)
                .ticketPromedio(ticketPromedio)
                .build();
    }

    // Dashboard con KPIs principales para toma de decisiones
    public DashboardKpiResponse dashboardKpis() {
        long totalProductos = productoRepository.count();
        long totalCategorias = categoriaRepository.count();
        long totalProveedores = proveedorRepository.count();

        long productosStockBajo = reporteStockBajo().size();
        long lotesPorVencer = reportesPorVencer(7).size();

        List<Venta> todasLasVentas = ventaRepository.findAll();
        long totalVentas = todasLasVentas.size();
        BigDecimal ingresosTotales = todasLasVentas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardKpiResponse.builder()
                .totalProductosRegistrados(totalProductos)
                .totalCategorias(totalCategorias)
                .totalProveedores(totalProveedores)
                .totalProductosConStockBajo(productosStockBajo)
                .totalLotesPorVencer(lotesPorVencer)
                .totalVentasRegistradas(totalVentas)
                .ingresosTotales(ingresosTotales)
                .build();
    }
}
