// ==============================================================================
// MODULO: HISTORIAL DE BOLETAS Y BOLETA DETALLES (boletas.js)
// ==============================================================================

let listaVentasGlobal = [];

/**
 * Carga el historial de boletas/ventas desde el backend.
 */
async function cargarHistorialBoletas() {
    try {
        const endpoint = (usuarioActual && usuarioActual.rol === 'ADMIN') ? '/ventas' : '/ventas/mis-ventas';
        const res = await fetch(`${API_BASE}${endpoint}`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaVentasGlobal = await res.json();
            renderizarTablaBoletas(listaVentasGlobal);
        }
    } catch (err) {
        mostrarToast('Error', 'No se pudo cargar el historial de boletas', 'danger');
    }
}

/**
 * Renderiza la tabla de boletas con opción de ver boletaDetalles.
 */
function renderizarTablaBoletas(ventas) {
    const tbody = document.querySelector('#tablaBoletas tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (ventas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No se han registrado boletas de venta aún.</td></tr>';
        return;
    }

    ventas.slice().reverse().forEach(v => {
        const cantTotalItems = v.detalles ? v.detalles.reduce((acc, d) => acc + d.cantidad, 0) : 0;
        const fechaFormateada = v.fecha ? v.fecha.replace('T', ' ').substring(0, 19) : '-';

        // Resumen rápido de productos vendidos en esta boleta
        const nombresProductos = v.detalles ? v.detalles.map(d => `${d.cantidad}x ${d.productoNombre}`).join(', ') : 'Sin detalle';
        const nomCliente = v.clienteNombre || 'Público General';
        const docCliente = v.clienteDocumento && v.clienteDocumento !== '-' ? `<br><small class="text-muted"><i class="bi bi-card-text me-1"></i>${v.clienteDocumento}</small>` : '';

        tbody.innerHTML += `
            <tr>
                <td><span class="badge bg-dark fs-6">#${v.id}</span></td>
                <td><i class="bi bi-clock me-1 text-muted"></i>${fechaFormateada}</td>
                <td>
                    <span class="fw-semibold text-dark"><i class="bi bi-person me-1 text-danger"></i>${nomCliente}</span>
                    ${docCliente}
                </td>
                <td><span class="badge bg-light text-dark border"><i class="bi bi-person-circle me-1"></i>${v.vendedor}</span></td>
                <td>
                    <span class="fw-bold d-block text-dark">${cantTotalItems} unid.</span>
                    <small class="text-muted text-truncate d-inline-block" style="max-width: 240px;" title="${nombresProductos}">
                        ${nombresProductos}
                    </small>
                </td>
                <td class="text-end fw-bold text-success fs-6">S/. ${parseFloat(v.total).toFixed(2)}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-brand px-3 py-1 shadow-sm" onclick="verBoletaDetalles(${v.id})">
                        <i class="bi bi-receipt me-1"></i> Ver Boleta
                    </button>
                </td>
            </tr>
        `;
    });
}

/**
 * Muestra el modal desglosado con todos los productos vendidos (boletaDetalles)
 */
function verBoletaDetalles(ventaId) {
    const venta = listaVentasGlobal.find(v => v.id === ventaId);
    if (!venta) return;

    const fechaFormateada = venta.fecha ? venta.fecha.replace('T', ' ').substring(0, 19) : '-';
    let filasDetalles = '';

    venta.detalles.forEach((d, idx) => {
        const sub = d.subtotal ? d.subtotal : (d.precioUnitario * d.cantidad);
        filasDetalles += `
            <tr>
                <td class="text-center text-muted">${idx + 1}</td>
                <td>
                    <strong>${d.productoNombre}</strong><br>
                    <small class="text-muted">SKU: <code>${d.sku || 'N/A'}</code></small>
                </td>
                <td><span class="badge bg-light text-dark border">${d.categoriaNombre || 'General'}</span></td>
                <td class="text-center fw-bold">${d.cantidad}</td>
                <td class="text-end">S/. ${parseFloat(d.precioUnitario).toFixed(2)}</td>
                <td class="text-end fw-bold text-dark">S/. ${parseFloat(sub).toFixed(2)}</td>
            </tr>
        `;
    });

    document.getElementById('boletaDetallesModalContent').innerHTML = `
        <div class="p-3 bg-light rounded-3 mb-3 border">
            <div class="row g-2 align-items-center">
                <div class="col-sm-6">
                    <h5 class="fw-bold text-danger mb-0">BOLETA DE VENTA #${venta.id}</h5>
                    <small class="text-muted">Minimarket Pool — Tu ahorro es nuestra prioridad</small>
                </div>
                <div class="col-sm-6 text-sm-end">
                    <div><strong>Fecha:</strong> ${fechaFormateada}</div>
                    <div><strong>Cliente:</strong> <span class="badge bg-secondary-subtle text-dark border">${venta.clienteNombre || 'Público General'} (${venta.clienteDocumento || 'S/D'})</span></div>
                    <div><strong>Atendido por:</strong> <span class="badge bg-primary-subtle text-primary">${venta.vendedor}</span></div>
                </div>
            </div>
        </div>

        <h6 class="fw-bold text-dark mb-2"><i class="bi bi-box-seam me-1 text-danger"></i> Productos Incluidos en la Boleta (boletaDetalles):</h6>
        <div class="table-responsive border rounded-3 mb-3">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="text-center" style="width: 40px;">#</th>
                        <th>Producto</th>
                        <th>Categoría</th>
                        <th class="text-center">Cantidad</th>
                        <th class="text-end">P. Unitario</th>
                        <th class="text-end">Subtotal</th>
                    </tr>
                </thead>
                <tbody>
                    ${filasDetalles}
                </tbody>
            </table>
        </div>

        <div class="d-flex justify-content-between align-items-center p-3 bg-danger-subtle rounded-3 border border-danger-subtle">
            <span class="fw-bold fs-6 text-danger">TOTAL DE LA BOLETA:</span>
            <span class="fw-bold fs-4 text-danger">S/. ${parseFloat(venta.total).toFixed(2)}</span>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('modalDetalleBoletaCompleta'));
    modal.show();
}

/**
 * Filtra las boletas por texto de búsqueda.
 */
function filtrarBoletas() {
    const query = (document.getElementById('busquedaBoleta')?.value || '').toLowerCase();
    const filtradas = listaVentasGlobal.filter(v => {
        const matchId = v.id.toString().includes(query);
        const matchVendedor = v.vendedor.toLowerCase().includes(query);
        const matchProds = v.detalles && v.detalles.some(d => d.productoNombre.toLowerCase().includes(query));
        return matchId || matchVendedor || matchProds;
    });
    renderizarTablaBoletas(filtradas);
}
