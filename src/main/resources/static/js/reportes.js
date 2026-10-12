// ==============================================================================
// MODULO: DASHBOARD, KPIS, REPORTES Y TOMA DE DECISIONES (reportes.js)
// Estructura idéntica a StockMaster
// ==============================================================================

/**
 * Carga los KPIs ejecutivos del Dashboard principal y la tabla de últimas ventas.
 */
async function cargarDashboard() {
    try {
        // 1. Cargar fecha actual formateada en español
        const hoy = new Date();
        const opcionesFecha = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        const fechaTexto = hoy.toLocaleDateString('es-PE', opcionesFecha);
        const elFecha = document.getElementById('dashFechaActual');
        if (elFecha) elFecha.innerText = fechaTexto;

        // 2. Cargar KPIs del backend
        const resKpis = await fetch(`${API_BASE}/reportes/dashboard`, { headers: getAuthHeaders() });
        if (resKpis.ok) {
            const data = await resKpis.json();
            
            // Total productos y alertas
            document.getElementById('kpiTotalProductos').innerText = data.totalProductosRegistrados || 0;
            document.getElementById('kpiAlertasStock').innerText = (data.totalProductosConStockBajo || 0) + (data.totalLotesPorVencer || 0);
            
            const ingresosTotales = parseFloat(data.ingresosTotales || 0);
            document.getElementById('kpiIngresosTotales').innerText = 'S/ ' + ingresosTotales.toFixed(2);
            
            // IGV Total (18% aproximado del total recaudado)
            const igvCalculado = ingresosTotales > 0 ? (ingresosTotales - (ingresosTotales / 1.18)) : 0;
            document.getElementById('kpiIgvTotal').innerText = 'S/ ' + igvCalculado.toFixed(2);
        }

        // 3. Cargar ventas para calcular métricas de HOY y tabla de Últimas Ventas
        const resVentas = await fetch(`${API_BASE}/ventas`, { headers: getAuthHeaders() });
        if (resVentas.ok) {
            const ventas = await resVentas.json();
            const hoyStr = new Date().toISOString().split('T')[0];

            const ventasHoy = ventas.filter(v => v.fecha && v.fecha.startsWith(hoyStr));
            const totalIngresosHoy = ventasHoy.reduce((acc, v) => acc + parseFloat(v.total || 0), 0);

            document.getElementById('kpiIngresosHoy').innerText = 'S/ ' + totalIngresosHoy.toFixed(2);
            document.getElementById('kpiVentasHoy').innerText = ventasHoy.length;

            renderizarUltimasVentasDashboard(ventas.slice(-5).reverse());
        }

        // 4. Cargar alerta rápida de stock
        await cargarResumenAlertasDashboard();

    } catch (err) {
        console.error('Error cargando Dashboard:', err);
    }
}

/**
 * Renderiza la tabla de Últimas Ventas en la vista de Dashboard (Estilo StockMaster)
 */
function renderizarUltimasVentasDashboard(ventas) {
    const tbody = document.querySelector('#tablaUltimasVentasDash tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (ventas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Sin ventas registradas.</td></tr>';
        return;
    }

    ventas.forEach(v => {
        const subtotal = parseFloat(v.total) / 1.18;
        const igv = parseFloat(v.total) - subtotal;
        const fechaFormato = v.fecha ? v.fecha.replace('T', ' ').substring(0, 16) : '-';

        tbody.innerHTML += `
            <tr>
                <td><strong>#${v.id}</strong></td>
                <td>${v.vendedor || 'Cliente General'}</td>
                <td><span class="badge bg-light text-dark border">EFECTIVO</span></td>
                <td>S/ ${subtotal.toFixed(2)}</td>
                <td>S/ ${igv.toFixed(2)}</td>
                <td><strong>S/ ${parseFloat(v.total).toFixed(2)}</strong></td>
                <td><span class="badge bg-success">Completado</span></td>
                <td><small class="text-muted">${fechaFormato}</small></td>
            </tr>
        `;
    });
}

/**
 * Carga el mensaje de estado de stock en el widget de Dashboard.
 */
async function cargarResumenAlertasDashboard() {
    try {
        const res = await fetch(`${API_BASE}/reportes/stock-bajo`, { headers: getAuthHeaders() });
        const prods = await res.json();
        const container = document.getElementById('widgetAlertasStockDash');
        if (!container) return;

        if (prods.length === 0) {
            container.innerHTML = `
                <div class="d-flex align-items-center gap-2 text-success fw-semibold py-2">
                    <i class="bi bi-check-circle-fill fs-5"></i> Todo el stock está en orden
                </div>
            `;
        } else {
            let itemsHtml = '';
            prods.slice(0, 3).forEach(p => {
                itemsHtml += `
                    <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                        <span><strong>${p.nombre}</strong> <small class="text-muted">(SKU: ${p.sku})</small></span>
                        <span class="badge bg-danger">${p.stockActual} disp. (Mín: ${p.stockMinimo})</span>
                    </div>
                `;
            });
            container.innerHTML = `
                <div class="text-danger fw-bold mb-2"><i class="bi bi-exclamation-triangle-fill me-1"></i> ${prods.length} productos con stock crítico:</div>
                ${itemsHtml}
            `;
        }
    } catch (e) {}
}

/**
 * Carga todos los informes de la pestaña Reportes & Decisiones.
 */
async function cargarReportes() {
    await cargarTopProductos();
    await cargarStockBajo();
    await cargarLotesPorVencer();
}

/**
 * Carga el Ranking de Productos Más Vendidos y sugerencias para Toma de Decisiones.
 */
async function cargarTopProductos() {
    try {
        const res = await fetch(`${API_BASE}/reportes/top-productos`, { headers: getAuthHeaders() });
        if (res.ok) {
            const topList = await res.json();
            const tbody = document.querySelector('#tablaTopProductos tbody');
            if (!tbody) return;
            tbody.innerHTML = '';

            if (topList.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No hay datos de ventas para analizar. Realiza ventas en el POS para generar el reporte.</td></tr>';
                return;
            }

            topList.forEach((item, index) => {
                const medalla = index === 0 ? '🥇 #1' : index === 1 ? '🥈 #2' : index === 2 ? '🥉 #3' : `#${index + 1}`;
                const badgeEstrategia = item.unidadesVendidas >= 10 || item.porcentajeVentas >= 25.0
                    ? '<span class="badge bg-danger-subtle text-danger border border-danger fw-bold">🔥 Alta Demanda / Estrella</span>'
                    : item.unidadesVendidas >= 5
                    ? '<span class="badge bg-warning-subtle text-warning-emphasis border border-warning fw-bold">⭐ Demanda Media</span>'
                    : '<span class="badge bg-secondary-subtle text-secondary border border-secondary fw-bold">📉 Baja Rotación</span>';

                tbody.innerHTML += `
                    <tr>
                        <td class="text-center fw-bold fs-6">${medalla}</td>
                        <td>
                            <strong>${item.nombre}</strong><br>
                            <small class="text-muted">SKU: <code>${item.sku}</code></small>
                        </td>
                        <td><span class="badge bg-light text-dark border">${item.categoriaNombre}</span></td>
                        <td class="text-center fw-bold fs-6">${item.unidadesVendidas} unid.</td>
                        <td class="text-end fw-bold text-dark">S/ ${parseFloat(item.totalRecaudado).toFixed(2)}</td>
                        <td class="text-center">
                            <div class="progress" style="height: 18px;">
                                <div class="progress-bar bg-danger progress-bar-striped" role="progressbar" style="width: ${item.porcentajeVentas}%;">
                                    ${item.porcentajeVentas}%
                                </div>
                            </div>
                        </td>
                        <td class="small">
                            ${badgeEstrategia}<br>
                            <span class="text-muted">${item.sugerenciaEstrategica}</span>
                        </td>
                    </tr>
                `;
            });
        }
    } catch (err) {
        mostrarToast('Error', 'No se pudo cargar el análisis de productos vendidos', 'danger');
    }
}

/**
 * Carga los productos con stock menor al mínimo.
 */
async function cargarStockBajo() {
    try {
        const res = await fetch(`${API_BASE}/reportes/stock-bajo`, { headers: getAuthHeaders() });
        if (res.ok) {
            const prods = await res.json();
            const tbody = document.querySelector('#tablaStockBajo tbody');
            if (!tbody) return;
            tbody.innerHTML = '';

            if (prods.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-success py-3"><i class="bi bi-check-circle me-1"></i> Todo el inventario se encuentra sobre el stock mínimo.</td></tr>';
                return;
            }

            prods.forEach(p => {
                tbody.innerHTML += `
                    <tr class="table-danger">
                        <td><strong>${p.nombre}</strong></td>
                        <td><code>${p.sku}</code></td>
                        <td class="text-center"><span class="badge bg-danger">${p.stockActual}</span></td>
                        <td class="text-center">${p.stockMinimo}</td>
                        <td class="text-center text-danger fw-bold">-${p.deficit}</td>
                    </tr>
                `;
            });
        }
    } catch (e) {}
}

/**
 * Carga los lotes que vencen en los próximos N días.
 */
async function cargarLotesPorVencer() {
    const dias = document.getElementById('filtroDiasVencer')?.value || 7;
    try {
        const res = await fetch(`${API_BASE}/reportes/por-vencer?dias=${dias}`, { headers: getAuthHeaders() });
        if (res.ok) {
            const lotes = await res.json();
            const tbody = document.querySelector('#tablaPorVencer tbody');
            if (!tbody) return;
            tbody.innerHTML = '';

            if (lotes.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" class="text-center text-success py-3"><i class="bi bi-check-circle me-1"></i> No hay lotes próximos a vencer en los siguientes ${dias} días.</td></tr>`;
                return;
            }

            lotes.forEach(l => {
                tbody.innerHTML += `
                    <tr class="table-warning">
                        <td>#${l.loteId}</td>
                        <td><strong>${l.productoNombre}</strong></td>
                        <td class="text-center fw-bold">${l.cantidad}</td>
                        <td>${l.fechaVencimiento}</td>
                        <td><span class="badge bg-warning text-dark">${l.estadoVencimiento}</span></td>
                    </tr>
                `;
            });
        }
    } catch (e) {}
}
