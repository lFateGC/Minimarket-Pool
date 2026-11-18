// ==============================================================================
// MODULO: PUNTO DE VENTA (POS) Y VENTAS CON ALGORITMO FEFO (pos.js)
// ==============================================================================

let carrito = [];

/**
 * Carga los productos y categorías para el Punto de Venta.
 */
async function cargarPOS() {
    await cargarCatalogo();
    await poblarSelectCategoriasPOS();
    if (typeof poblarSelectClientesPOS === 'function') {
        await poblarSelectClientesPOS();
    }
    filtrarProductosPOS();
}

/**
 * Llena el selector de categorías para el filtro del POS.
 */
async function poblarSelectCategoriasPOS() {
    try {
        const res = await fetch(`${API_BASE}/categorias`, { headers: getAuthHeaders() });
        if (res.ok) {
            const cats = await res.json();
            const sel = document.getElementById('posFiltroCategoria');
            if (sel) {
                sel.innerHTML = '<option value="">Todas las Categorías</option>';
                cats.forEach(c => sel.innerHTML += `<option value="${c.nombre}">${c.nombre}</option>`);
            }
        }
    } catch (e) {}
}

/**
 * Filtra los productos visibles en el POS por búsqueda de texto y categoría.
 */
function filtrarProductosPOS() {
    const query = (document.getElementById('posBusqueda')?.value || '').toLowerCase();
    const cat = document.getElementById('posFiltroCategoria')?.value || '';
    const container = document.getElementById('posGridProductos');
    if (!container) return;
    container.innerHTML = '';

    const filtrados = listaProductosGlobal.filter(p => {
        const matchTexto = p.nombre.toLowerCase().includes(query) || p.sku.toLowerCase().includes(query);
        const matchCat = cat === '' || p.categoriaNombre === cat;
        return matchTexto && matchCat;
    });

    if (filtrados.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted py-5">No se encontraron productos coincidentes.</div>';
        return;
    }

    filtrados.forEach(p => {
        const img = p.imagenUrl ? p.imagenUrl : 'https://placehold.co/200x120/f1f5f9/94a3b8?text=Sin+Foto';
        const sinStock = p.stockActual <= 0;

        container.innerHTML += `
            <div class="col-lg-4 col-md-6 col-sm-6">
                <div class="pos-prod-card-sm ${sinStock ? 'opacity-50' : ''}" onclick="${sinStock ? '' : `agregarAlCarrito(${p.id})`}" title="${sinStock ? 'Producto agotado' : 'Clic para agregar al carrito'}">
                    <div class="pos-img-box">
                        <img src="${img}" class="pos-img-sm" alt="${p.nombre}" onerror="this.src='https://placehold.co/200x120/f1f5f9/94a3b8?text=Sin+Foto'">
                    </div>
                    <div class="pos-prod-info">
                        <span class="pos-prod-categoria">${p.categoriaNombre || 'General'}</span>
                        <span class="pos-prod-nombre" title="${p.nombre}">${p.nombre}</span>
                    </div>
                    <div class="pos-prod-footer">
                        <span class="pos-prod-precio">S/ ${parseFloat(p.precioUnitario).toFixed(2)}</span>
                        <span class="pos-prod-stock-badge badge ${sinStock ? 'bg-danger' : 'bg-success'}">
                            ${sinStock ? 'Agotado' : p.stockActual + ' disp.'}
                        </span>
                    </div>
                </div>
            </div>
        `;
    });
}

/**
 * Añade un producto al carrito de compras verificando el stock disponible.
 */
function agregarAlCarrito(productoId) {
    const prod = listaProductosGlobal.find(p => p.id === productoId);
    if (!prod) return;

    const item = carrito.find(i => i.productoId === productoId);
    if (item) {
        if (item.cantidad + 1 > prod.stockActual) {
            mostrarToast('Aviso', `Stock insuficiente. Solo hay ${prod.stockActual} unidades.`, 'warning');
            return;
        }
        item.cantidad += 1;
    } else {
        carrito.push({
            productoId: prod.id,
            nombre: prod.nombre,
            precioUnitario: prod.precioUnitario,
            cantidad: 1
        });
    }
    renderizarCarrito();
}

/**
 * Dibuja la tabla del carrito de compras y actualiza el total.
 */
function renderizarCarrito() {
    const tbody = document.querySelector('#tablaCarrito tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    let total = 0;

    if (carrito.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">El carrito está vacío</td></tr>';
        document.getElementById('posTotalVenta').innerText = 'S/. 0.00';
        return;
    }

    carrito.forEach((item, index) => {
        const subtotal = item.precioUnitario * item.cantidad;
        total += subtotal;
        tbody.innerHTML += `
            <tr>
                <td class="text-truncate" style="max-width: 140px;"><strong>${item.nombre}</strong></td>
                <td class="text-center">
                    <div class="d-flex align-items-center justify-content-center gap-1">
                        <button class="btn btn-sm btn-light border p-0 px-1" onclick="modificarCantidadCarrito(${index}, -1)">-</button>
                        <span class="small fw-bold px-1">${item.cantidad}</span>
                        <button class="btn btn-sm btn-light border p-0 px-1" onclick="modificarCantidadCarrito(${index}, 1)">+</button>
                    </div>
                </td>
                <td class="text-end small">S/. ${parseFloat(item.precioUnitario).toFixed(2)}</td>
                <td class="text-end fw-bold text-dark">S/. ${subtotal.toFixed(2)}</td>
                <td class="text-center">
                    <button class="btn btn-sm text-danger p-0" onclick="quitarItemCarrito(${index})">
                        <i class="bi bi-x-circle-fill"></i>
                    </button>
                </td>
            </tr>
        `;
    });

    document.getElementById('posTotalVenta').innerText = `S/. ${total.toFixed(2)}`;
}

/**
 * Modifica la cantidad de un ítem en el carrito con botones +/-
 */
function modificarCantidadCarrito(index, delta) {
    const item = carrito[index];
    const prod = listaProductosGlobal.find(p => p.id === item.productoId);

    if (item.cantidad + delta <= 0) {
        quitarItemCarrito(index);
        return;
    }

    if (prod && item.cantidad + delta > prod.stockActual) {
        mostrarToast('Aviso', `Stock insuficiente. Solo hay ${prod.stockActual} unidades disponibles.`, 'warning');
        return;
    }

    item.cantidad += delta;
    renderizarCarrito();
}

/**
 * Elimina un ítem específico del carrito.
 */
function quitarItemCarrito(index) {
    carrito.splice(index, 1);
    renderizarCarrito();
}

/**
 * Vacía el carrito de compras.
 */
function vaciarCarrito() {
    carrito = [];
    renderizarCarrito();
}

/**
 * Procesa la venta atómica en el backend (POST /api/ventas).
 */
async function procesarVenta() {
    if (carrito.length === 0) {
        mostrarToast('Aviso', 'Agrega productos al carrito para cobrar', 'warning');
        return;
    }

    const clienteIdVal = document.getElementById('posSelectCliente')?.value;
    const payload = {
        clienteId: clienteIdVal ? parseInt(clienteIdVal) : null,
        items: carrito.map(i => ({ productoId: i.productoId, cantidad: i.cantidad }))
    };

    try {
        const res = await fetch(`${API_BASE}/ventas`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast('Venta Exitosa', `Venta #${data.id} procesada y registrada correctamente.`, 'success');
            imprimirTicketVenta(data);
            carrito = [];
            renderizarCarrito();
            cargarPOS();
        } else {
            mostrarToast('Error al Vender', data.mensaje || 'Stock insuficiente en los lotes disponibles.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo procesar la venta', 'danger');
    }
}

// Alias para compatibilidad
const procesarVentaFefo = procesarVenta;

/**
 * Muestra el comprobante de pago / ticket digital de la venta realizada.
 */
function imprimirTicketVenta(venta) {
    let detallesHtml = '';
    venta.detalles.forEach(d => {
        const sub = d.precioUnitario * d.cantidad;
        detallesHtml += `
            <div class="d-flex justify-content-between small">
                <span>${d.cantidad}x ${d.productoNombre}</span>
                <span>S/. ${sub.toFixed(2)}</span>
            </div>
        `;
    });

    const nomCliente = venta.clienteNombre || 'Público General';
    const docCliente = venta.clienteDocumento && venta.clienteDocumento !== '-' ? ` (${venta.clienteDocumento})` : '';

    document.getElementById('ticketVentaBody').innerHTML = `
        <div class="ticket-box text-center">
            <h5 class="fw-bold text-danger mb-0">MINIMARKET POOL</h5>
            <small class="text-muted d-block">Tu ahorro es nuestra prioridad</small>
            <div class="border-top border-bottom py-2 my-2 text-start small">
                <strong>Ticket Venta:</strong> #${venta.id}<br>
                <strong>Fecha:</strong> ${venta.fecha.replace('T', ' ').substring(0, 19)}<br>
                <strong>Vendedor:</strong> ${venta.vendedor}<br>
                <strong>Cliente:</strong> ${nomCliente}${docCliente}
            </div>
            <div class="text-start mb-2">
                ${detallesHtml}
            </div>
            <div class="border-top pt-2 d-flex justify-content-between fw-bold fs-6">
                <span>TOTAL:</span>
                <span class="text-danger">S/. ${parseFloat(venta.total).toFixed(2)}</span>
            </div>
            <small class="text-muted d-block mt-3">¡Gracias por su preferencia!</small>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('modalTicketVenta'));
    modal.show();
}
