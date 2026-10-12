package com.minimarket.gestion.controller;

import com.minimarket.gestion.dto.*;
import com.minimarket.gestion.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/stock-bajo")
    public List<ReporteStockBajoResponse> reporteStockBajo() {
        return reporteService.reporteStockBajo();
    }

    @GetMapping("/por-vencer")
    public List<LotePorVencerResponse> reportesPorVencer(@RequestParam(defaultValue = "7") int dias) {
        return reporteService.reportesPorVencer(dias);
    }

    @GetMapping("/top-productos")
    public List<TopProductoVendidoResponse> topProductosVendidos() {
        return reporteService.topProductosVendidos();
    }

    @GetMapping("/ventas-resumen")
    public ResumenVentasResponse resumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        return reporteService.resumenVentas(fechaInicio, fechaFin);
    }

    @GetMapping("/dashboard")
    public DashboardKpiResponse dashboardKpis() {
        return reporteService.dashboardKpis();
    }
}
