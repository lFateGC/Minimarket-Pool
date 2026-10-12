// ==============================================================================
// MODULO DE ADMINISTRACIÓN: INVENTARIO, LOTES FEFO, MERMAS Y KARDEX (inventario.js)
// ==============================================================================

let listaMovimientosGlobal = [];

/**
 * Carga los datos necesarios para la gestión de inventario y lotes.
 */
async function cargarInventario() {
    await poblarSelectProductosLote();
    await cargarMovimientos();
    await cargarLotesAlmacen();
}

/**
 * Llena el selector de productos en el formulario de entrada de lotes.
 */
async function poblarSelectProductosLote() {
    try {
        const res = await fetch(`${API_BASE}/productos`, { headers: getAuthHeaders() });
        if (res.ok) {
            const prods = await res.json();
            const sel = document.getElementById('loteProductoId');
            if (sel) {
                sel.innerHTML = '<option value="">Seleccione un producto...</option>';
                prods.forEach(p => {
                    sel.innerHTML += `<option value="${p.id}">${p.nombre} (SKU: ${p.sku})</option>`;
                });
            }
        }
    } catch (e) {}
}

/**
 * Registra una nueva entrada de compra / lote con fecha de vencimiento (POST /api/inventario/lote).
 */
async function ejecutarIngresoLote(e) {
    e.preventDefault();
    const productoId = parseInt(document.getElementById('loteProductoId').value);
    const cantidad = parseInt(document.getElementById('loteCantidad').value);
    const fechaVencimiento = document.getElementById('loteFechaVence').value;

    const payload = { productoId, cantidad, fechaVencimiento };

    try {
        const res = await fetch(`${API_BASE}/inventario/lote`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok) {
            mostrarToast('Lote Registrado', `Se ingresaron ${cantidad} unidades al lote #${data.id} con vencimiento el ${fechaVencimiento}.`, 'success');
            document.getElementById('loteCantidad').value = '';
            document.getElementById('loteFechaVence').value = '';
            cargarInventario();
            if (typeof cargarCatalogo === 'function') cargarCatalogo();
        } else {
            mostrarToast('Error en Entrada', data.mensaje || 'Fecha de vencimiento o datos inválidos.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo comunicar con el servidor', 'danger');
    }
}

/**
 * Registra una merma o baja física de stock (POST /api/inventario/merma).
 */
async function ejecutarRegistroMerma(e) {
    e.preventDefault();
    const loteId = parseInt(document.getElementById('mermaLoteId').value);
    const cantidad = parseInt(document.getElementById('mermaCantidad').value);
    const motivo = document.getElementById('mermaMotivo').value.trim();

    const payload = { loteId, cantidad, motivo };

    try {
        const res = await fetch(`${API_BASE}/inventario/merma`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast('Merma Registrada', `Se dieron de baja ${cantidad} unidades del lote #${loteId}.`, 'info');
            document.getElementById('mermaLoteId').value = '';
            document.getElementById('mermaCantidad').value = '';
            document.getElementById('mermaMotivo').value = '';
            cargarInventario();
            if (typeof cargarCatalogo === 'function') cargarCatalogo();
        } else {
            const data = await res.json();
            mostrarToast('Error al Registrar Merma', data.mensaje || 'El stock del lote es insuficiente.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo procesar la merma', 'danger');
    }
}

/**
 * Carga el Kardex de movimientos de auditoría desde GET /api/inventario/movimientos
 */
async function cargarMovimientos() {
    try {
        const res = await fetch(`${API_BASE}/inventario/movimientos`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaMovimientosGlobal = await res.json();
            filtrarMovimientos();
        }
    } catch (err) {
        mostrarToast('Error', 'No se pudo cargar el Kardex de movimientos', 'danger');
    }
}

/**
 * Filtra los movimientos del Kardex por tipo y texto de búsqueda.
 */
function filtrarMovimientos() {
    const tipoFiltro = document.getElementById('filtroTipoKardex')?.value || '';
    const query = (document.getElementById('busquedaKardex')?.value || '').toLowerCase();
    const tbody = document.querySelector('#tablaKardex tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    const filtrados = listaMovimientosGlobal.filter(m => {
        const matchTipo = tipoFiltro === '' || m.tipo === tipoFiltro;
        const matchTexto = m.productoNombre.toLowerCase().includes(query) || (m.motivo || '').toLowerCase().includes(query);
        return matchTipo && matchTexto;
    });

    if (filtrados.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No se encontraron movimientos registrados.</td></tr>';
        return;
    }

    filtrados.forEach(m => {
        let badgeTipo = '';
        if (m.tipo === 'ENTRADA') {
            badgeTipo = '<span class="badge bg-success-subtle text-success border border-success fw-bold"><i class="bi bi-arrow-down-left me-1"></i> ENTRADA (COMPRA)</span>';
        } else if (m.tipo === 'SALIDA_VENTA') {
            badgeTipo = '<span class="badge bg-primary-subtle text-primary border border-primary fw-bold"><i class="bi bi-arrow-up-right me-1"></i> SALIDA (VENTA)</span>';
        } else if (m.tipo === 'MERMA') {
            badgeTipo = '<span class="badge bg-danger-subtle text-danger border border-danger fw-bold"><i class="bi bi-trash me-1"></i> MERMA / BAJA</span>';
        } else {
            badgeTipo = `<span class="badge bg-secondary">${m.tipo}</span>`;
        }

        const fechaFormateada = m.fecha ? m.fecha.replace('T', ' ').substring(0, 19) : '-';

        tbody.innerHTML += `
            <tr>
                <td><small class="text-muted"><i class="bi bi-clock me-1"></i>${fechaFormateada}</small></td>
                <td>${badgeTipo}</td>
                <td><strong>${m.productoNombre}</strong></td>
                <td><span class="badge bg-light text-dark border">Lote #${m.loteId}</span></td>
                <td class="text-center fw-bold fs-6">${m.cantidad} unid.</td>
                <td><small class="text-muted">${m.motivo || 'Operación regular'}</small></td>
            </tr>
        `;
    });
}

/**
 * Carga los lotes vigentes en almacén para control de caducidad.
 */
async function cargarLotesAlmacen() {
    try {
        const res = await fetch(`${API_BASE}/reportes/por-vencer?dias=365`, { headers: getAuthHeaders() });
        if (res.ok) {
            const lotes = await res.json();
            const tbody = document.querySelector('#tablaLotesAlmacen tbody');
            if (!tbody) return;
            tbody.innerHTML = '';

            if (lotes.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">No hay lotes con stock en almacén.</td></tr>';
                return;
            }

            lotes.forEach(l => {
                const badgeEstado = l.diasParaVencer <= 3
                    ? '<span class="badge bg-danger">VENCE PRONTO</span>'
                    : l.diasParaVencer <= 15
                    ? '<span class="badge bg-warning text-dark">ALERTA MEDIA</span>'
                    : '<span class="badge bg-success">ÓPTIMO</span>';

                tbody.innerHTML += `
                    <tr>
                        <td><strong>#${l.loteId}</strong></td>
                        <td>${l.productoNombre} <small class="text-muted">(SKU: ${l.sku})</small></td>
                        <td class="text-center fw-bold text-dark">${l.cantidad} unid.</td>
                        <td>${l.fechaIngreso || '-'}</td>
                        <td><strong class="text-danger">${l.fechaVencimiento}</strong></td>
                        <td class="text-center">${badgeEstado}</td>
                    </tr>
                `;
            });
        }
    } catch (e) {}
}
