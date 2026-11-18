// ==============================================================================
// MODULO: TIENDA VIRTUAL Y PORTAL DE CLIENTES (cliente.js)
// Minimarket Pool — "Tu ahorro es nuestra prioridad"
// ==============================================================================

// Configuración de API
const API_BASE = localStorage.getItem('api_base') || (
    (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080'))
        ? 'http://localhost:8080/api'
        : '/api'
);

let productosClienteGlobal = [];
let categoriasClienteGlobal = [];
let categoriaSeleccionada = '';
let carritoCliente = JSON.parse(localStorage.getItem('pool_cart') || '[]');
let tokenJWT = localStorage.getItem('jwt_token') || null;
let usuarioActual = JSON.parse(localStorage.getItem('jwt_user') || 'null');

/**
 * Encabezados de autorización para peticiones autenticadas.
 */
function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (tokenJWT) {
        headers['Authorization'] = `Bearer ${tokenJWT}`;
    }
    return headers;
}

/**
 * Toast de notificaciones para la tienda virtual.
 */
function mostrarToastCliente(titulo, mensaje, tipo = 'info') {
    const container = document.getElementById('toastContainerCliente');
    if (!container) return;

    const colorBorder = tipo === 'success' ? '#10b981' : tipo === 'danger' ? '#ef4444' : tipo === 'warning' ? '#f59e0b' : '#0284c7';
    const icono = tipo === 'success' ? 'bi-check-circle-fill' : tipo === 'danger' ? 'bi-exclamation-octagon-fill' : tipo === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill';

    const toastEl = document.createElement('div');
    toastEl.style.cssText = `
        background: #ffffff;
        color: #0f172a;
        border-left: 4px solid ${colorBorder};
        border-radius: 10px;
        box-shadow: 0 10px 25px rgba(0,0,0,0.12);
        padding: 12px 16px;
        min-width: 280px;
        max-width: 360px;
        display: flex;
        gap: 12px;
        align-items: flex-start;
        border: 1px solid #e2e8f0;
        margin-bottom: 8px;
    `;

    toastEl.innerHTML = `
        <i class="bi ${icono} fs-5" style="color: ${colorBorder}"></i>
        <div style="flex: 1;">
            <strong style="display: block; font-size: 0.88rem; font-family: 'Plus Jakarta Sans', sans-serif;">${titulo}</strong>
            <small style="color: #64748b; font-size: 0.82rem; line-height: 1.3; display: block; margin-top: 2px;">${mensaje}</small>
        </div>
        <button style="background:none; border:none; color:#94a3b8; font-size:1.1rem; cursor:pointer;" onclick="this.parentElement.remove()">×</button>
    `;

    container.appendChild(toastEl);
    setTimeout(() => {
        toastEl.style.opacity = '0';
        toastEl.style.transform = 'translateX(20px)';
        toastEl.style.transition = 'all 0.35s';
        setTimeout(() => toastEl.remove(), 350);
    }, 4000);
}

// ==============================================================================
// 1. CARGA INICIAL Y CATÁLOGO
// ==============================================================================

document.addEventListener('DOMContentLoaded', async () => {
    actualizarUINavbarUsuario();
    actualizarBadgeCarrito();
    await cargarCategoriasCliente();
    await cargarProductosCliente();
});

/**
 * Carga las categorías desde el backend Spring Boot.
 */
async function cargarCategoriasCliente() {
    try {
        const res = await fetch(`${API_BASE}/categorias`);
        if (res.ok) {
            categoriasClienteGlobal = await res.json();
            renderizarPillsCategorias();
        }
    } catch (e) {
        console.warn('Error al cargar categorías para tienda:', e);
    }
}

/**
 * Renderiza los botones de categoría (pills).
 */
function renderizarPillsCategorias() {
    const container = document.getElementById('categoryScrollContainer');
    if (!container) return;

    container.innerHTML = `
        <button class="cat-pill-btn ${categoriaSeleccionada === '' ? 'activo' : ''}" onclick="seleccionarCategoria('')">
            <i class="bi bi-grid-fill"></i> Todos los Productos
        </button>
    `;

    categoriasClienteGlobal.forEach(cat => {
        const esActivo = categoriaSeleccionada === cat.nombre ? 'activo' : '';
        container.innerHTML += `
            <button class="cat-pill-btn ${esActivo}" onclick="seleccionarCategoria('${cat.nombre}')">
                <i class="bi bi-tag-fill text-danger"></i> ${cat.nombre}
            </button>
        `;
    });
}

function seleccionarCategoria(catNombre) {
    categoriaSeleccionada = catNombre;
    renderizarPillsCategorias();
    filtrarProductosCliente();
}

/**
 * Carga todos los productos desde el backend.
 */
async function cargarProductosCliente() {
    const grid = document.getElementById('gridProductosCliente');
    if (grid) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-danger" role="status"></div>
                <p class="text-muted mt-2">Cargando productos frescos y ofertas...</p>
            </div>
        `;
    }

    try {
        const res = await fetch(`${API_BASE}/productos`);
        if (res.ok) {
            productosClienteGlobal = await res.json();
            filtrarProductosCliente();
        } else {
            if (grid) {
                grid.innerHTML = `
                    <div class="col-12 text-center py-5 text-muted">
                        <i class="bi bi-wifi-off fs-1 d-block mb-2 text-danger"></i>
                        <h5>No se pudo conectar con el catálogo</h5>
                        <p class="small">Verifica que el servidor Spring Boot esté encendido en <code>${API_BASE}</code></p>
                        <button class="btn btn-sm btn-outline-danger mt-2" onclick="cargarProductosCliente()">Reintentar</button>
                    </div>
                `;
            }
        }
    } catch (err) {
        if (grid) {
            grid.innerHTML = `
                <div class="col-12 text-center py-5 text-muted">
                    <i class="bi bi-exclamation-triangle-fill fs-1 d-block mb-2 text-warning"></i>
                    <h5>Error de conexión con Minimarket Pool</h5>
                    <p class="small">No se pudo alcanzar el servidor backend en <code>${API_BASE}</code>.</p>
                    <button class="btn btn-sm btn-outline-danger mt-2" onclick="cargarProductosCliente()">Reintentar</button>
                </div>
            `;
        }
    }
}

/**
 * Filtra los productos en pantalla por texto y categoría.
 */
function filtrarProductosCliente() {
    const query = (document.getElementById('inputBusquedaCliente')?.value || '').toLowerCase().trim();
    const grid = document.getElementById('gridProductosCliente');
    if (!grid) return;

    const filtrados = productosClienteGlobal.filter(p => {
        const matchTexto = p.nombre.toLowerCase().includes(query) || (p.sku && p.sku.toLowerCase().includes(query));
        const matchCat = categoriaSeleccionada === '' || p.categoriaNombre === categoriaSeleccionada;
        return matchTexto && matchCat;
    });

    if (filtrados.length === 0) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-search fs-1 text-muted d-block mb-2"></i>
                <h5 class="fw-bold text-secondary">No encontramos productos coincidentes</h5>
                <p class="text-muted small">Intenta buscar con otra palabra o revisa otra categoría.</p>
                <button class="btn btn-sm btn-outline-danger" onclick="document.getElementById('inputBusquedaCliente').value=''; seleccionarCategoria('');">
                    Limpiar Búsqueda
                </button>
            </div>
        `;
        return;
    }

    grid.innerHTML = '';
    filtrados.forEach(p => {
        const sinStock = p.stockActual <= 0;
        const imagen = p.imagenUrl ? p.imagenUrl : 'https://placehold.co/400x300/f8fafc/94a3b8?text=Minimarket+Pool';
        const badgeStock = sinStock
            ? '<span class="prod-stock-badge bg-danger text-white">Agotado</span>'
            : p.stockActual <= (p.stockMinimo || 5)
            ? `<span class="prod-stock-badge bg-warning text-dark">¡Últimas ${p.stockActual} u.!</span>`
            : `<span class="prod-stock-badge bg-success text-white">${p.stockActual} disponibles</span>`;

        grid.innerHTML += `
            <div class="col-xl-3 col-lg-4 col-md-6 col-sm-6">
                <div class="prod-card-cliente">
                    <div class="prod-img-wrap">
                        <img src="${imagen}" alt="${p.nombre}" loading="lazy" onerror="this.src='https://placehold.co/400x300/f8fafc/94a3b8?text=Minimarket+Pool'">
                        <span class="prod-cat-badge">${p.categoriaNombre || 'General'}</span>
                        ${badgeStock}
                    </div>
                    <div class="prod-body">
                        <h6 class="prod-titulo" title="${p.nombre}">${p.nombre}</h6>
                        <span class="prod-sku">SKU: ${p.sku || '-'}</span>
                        <div class="d-flex align-items-baseline justify-content-between my-2">
                            <span class="prod-precio">S/ ${parseFloat(p.precioUnitario).toFixed(2)}</span>
                            <small class="text-muted">Unidad</small>
                        </div>
                        <div class="prod-footer-action">
                            <button class="btn-agregar-carrito" onclick="agregarAlCarritoCliente(${p.id})" ${sinStock ? 'disabled' : ''}>
                                <i class="bi bi-cart-plus-fill"></i> ${sinStock ? 'Agotado' : 'Añadir al Carrito'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });
}

// ==============================================================================
// 2. CARRITO DE COMPRAS
// ==============================================================================

function guardarCarrito() {
    localStorage.setItem('pool_cart', JSON.stringify(carritoCliente));
    actualizarBadgeCarrito();
    renderizarOffcanvasCarrito();
}

function actualizarBadgeCarrito() {
    const totalItems = carritoCliente.reduce((acc, item) => acc + item.cantidad, 0);
    const badge = document.getElementById('badgeTotalCarrito');
    if (badge) badge.innerText = totalItems;

    const totalMonto = carritoCliente.reduce((acc, item) => acc + (item.cantidad * item.precioUnitario), 0);
    const totalNav = document.getElementById('montoNavCarrito');
    if (totalNav) totalNav.innerText = `S/ ${totalMonto.toFixed(2)}`;
}

function agregarAlCarritoCliente(productoId) {
    const prod = productosClienteGlobal.find(p => p.id === productoId);
    if (!prod) return;

    if (prod.stockActual <= 0) {
        mostrarToastCliente('Sin Stock', 'Este producto está agotado temporalmente.', 'warning');
        return;
    }

    const item = carritoCliente.find(i => i.productoId === productoId);
    if (item) {
        if (item.cantidad + 1 > prod.stockActual) {
            mostrarToastCliente('Límite de Stock', `Solo hay ${prod.stockActual} unidades disponibles en tienda.`, 'warning');
            return;
        }
        item.cantidad += 1;
    } else {
        carritoCliente.push({
            productoId: prod.id,
            nombre: prod.nombre,
            sku: prod.sku,
            imagenUrl: prod.imagenUrl,
            precioUnitario: parseFloat(prod.precioUnitario),
            stockActual: prod.stockActual,
            cantidad: 1
        });
    }

    guardarCarrito();
    mostrarToastCliente('Agregado al Carrito', `${prod.nombre} (1 unidad)`, 'success');
}

function cambiarCantidadCliente(productoId, delta) {
    const item = carritoCliente.find(i => i.productoId === productoId);
    if (!item) return;

    const prod = productosClienteGlobal.find(p => p.id === productoId);
    const stockMax = prod ? prod.stockActual : item.stockActual;

    if (delta > 0 && item.cantidad + delta > stockMax) {
        mostrarToastCliente('Límite de Stock', `No puedes agregar más de ${stockMax} unidades.`, 'warning');
        return;
    }

    item.cantidad += delta;
    if (item.cantidad <= 0) {
        eliminarDelCarritoCliente(productoId);
        return;
    }

    guardarCarrito();
}

function eliminarDelCarritoCliente(productoId) {
    carritoCliente = carritoCliente.filter(i => i.productoId !== productoId);
    guardarCarrito();
}

function vaciarCarritoCliente() {
    if (carritoCliente.length === 0) return;
    if (confirm('¿Deseas vaciar todos los productos de tu carrito?')) {
        carritoCliente = [];
        guardarCarrito();
        mostrarToastCliente('Carrito Vacío', 'Se eliminaron todos los productos.', 'info');
    }
}

function renderizarOffcanvasCarrito() {
    const container = document.getElementById('cartItemsContainer');
    if (!container) return;

    if (carritoCliente.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5">
                <i class="bi bi-cart-x fs-1 text-muted d-block mb-3"></i>
                <h6 class="fw-bold text-secondary">Tu carrito está vacío</h6>
                <p class="text-muted small">Explora nuestros productos y ahorra con Minimarket Pool.</p>
                <button class="btn btn-sm btn-danger mt-2" data-bs-dismiss="offcanvas">Ver Ofertas</button>
            </div>
        `;
        document.getElementById('cartSubtotal').innerText = 'S/ 0.00';
        document.getElementById('cartIgv').innerText = 'S/ 0.00';
        document.getElementById('cartTotal').innerText = 'S/ 0.00';
        document.getElementById('btnIniciarCheckout').disabled = true;
        return;
    }

    let subtotalSinIgv = 0;
    let totalGeneral = 0;

    container.innerHTML = '';
    carritoCliente.forEach(item => {
        const itemTotal = item.cantidad * item.precioUnitario;
        totalGeneral += itemTotal;
        const img = item.imagenUrl ? item.imagenUrl : 'https://placehold.co/100x100/f8fafc/94a3b8?text=Pool';

        container.innerHTML += `
            <div class="cart-item-row">
                <img src="${img}" class="cart-item-img" alt="${item.nombre}" onerror="this.src='https://placehold.co/100x100/f8fafc/94a3b8?text=Pool'">
                <div class="cart-item-info">
                    <div class="cart-item-title" title="${item.nombre}">${item.nombre}</div>
                    <div class="text-danger fw-bold small">S/ ${item.precioUnitario.toFixed(2)} c/u</div>
                    <div class="d-flex align-items-center justify-content-between mt-2">
                        <div class="cart-stepper">
                            <button onclick="cambiarCantidadCliente(${item.productoId}, -1)">-</button>
                            <span>${item.cantidad}</span>
                            <button onclick="cambiarCantidadCliente(${item.productoId}, 1)">+</button>
                        </div>
                        <span class="fw-bold fs-6">S/ ${itemTotal.toFixed(2)}</span>
                    </div>
                </div>
                <button class="btn btn-sm btn-link text-danger p-0 ms-2" onclick="eliminarDelCarritoCliente(${item.productoId})" title="Quitar producto">
                    <i class="bi bi-trash fs-5"></i>
                </button>
            </div>
        `;
    });

    // Desglose IGV 18% incluido
    subtotalSinIgv = totalGeneral / 1.18;
    const igv = totalGeneral - subtotalSinIgv;

    document.getElementById('cartSubtotal').innerText = `S/ ${subtotalSinIgv.toFixed(2)}`;
    document.getElementById('cartIgv').innerText = `S/ ${igv.toFixed(2)}`;
    document.getElementById('cartTotal').innerText = `S/ ${totalGeneral.toFixed(2)}`;
    document.getElementById('btnIniciarCheckout').disabled = false;
}

// ==============================================================================
// 3. CHECKOUT Y FINALIZACIÓN DE COMPRA
// ==============================================================================

async function abrirCheckoutModal() {
    if (carritoCliente.length === 0) {
        mostrarToastCliente('Carrito Vacío', 'Agrega al menos un producto para proceder al pago.', 'warning');
        return;
    }

    // Cerrar offcanvas
    const offcanvasEl = document.getElementById('offcanvasCarrito');
    const offcanvasInstance = bootstrap.Offcanvas.getInstance(offcanvasEl);
    if (offcanvasInstance) offcanvasInstance.hide();

    // Si no está autenticado, solicitar inicio de sesión o registro
    if (!tokenJWT || !usuarioActual || usuarioActual.rol !== 'CLIENTE') {
        mostrarToastCliente('Inicia Sesión', 'Debes ingresar a tu cuenta de cliente para completar el pedido.', 'info');
        abrirModalAuthCliente('login');
        return;
    }

    // Cargar datos de cliente autenticado para autofill
    try {
        const res = await fetch(`${API_BASE}/clientes/mi-perfil`, { headers: getAuthHeaders() });
        if (res.ok) {
            const perfil = await res.json();
            document.getElementById('checkoutNombre').value = perfil.nombreCompleto || `${perfil.nombres} ${perfil.apellidos || ''}`.trim();
            document.getElementById('checkoutDoc').value = `${perfil.tipoDocumento}: ${perfil.numeroDocumento}`;
            document.getElementById('checkoutTelefono').value = perfil.telefono || '';
            document.getElementById('checkoutDireccion').value = perfil.direccion || '';
        }
    } catch (e) {}

    // Resumen de pago
    const totalGeneral = carritoCliente.reduce((acc, item) => acc + (item.cantidad * item.precioUnitario), 0);
    document.getElementById('checkoutTotalMonto').innerText = `S/ ${totalGeneral.toFixed(2)}`;

    actualizarInstruccionesPago();

    const modal = new bootstrap.Modal(document.getElementById('modalCheckout'));
    modal.show();
}

function actualizarInstruccionesPago() {
    const metodo = document.getElementById('checkoutMetodoPago').value;
    const instrucciones = document.getElementById('instruccionesPago');
    
    if (metodo === 'YAPE' || metodo === 'PLIN') {
        instrucciones.innerHTML = `
            <div class="alert alert-warning border-warning d-flex align-items-center gap-3 py-2 px-3 rounded-3">
                <i class="bi bi-qr-code-scan fs-3 text-dark"></i>
                <div class="small">
                    <strong>Paga al instante con ${metodo}:</strong><br>
                    Número oficial: <strong>987 654 321</strong> (Minimarket Pool S.A.C.)<br>
                    Tu pedido será despachado inmediatamente.
                </div>
            </div>
        `;
    } else if (metodo === 'EFECTIVO') {
        instrucciones.innerHTML = `
            <div class="alert alert-success border-success d-flex align-items-center gap-3 py-2 px-3 rounded-3">
                <i class="bi bi-cash-coin fs-3 text-success"></i>
                <div class="small">
                    <strong>Pago Contra Entrega:</strong><br>
                    Pagas en efectivo al repartidor cuando recibas tus productos en tu domicilio.
                </div>
            </div>
        `;
    } else {
        instrucciones.innerHTML = `
            <div class="alert alert-info border-info d-flex align-items-center gap-3 py-2 px-3 rounded-3">
                <i class="bi bi-credit-card fs-3 text-info"></i>
                <div class="small">
                    <strong>${metodo}:</strong><br>
                    El repartidor llevará POS inalámbrico para tarjeta o te contactaremos con la cuenta bancaria BCP/BBVA.
                </div>
            </div>
        `;
    }
}

/**
 * Envía el pedido al backend (POST /api/ventas/mi-compra).
 */
async function procesarCompraFinal(e) {
    e.preventDefault();

    if (carritoCliente.length === 0) return;

    const metodoPago = document.getElementById('checkoutMetodoPago').value;
    const direccion = document.getElementById('checkoutDireccion').value.trim();
    const telefono = document.getElementById('checkoutTelefono').value.trim();
    const btnSubmit = document.getElementById('btnConfirmarPedido');

    const items = carritoCliente.map(i => ({
        productoId: i.productoId,
        cantidad: i.cantidad
    }));

    const payload = {
        metodoPago: metodoPago,
        items: items
    };

    try {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Confirmando Pedido...';

        // Actualizar dirección si la modificó
        if (direccion || telefono) {
            try {
                await fetch(`${API_BASE}/clientes/mi-perfil`, {
                    method: 'PUT',
                    headers: getAuthHeaders(),
                    body: JSON.stringify({ direccion, telefono })
                });
            } catch (e) {}
        }

        const res = await fetch(`${API_BASE}/ventas/mi-compra`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok) {
            // Cerrar modal de checkout
            const modalEl = document.getElementById('modalCheckout');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            // Limpiar carrito
            carritoCliente = [];
            guardarCarrito();

            // Actualizar stock local
            await cargarProductosCliente();

            // Mostrar boleta digital de confirmación
            mostrarBoletaConfirmacion(data);
            mostrarToastCliente('¡Compra Exitosa!', `Tu pedido #${data.id} ha sido registrado correctamente.`, 'success');
        } else {
            mostrarToastCliente('Error en la Compra', data.mensaje || 'No se pudo procesar tu pedido.', 'danger');
        }
    } catch (err) {
        mostrarToastCliente('Error de Conexión', 'No se pudo conectar con el servidor para procesar la compra.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = '<i class="bi bi-bag-check-fill me-2"></i> Confirmar y Realizar Pedido';
    }
}

function mostrarBoletaConfirmacion(venta) {
    const contenedor = document.getElementById('boletaConfirmacionBody');
    if (!contenedor) return;

    const fechaFormateada = venta.fecha ? venta.fecha.replace('T', ' ').substring(0, 19) : new Date().toLocaleString();
    let detallesHtml = '';

    if (venta.detalles) {
        venta.detalles.forEach(d => {
            detallesHtml += `
                <tr>
                    <td>${d.productoNombre}</td>
                    <td class="text-center">${d.cantidad}</td>
                    <td class="text-end">S/ ${parseFloat(d.precioUnitario).toFixed(2)}</td>
                    <td class="text-end fw-bold">S/ ${parseFloat(d.subtotal).toFixed(2)}</td>
                </tr>
            `;
        });
    }

    const total = parseFloat(venta.total);
    const subtotal = total / 1.18;
    const igv = total - subtotal;

    contenedor.innerHTML = `
        <div class="boleta-ticket-card">
            <div class="boleta-ticket-header">
                <h5 class="fw-bold mb-1" style="color: var(--pool-rojo);">MINIMARKET POOL</h5>
                <small class="text-muted d-block">RUC: 20608912341 — Lima, Perú</small>
                <small class="text-muted d-block">"Tu ahorro es nuestra prioridad"</small>
                <div class="badge bg-danger mt-2 py-2 px-3 fs-6">BOLETA ELECTRÓNICA #${venta.id}</div>
            </div>

            <div class="small mb-3">
                <div><strong>Cliente:</strong> ${venta.clienteNombre || (usuarioActual ? usuarioActual.nombre : 'Cliente')}</div>
                <div><strong>Documento:</strong> ${venta.clienteDocumento || '-'}</div>
                <div><strong>Fecha:</strong> ${fechaFormateada}</div>
                <div><strong>Método de Pago:</strong> <span class="badge bg-warning text-dark">${venta.metodoPago || 'EFECTIVO'}</span></div>
            </div>

            <table class="table table-sm table-borderless small mb-3">
                <thead class="border-bottom">
                    <tr>
                        <th>Producto</th>
                        <th class="text-center">Cant.</th>
                        <th class="text-end">P.Unit</th>
                        <th class="text-end">Total</th>
                    </tr>
                </thead>
                <tbody>
                    ${detallesHtml}
                </tbody>
            </table>

            <div class="border-top pt-2 small">
                <div class="d-flex justify-content-between">
                    <span>Op. Gravada:</span>
                    <span>S/ ${subtotal.toFixed(2)}</span>
                </div>
                <div class="d-flex justify-content-between">
                    <span>I.G.V. (18%):</span>
                    <span>S/ ${igv.toFixed(2)}</span>
                </div>
                <div class="d-flex justify-content-between fs-5 fw-bold text-danger border-top mt-1 pt-1">
                    <span>IMPORTE TOTAL:</span>
                    <span>S/ ${total.toFixed(2)}</span>
                </div>
            </div>

            <div class="boleta-ticket-footer">
                <p class="small text-muted mb-1">¡Gracias por comprar en Minimarket Pool!</p>
                <small class="text-muted">Tu pedido está siendo preparado para entrega inmediata.</small>
            </div>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('modalBoletaConfirmacion'));
    modal.show();
}

// ==============================================================================
// 4. HISTORIAL DE COMPRAS (MIS COMPRAS)
// ==============================================================================

async function abrirModalMisCompras() {
    if (!tokenJWT || !usuarioActual) {
        abrirModalAuthCliente('login');
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('modalMisCompras'));
    modal.show();

    const tbody = document.getElementById('tablaMisComprasBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-danger me-2"></div> Cargando tus compras...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/ventas/mis-compras`, { headers: getAuthHeaders() });
        if (res.ok) {
            const compras = await res.json();
            if (compras.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Aún no has realizado pedidos en Minimarket Pool.</td></tr>';
                return;
            }

            tbody.innerHTML = '';
            compras.slice().reverse().forEach(c => {
                const fecha = c.fecha ? c.fecha.replace('T', ' ').substring(0, 16) : '-';
                const totalItems = c.detalles ? c.detalles.reduce((acc, d) => acc + d.cantidad, 0) : 0;
                const resumenProds = c.detalles ? c.detalles.map(d => `${d.cantidad}x ${d.productoNombre}`).join(', ') : 'Sin detalle';

                tbody.innerHTML += `
                    <tr>
                        <td><span class="badge bg-dark">#${c.id}</span></td>
                        <td><small>${fecha}</small></td>
                        <td>
                            <small class="text-truncate d-inline-block" style="max-width: 200px;" title="${resumenProds}">
                                ${resumenProds}
                            </small><br>
                            <span class="badge bg-light text-dark border">${totalItems} u.</span>
                        </td>
                        <td><span class="badge bg-warning text-dark">${c.metodoPago || 'EFECTIVO'}</span></td>
                        <td class="text-end fw-bold text-danger">S/ ${parseFloat(c.total).toFixed(2)}</td>
                        <td class="text-center">
                            <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="verBoletaHistorial(${JSON.stringify(c).replace(/"/g, '&quot;')})">
                                <i class="bi bi-receipt"></i> Boleta
                            </button>
                        </td>
                    </tr>
                `;
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">No se pudieron cargar tus pedidos.</td></tr>';
        }
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">Error de conexión al cargar pedidos.</td></tr>';
    }
}

function verBoletaHistorial(venta) {
    mostrarBoletaConfirmacion(venta);
}

// ==============================================================================
// 5. MI PERFIL DE CLIENTE
// ==============================================================================

async function abrirModalMiPerfil() {
    if (!tokenJWT || !usuarioActual) {
        abrirModalAuthCliente('login');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/clientes/mi-perfil`, { headers: getAuthHeaders() });
        if (res.ok) {
            const perfil = await res.json();
            document.getElementById('perfilNombres').value = perfil.nombres || '';
            document.getElementById('perfilApellidos').value = perfil.apellidos || '';
            document.getElementById('perfilTipoDoc').value = perfil.tipoDocumento || 'DNI';
            document.getElementById('perfilNumDoc').value = perfil.numeroDocumento || '';
            document.getElementById('perfilEmail').value = perfil.email || usuarioActual.email || '';
            document.getElementById('perfilTelefono').value = perfil.telefono || '';
            document.getElementById('perfilDireccion').value = perfil.direccion || '';

            const modal = new bootstrap.Modal(document.getElementById('modalMiPerfil'));
            modal.show();
        } else {
            mostrarToastCliente('Perfil', 'No se pudo obtener la información de tu perfil.', 'warning');
        }
    } catch (e) {
        mostrarToastCliente('Error', 'Fallo al conectar con el servidor.', 'danger');
    }
}

async function guardarMiPerfil(e) {
    e.preventDefault();

    const telefono = document.getElementById('perfilTelefono').value.trim();
    const direccion = document.getElementById('perfilDireccion').value.trim();
    const nombres = document.getElementById('perfilNombres').value.trim();
    const apellidos = document.getElementById('perfilApellidos').value.trim();

    try {
        const res = await fetch(`${API_BASE}/clientes/mi-perfil`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify({ nombres, apellidos, telefono, direccion })
        });

        if (res.ok) {
            mostrarToastCliente('Perfil Guardado', 'Tus datos de entrega fueron actualizados.', 'success');
            const modalEl = document.getElementById('modalMiPerfil');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        } else {
            mostrarToastCliente('Error', 'No se pudo actualizar tu perfil.', 'danger');
        }
    } catch (e) {
        mostrarToastCliente('Error de Red', 'Error al comunicar con el servidor.', 'danger');
    }
}

// ==============================================================================
// 6. AUTENTICACIÓN Y CUENTA CLIENTE
// ==============================================================================

function abrirModalAuthCliente(tabInicial = 'login') {
    const tabLogin = document.getElementById('tabAuthClienteLogin');
    const tabRegistro = document.getElementById('tabAuthClienteRegistro');

    if (tabInicial === 'registro') {
        if (tabRegistro) tabRegistro.click();
    } else {
        if (tabLogin) tabLogin.click();
    }

    const modal = new bootstrap.Modal(document.getElementById('modalAuthCliente'));
    modal.show();
}

async function ejecutarLoginCliente(e) {
    e.preventDefault();
    const email = document.getElementById('authClienteEmail').value.trim();
    const password = document.getElementById('authClientePassword').value.trim();
    const btnSubmit = document.getElementById('btnSubmitLoginCliente');

    try {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Entrando...';

        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();

        if (res.ok) {
            tokenJWT = data.token;
            usuarioActual = {
                id: data.id,
                nombre: data.nombre,
                email: data.email,
                rol: data.rol
            };

            localStorage.setItem('jwt_token', tokenJWT);
            localStorage.setItem('jwt_user', JSON.stringify(usuarioActual));

            mostrarToastCliente('¡Bienvenido!', `Hola, ${usuarioActual.nombre}`, 'success');

            const modalEl = document.getElementById('modalAuthCliente');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            actualizarUINavbarUsuario();

            // Si tenía productos en el carrito y estaba por pagar, abrir checkout
            if (carritoCliente.length > 0) {
                setTimeout(() => abrirCheckoutModal(), 600);
            }
        } else {
            mostrarToastCliente('Error de Acceso', data.mensaje || 'Correo o contraseña incorrectos.', 'danger');
        }
    } catch (err) {
        mostrarToastCliente('Error de Red', 'No se pudo conectar con el servidor.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = 'Ingresar a mi Cuenta';
    }
}

async function ejecutarRegistroCliente(e) {
    e.preventDefault();

    const nombres = document.getElementById('regClienteNombres').value.trim();
    const apellidos = document.getElementById('regClienteApellidos').value.trim();
    const tipoDocumento = document.getElementById('regClienteTipoDoc').value;
    const numeroDocumento = document.getElementById('regClienteNumDoc').value.trim();
    const email = document.getElementById('regClienteEmail').value.trim();
    const password = document.getElementById('regClientePassword').value.trim();
    const telefono = document.getElementById('regClienteTelefono').value.trim();
    const direccion = document.getElementById('regClienteDireccion').value.trim();
    const btnSubmit = document.getElementById('btnSubmitRegCliente');

    if (tipoDocumento === 'DNI' && numeroDocumento.length !== 8) {
        mostrarToastCliente('Validación', 'El DNI debe tener exactamente 8 dígitos.', 'warning');
        return;
    }

    if (tipoDocumento === 'RUC' && numeroDocumento.length !== 11) {
        mostrarToastCliente('Validación', 'El RUC debe tener exactamente 11 dígitos.', 'warning');
        return;
    }

    const payload = {
        nombres,
        apellidos,
        tipoDocumento,
        numeroDocumento,
        email,
        password,
        telefono,
        direccion
    };

    try {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Registrando...';

        const res = await fetch(`${API_BASE}/auth/registro-cliente`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok) {
            tokenJWT = data.token;
            usuarioActual = {
                id: data.id,
                nombre: data.nombre,
                email: data.email,
                rol: data.rol || 'CLIENTE'
            };

            localStorage.setItem('jwt_token', tokenJWT);
            localStorage.setItem('jwt_user', JSON.stringify(usuarioActual));

            mostrarToastCliente('¡Cuenta Creada!', `Bienvenido a Minimarket Pool, ${usuarioActual.nombre}`, 'success');

            const modalEl = document.getElementById('modalAuthCliente');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            actualizarUINavbarUsuario();

            if (carritoCliente.length > 0) {
                setTimeout(() => abrirCheckoutModal(), 600);
            }
        } else {
            mostrarToastCliente('Error en Registro', data.mensaje || 'No se pudo crear la cuenta.', 'danger');
        }
    } catch (err) {
        mostrarToastCliente('Error de Red', 'Fallo al comunicar con el servidor.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = 'Crear Cuenta y Continuar';
    }
}

function cerrarSesionCliente() {
    tokenJWT = null;
    usuarioActual = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_user');
    actualizarUINavbarUsuario();
    mostrarToastCliente('Sesión Cerrada', 'Has cerrado tu cuenta de cliente.', 'info');
}

function actualizarUINavbarUsuario() {
    const contenedor = document.getElementById('userNavContainer');
    if (!contenedor) return;

    if (tokenJWT && usuarioActual) {
        const esCliente = usuarioActual.rol === 'CLIENTE';
        contenedor.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-sm btn-outline-light dropdown-toggle d-flex align-items-center gap-2 fw-semibold rounded-pill px-3 py-1" type="button" data-bs-toggle="dropdown">
                    <i class="bi bi-person-circle fs-5 text-warning"></i>
                    <span>${usuarioActual.nombre || 'Mi Cuenta'}</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow border-0 rounded-3 mt-2">
                    <li class="px-3 py-2 border-bottom">
                        <small class="text-muted d-block">Sesión activa como:</small>
                        <strong class="text-dark">${usuarioActual.email}</strong><br>
                        <span class="badge ${esCliente ? 'bg-success' : 'bg-primary'}">${usuarioActual.rol}</span>
                    </li>
                    ${esCliente ? `
                    <li><button class="dropdown-item py-2" onclick="abrirModalMiPerfil()"><i class="bi bi-person-lines-fill me-2 text-danger"></i> Mi Perfil y Dirección</button></li>
                    <li><button class="dropdown-item py-2" onclick="abrirModalMisCompras()"><i class="bi bi-bag-check-fill me-2 text-danger"></i> Mis Pedidos / Compras</button></li>
                    ` : `
                    <li><a class="dropdown-item py-2" href="index.html"><i class="bi bi-speedometer2 me-2 text-primary"></i> Ir al Panel de Personal</a></li>
                    `}
                    <li><hr class="dropdown-divider my-1"></li>
                    <li><button class="dropdown-item py-2 text-danger" onclick="cerrarSesionCliente()"><i class="bi bi-box-arrow-right me-2"></i> Cerrar Sesión</button></li>
                </ul>
            </div>
        `;
    } else {
        contenedor.innerHTML = `
            <div class="d-flex align-items-center gap-2">
                <button class="btn btn-sm btn-outline-light rounded-pill px-3 fw-semibold" onclick="abrirModalAuthCliente('login')">
                    <i class="bi bi-box-arrow-in-right me-1"></i> Ingresar
                </button>
                <button class="btn btn-sm btn-warning text-dark rounded-pill px-3 fw-bold shadow-sm" onclick="abrirModalAuthCliente('registro')">
                    Crear Cuenta
                </button>
            </div>
        `;
    }
}
