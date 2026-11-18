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
let productoPendienteId = null;

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

function agregarAlCarritoCliente(productoId, omitirAvisoAuth = false) {
    const prod = productosClienteGlobal.find(p => p.id === productoId);
    if (!prod) return;

    if (prod.stockActual <= 0) {
        mostrarToastCliente('Sin Stock', 'Este producto está agotado temporalmente.', 'warning');
        return;
    }

    // Si el usuario no tiene cuenta activa de cliente, mostrar el modal de requerimiento
    if (!omitirAvisoAuth && (!tokenJWT || !usuarioActual || usuarioActual.rol !== 'CLIENTE')) {
        productoPendienteId = productoId;
        const modalReqEl = document.getElementById('modalRequiereCuenta');
        if (modalReqEl) {
            const modalReq = new bootstrap.Modal(modalReqEl);
            modalReq.show();
        } else {
            abrirModalAuthCliente('registro');
        }
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

/**
 * Transición limpia desde el aviso de cuenta requerida al modal de autenticación.
 */
function procederConAuthDesdeAviso(tab = 'registro') {
    const modalReqEl = document.getElementById('modalRequiereCuenta');
    if (modalReqEl) {
        const modalReqInstance = bootstrap.Modal.getInstance(modalReqEl);
        if (modalReqInstance) modalReqInstance.hide();
    }
    setTimeout(() => {
        abrirModalAuthCliente(tab);
    }, 350);
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
        const modalReqEl = document.getElementById('modalRequiereCuenta');
        if (modalReqEl) {
            const modalReq = new bootstrap.Modal(modalReqEl);
            modalReq.show();
        } else {
            abrirModalAuthCliente('login');
        }
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

/**
 * Convierte montos numéricos a texto formal en Soles peruanos (Estándar SUNAT).
 */
function numeroALetrasSoles(monto) {
    if (!monto || isNaN(monto)) return 'CERO Y 00/100 SOLES';
    const unidades = ['', 'UN', 'DOS', 'TRES', 'CUATRO', 'CINCO', 'SEIS', 'SIETE', 'OCHO', 'NUEVE'];
    const especiales = {
        10: 'DIEZ', 11: 'ONCE', 12: 'DOCE', 13: 'TRECE', 14: 'CATORCE', 15: 'QUINCE',
        16: 'DIECISÉIS', 17: 'DIECISIETE', 18: 'DIECIOCHO', 19: 'DIECINUEVE',
        20: 'VEINTE', 21: 'VEINTIUNO', 22: 'VEINTIDÓS', 23: 'VEINTITRÉS', 24: 'VEINTICUATRO',
        25: 'VEINTICINCO', 26: 'VEINTISÉIS', 27: 'VEINTISIETE', 28: 'VEINTIOCHO', 29: 'VEINTINUEVE'
    };
    const decenas = ['', '', '', 'TREINTA', 'CUARENTA', 'CINCUENTA', 'SESENTA', 'SETENTA', 'OCHENTA', 'NOVENTA'];
    const centenas = ['', 'CIENTO', 'DOSCIENTOS', 'TRESCIENTOS', 'CUATROCIENTOS', 'QUINIENTOS', 'SEISCIENTOS', 'SETECIENTOS', 'OCHOCIENTOS', 'NOVECIENTOS'];

    const num = Math.floor(monto);
    const centavos = Math.round((monto - num) * 100);
    const strCentavos = centavos.toString().padStart(2, '0');

    if (num === 0) return `CERO Y ${strCentavos}/100 SOLES`;
    if (num === 100) return `CIEN Y ${strCentavos}/100 SOLES`;

    function convertirCentena(n) {
        if (n === 0) return '';
        if (n === 100) return 'CIEN ';
        let out = '';
        const c = Math.floor(n / 100);
        const restC = n % 100;

        if (c > 0) out += centenas[c] + ' ';
        if (restC === 0) return out;

        if (especiales[restC]) {
            out += especiales[restC] + ' ';
        } else {
            const d = Math.floor(restC / 10);
            const u = restC % 10;
            if (d > 0) out += decenas[d] + (u > 0 ? ' Y ' : ' ');
            if (u > 0) out += unidades[u] + ' ';
        }
        return out;
    }

    let texto = '';
    if (num >= 1000) {
        const miles = Math.floor(num / 1000);
        const resto = num % 1000;
        texto += (miles === 1 ? 'MIL ' : convertirCentena(miles) + 'MIL ');
        if (resto > 0) texto += convertirCentena(resto);
    } else {
        texto += convertirCentena(num);
    }

    return `${texto.trim()} Y ${strCentavos}/100 SOLES`;
}

/**
 * Renderiza la Boleta de Venta Electrónica con diseño oficial y formal.
 */
let viendoBoletaDesdeHistorial = false;

function mostrarBoletaConfirmacion(venta, esDesdeHistorial = false) {
    viendoBoletaDesdeHistorial = esDesdeHistorial;
    const contenedor = document.getElementById('boletaConfirmacionBody');
    if (!contenedor) return;

    // Actualizar encabezado según procedencia
    const tituloEl = document.getElementById('modalBoletaTitulo');
    if (tituloEl) {
        if (esDesdeHistorial) {
            tituloEl.innerHTML = `<i class="bi bi-receipt text-warning me-2"></i> Boleta Electrónica #${venta.id}`;
        } else {
            tituloEl.innerHTML = `<i class="bi bi-check-circle-fill text-warning me-2"></i> ¡Pedido Confirmado Exitosamente!`;
        }
    }

    // Actualizar botones de navegación en el pie del modal
    const footerBotones = document.getElementById('modalBoletaBotonesFooter');
    if (footerBotones) {
        if (esDesdeHistorial) {
            footerBotones.innerHTML = `
                <button type="button" class="btn btn-secondary px-3" onclick="cerrarBoletaConfirmacion()">
                    <i class="bi bi-arrow-left me-1"></i> Volver a Mis Pedidos
                </button>
                <button type="button" class="btn btn-danger fw-bold px-3 shadow-sm" onclick="imprimirBoletaFormal()" style="background: var(--pool-rojo); border-color: var(--pool-rojo);">
                    <i class="bi bi-printer-fill me-1"></i> Imprimir Boleta
                </button>
            `;
        } else {
            footerBotones.innerHTML = `
                <button type="button" class="btn btn-secondary px-3" onclick="cerrarBoletaConfirmacion()">Cerrar</button>
                <button type="button" class="btn btn-danger fw-bold px-3 shadow-sm" onclick="imprimirBoletaFormal()" style="background: var(--pool-rojo); border-color: var(--pool-rojo);">
                    <i class="bi bi-printer-fill me-1"></i> Imprimir Boleta
                </button>
            `;
        }
    }

    const fechaFormateada = venta.fecha ? venta.fecha.replace('T', ' ').substring(0, 19) : new Date().toLocaleString('es-PE');
    let filasDetalles = '';

    if (venta.detalles && venta.detalles.length > 0) {
        venta.detalles.forEach((d, idx) => {
            const cant = d.cantidad || 1;
            const pUnit = parseFloat(d.precioUnitario || 0);
            const sub = parseFloat(d.subtotal || (cant * pUnit));
            filasDetalles += `
                <tr>
                    <td class="text-center text-muted small py-2">${idx + 1}</td>
                    <td class="text-center fw-bold py-2">${cant}</td>
                    <td class="py-2">
                        <div class="fw-semibold text-dark">${d.productoNombre}</div>
                        ${d.sku ? `<small class="text-muted">SKU: ${d.sku}</small>` : ''}
                    </td>
                    <td class="text-end py-2">S/ ${pUnit.toFixed(2)}</td>
                    <td class="text-end fw-bold text-dark py-2">S/ ${sub.toFixed(2)}</td>
                </tr>
            `;
        });
    }

    const total = parseFloat(venta.total || 0);
    const subtotal = total / 1.18;
    const igv = total - subtotal;
    const serieNumero = `B001 - ${String(venta.id || 1).padStart(8, '0')}`;
    const textoEnLetras = numeroALetrasSoles(total);
    const clienteNombre = venta.clienteNombre || (usuarioActual ? usuarioActual.nombre : 'CLIENTE GENERAL');
    const clienteDoc = venta.clienteDocumento || '71837969';
    const qrData = encodeURIComponent(`20608912341|03|B001|${venta.id}|${igv.toFixed(2)}|${total.toFixed(2)}|${fechaFormateada}|1|${clienteDoc}|`);

    contenedor.innerHTML = `
        <div class="boleta-formal-document" id="boletaImprimibleFormal">
            <!-- CABECERA CORPORATIVA Y CUADRO RUC SUNAT -->
            <div class="row align-items-center mb-3 pb-3 border-bottom border-2">
                <div class="col-sm-7">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <div class="brand-logo-boleta">
                            <i class="bi bi-cart4 text-white fs-4"></i>
                        </div>
                        <div>
                            <h4 class="fw-bold mb-0 text-dark" style="letter-spacing: -0.02em;">MINIMARKET POOL S.A.C.</h4>
                            <span class="text-danger fw-semibold small" style="font-size: 0.8rem;">"Tu ahorro es nuestra prioridad"</span>
                        </div>
                    </div>
                    <div class="small text-secondary lh-sm">
                        <div><i class="bi bi-geo-alt-fill text-danger me-1"></i> Av. Los Emprendedores 456, San Juan de Lurigancho - Lima</div>
                        <div><i class="bi bi-telephone-fill text-danger me-1"></i> Central de Pedidos: (01) 456-7890 | WhatsApp: 987 654 321</div>
                        <div><i class="bi bi-envelope-fill text-danger me-1"></i> contacto@minimarketpool.com.pe</div>
                    </div>
                </div>
                <div class="col-sm-5 mt-3 mt-sm-0">
                    <div class="boleta-sunat-box text-center p-3 shadow-sm">
                        <div class="fw-bold text-dark fs-6">R.U.C. 20608912341</div>
                        <div class="bg-danger text-white fw-bold py-1 my-1 rounded text-uppercase" style="font-size: 0.82rem; letter-spacing: 0.05em;">
                            BOLETA DE VENTA ELECTRÓNICA
                        </div>
                        <div class="fs-5 fw-bold text-dark font-monospace">${serieNumero}</div>
                    </div>
                </div>
            </div>

            <!-- DATOS DEL CLIENTE Y FECHA DE EMISIÓN -->
            <div class="bg-light p-3 rounded-3 mb-3 border">
                <div class="row g-2 small">
                    <div class="col-sm-7">
                        <div class="mb-1"><span class="text-muted fw-bold">SEÑOR(ES):</span> <span class="fw-bold text-dark text-uppercase">${clienteNombre}</span></div>
                        <div class="mb-1"><span class="text-muted fw-bold">TIPO / N° DOC:</span> <span class="fw-semibold text-dark">DNI ${clienteDoc}</span></div>
                        <div><span class="text-muted fw-bold">CONDICIÓN:</span> <span class="text-dark">Entrega a Domicilio Inmediata</span></div>
                    </div>
                    <div class="col-sm-5 text-sm-end">
                        <div class="mb-1"><span class="text-muted fw-bold">FECHA EMISIÓN:</span> <span class="fw-bold text-dark">${fechaFormateada}</span></div>
                        <div class="mb-1"><span class="text-muted fw-bold">MEDIO DE PAGO:</span> <span class="badge bg-danger-subtle text-danger fw-bold border border-danger-subtle">${venta.metodoPago || 'EFECTIVO'}</span></div>
                        <div><span class="text-muted fw-bold">MONEDA:</span> <span class="fw-bold text-dark">SOLES (PEN)</span></div>
                    </div>
                </div>
            </div>

            <!-- DETALLE DE ARTÍCULOS VENDIDOS -->
            <div class="table-responsive mb-3">
                <table class="table table-bordered table-sm align-middle mb-0 boleta-tabla-items">
                    <thead class="table-light">
                        <tr class="text-muted small text-uppercase">
                            <th class="text-center py-2" style="width: 40px;">#</th>
                            <th class="text-center py-2" style="width: 65px;">Cant.</th>
                            <th class="py-2">Descripción del Producto</th>
                            <th class="text-end py-2" style="width: 105px;">P. Unit.</th>
                            <th class="text-end py-2" style="width: 105px;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${filasDetalles}
                    </tbody>
                </table>
            </div>

            <!-- RESUMEN ECONÓMICO Y LIQUIDACIÓN SUNAT -->
            <div class="row g-3 align-items-center mb-3">
                <div class="col-md-7">
                    <div class="p-3 bg-light rounded-3 border small">
                        <div class="mb-1 text-muted fw-bold">IMPORTE EN LETRAS:</div>
                        <div class="text-dark fw-bold text-uppercase mb-2 font-monospace" style="font-size: 0.8rem;">
                            SON: ${textoEnLetras}
                        </div>
                        <div class="d-flex align-items-center gap-3 pt-2 border-top">
                            <div class="bg-white p-1 border rounded shadow-sm text-center" style="width: 62px; height: 62px;">
                                <img src="https://api.qrserver.com/v1/create-qr-code/?size=54x54&data=${qrData}" alt="QR Fiscal" width="54" height="54" style="display:block; margin: 0 auto;">
                            </div>
                            <div style="font-size: 0.72rem;" class="text-muted lh-sm">
                                <div><strong>Resumen Hash:</strong> <code>7b9a5f4c21...8e31</code></div>
                                <div>Representación impresa de la <strong>BOLETA DE VENTA ELECTRÓNICA</strong>.</div>
                                <div>Consulte la validez en: <span class="text-decoration-underline">www.minimarketpool.com.pe</span></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-5">
                    <div class="border rounded-3 p-3 bg-light">
                        <div class="d-flex justify-content-between small text-muted mb-1">
                            <span>OP. GRAVADA:</span>
                            <span class="text-dark fw-semibold">S/ ${subtotal.toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between small text-muted mb-1">
                            <span>OP. EXONERADA:</span>
                            <span class="text-dark fw-semibold">S/ 0.00</span>
                        </div>
                        <div class="d-flex justify-content-between small text-muted mb-2">
                            <span>I.G.V. (18%):</span>
                            <span class="text-dark fw-semibold">S/ ${igv.toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between align-items-center border-top border-2 pt-2">
                            <span class="fw-bold fs-6 text-dark">IMPORTE TOTAL:</span>
                            <span class="fs-4 fw-bold text-danger">S/ ${total.toFixed(2)}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- PIE DE COMPROBANTE -->
            <div class="text-center pt-2 border-top text-muted small" style="font-size: 0.73rem;">
                ¡Muchas gracias por su preferencia! Su pedido ha sido registrado y se encuentra en camino.
            </div>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('modalBoletaConfirmacion'));
    modal.show();
}

/**
 * Impresión formal e hiper-limpia de la Boleta Oficial (Exactamente 1 hoja de papel, sin elementos web).
 */
function imprimirBoletaFormal() {
    const boletaEl = document.getElementById('boletaImprimibleFormal');
    if (!boletaEl) {
        window.print();
        return;
    }

    const ventana = window.open('', '_blank', 'width=840,height=900');
    ventana.document.write(`
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <title>Boleta de Venta Electrónica - Minimarket Pool</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
            <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;600;700;800&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
            <style>
                body {
                    background: #ffffff !important;
                    font-family: 'Inter', system-ui, sans-serif;
                    color: #0f172a;
                    margin: 0;
                    padding: 24px;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                }
                .brand-logo-boleta {
                    width: 44px;
                    height: 44px;
                    background: #dc2626 !important;
                    border-radius: 10px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .boleta-sunat-box {
                    border: 2px solid #0f172a !important;
                    border-radius: 8px;
                    background: #ffffff !important;
                }
                .table-bordered th, .table-bordered td {
                    border: 1px solid #cbd5e1 !important;
                }
                .table th {
                    background-color: #f1f5f9 !important;
                    color: #334155 !important;
                    font-weight: 700;
                }
                @page {
                    size: A4 portrait;
                    margin: 12mm 15mm;
                }
            </style>
        </head>
        <body>
            ${boletaEl.outerHTML}
            <script>
                window.onload = function() {
                    window.focus();
                    window.print();
                    setTimeout(function() { window.close(); }, 800);
                };
            <\/script>
        </body>
        </html>
    `);
    ventana.document.close();
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

/**
 * Abre la boleta desde el historial ocultando primero el modal de compras para evitar sobreposiciones.
 */
function verBoletaHistorial(venta) {
    const modalComprasEl = document.getElementById('modalMisCompras');
    if (modalComprasEl) {
        const modalComprasInst = bootstrap.Modal.getInstance(modalComprasEl);
        if (modalComprasInst) modalComprasInst.hide();
    }
    setTimeout(() => {
        mostrarBoletaConfirmacion(venta, true);
    }, 350);
}

/**
 * Cierra la boleta y, si provenía del historial de compras, reabre limpiamente la lista de pedidos.
 */
function cerrarBoletaConfirmacion() {
    const modalBoletaEl = document.getElementById('modalBoletaConfirmacion');
    if (modalBoletaEl) {
        const modalBoletaInst = bootstrap.Modal.getInstance(modalBoletaEl);
        if (modalBoletaInst) modalBoletaInst.hide();
    }
    if (viendoBoletaDesdeHistorial) {
        viendoBoletaDesdeHistorial = false;
        setTimeout(() => {
            abrirModalMisCompras();
        }, 350);
    }
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
                nombre: data.nombre || 'Cliente',
                email: data.email || email,
                rol: data.rol || 'CLIENTE'
            };

            localStorage.setItem('jwt_token', tokenJWT);
            localStorage.setItem('jwt_user', JSON.stringify(usuarioActual));

            mostrarToastCliente('¡Bienvenido!', `Hola, ${usuarioActual.nombre}`, 'success');

            const modalEl = document.getElementById('modalAuthCliente');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            actualizarUINavbarUsuario();

            // Si había un producto que el usuario intentó agregar sin iniciar sesión, agregarlo ahora
            if (productoPendienteId) {
                const prodId = productoPendienteId;
                productoPendienteId = null;
                setTimeout(() => {
                    agregarAlCarritoCliente(prodId, true);
                    const offcanvasEl = document.getElementById('offcanvasCarrito');
                    if (offcanvasEl) {
                        const offcanvas = new bootstrap.Offcanvas(offcanvasEl);
                        offcanvas.show();
                    }
                }, 400);
            } else if (carritoCliente.length > 0) {
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

// ==============================================================================
// VERIFICACIÓN INTELIGENTE DE CLIENTE / DOCUMENTO (TIENDA vs WEB)
// ==============================================================================
let timerVerificacionDoc = null;
let ultimoDocVerificado = '';
let docTieneCuentaWebActiva = false;

function alCambiarTipoDocRegistro() {
    const tipo = document.getElementById('regClienteTipoDoc')?.value || 'DNI';
    const inputDoc = document.getElementById('regClienteNumDoc');
    const labelNom = document.getElementById('labelRegClienteNombres');
    const colApe = document.getElementById('colRegClienteApellidos');
    const avisoBox = document.getElementById('regClienteAvisoDni');

    if (!inputDoc) return;

    if (tipo === 'DNI') {
        inputDoc.maxLength = 8;
        inputDoc.placeholder = '8 dígitos';
        if (labelNom) labelNom.innerText = 'Nombres *';
        if (colApe) colApe.style.display = 'block';
    } else if (tipo === 'RUC') {
        inputDoc.maxLength = 11;
        inputDoc.placeholder = '11 dígitos';
        if (labelNom) labelNom.innerText = 'Razón Social *';
        if (colApe) colApe.style.display = 'none';
    } else {
        inputDoc.maxLength = 12;
        inputDoc.placeholder = 'N° Carné Extranjería';
        if (labelNom) labelNom.innerText = 'Nombres *';
        if (colApe) colApe.style.display = 'block';
    }

    if (avisoBox) avisoBox.style.display = 'none';
    ultimoDocVerificado = '';
    docTieneCuentaWebActiva = false;
    desbloquearNombresRegistro();
}

function alEscribirDocRegistro() {
    const inputDoc = document.getElementById('regClienteNumDoc');
    if (!inputDoc) return;

    // Solo números para DNI / RUC
    const tipo = document.getElementById('regClienteTipoDoc')?.value || 'DNI';
    if (tipo === 'DNI' || tipo === 'RUC') {
        inputDoc.value = inputDoc.value.replace(/\D/g, '');
    }

    const doc = inputDoc.value.trim();
    if (timerVerificacionDoc) clearTimeout(timerVerificacionDoc);

    const lenRequerida = tipo === 'DNI' ? 8 : (tipo === 'RUC' ? 11 : 8);
    if (doc.length === lenRequerida) {
        timerVerificacionDoc = setTimeout(() => {
            verificarDocumentoRegistro(false);
        }, 300);
    } else {
        const avisoBox = document.getElementById('regClienteAvisoDni');
        if (avisoBox && doc.length < lenRequerida) {
            avisoBox.style.display = 'none';
        }
        docTieneCuentaWebActiva = false;
        const btnSubmit = document.getElementById('btnSubmitRegCliente');
        if (btnSubmit) {
            btnSubmit.innerHTML = '<i class="bi bi-person-plus-fill me-1"></i> Crear Cuenta y Continuar';
            btnSubmit.disabled = false;
        }
    }
}

async function verificarDocumentoRegistro(forzar = false) {
    const inputDoc = document.getElementById('regClienteNumDoc');
    const tipoDoc = document.getElementById('regClienteTipoDoc')?.value || 'DNI';
    const avisoBox = document.getElementById('regClienteAvisoDni');
    const icono = document.getElementById('iconoBuscarDocReg');
    const btnSubmit = document.getElementById('btnSubmitRegCliente');

    if (!inputDoc) return;
    const doc = inputDoc.value.trim();

    const minLen = tipoDoc === 'DNI' ? 8 : (tipoDoc === 'RUC' ? 11 : 6);
    if (doc.length < minLen) {
        if (forzar) {
            mostrarToastCliente('Validación', `El ${tipoDoc} debe tener al menos ${minLen} dígitos.`, 'warning');
        }
        return;
    }

    if (!forzar && doc === ultimoDocVerificado) return;
    ultimoDocVerificado = doc;

    try {
        if (icono) icono.className = 'spinner-border spinner-border-sm text-secondary';

        const res = await fetch(`${API_BASE}/auth/verificar-documento/${doc}`);
        if (!res.ok) return;

        const data = await res.json();
        if (!avisoBox) return;

        if (data.registradoEnTienda && !data.tieneCuentaWeb) {
            // ESCENARIO 1: Registrado en tienda por el vendedor / cajero
            docTieneCuentaWebActiva = false;

            const inputNom = document.getElementById('regClienteNombres');
            const inputApe = document.getElementById('regClienteApellidos');
            const inputTel = document.getElementById('regClienteTelefono');
            const inputDir = document.getElementById('regClienteDireccion');
            const btnEditar = document.getElementById('btnEditarNombresReg');

            if (inputNom && data.nombres) {
                inputNom.value = data.nombres;
                inputNom.readOnly = true;
                inputNom.classList.add('bg-light');
            }
            if (inputApe && data.apellidos) {
                inputApe.value = data.apellidos;
                inputApe.readOnly = true;
                inputApe.classList.add('bg-light');
            }
            if (btnEditar) btnEditar.style.display = 'inline-block';

            if (inputTel && data.telefono && !inputTel.value) {
                inputTel.value = data.telefono;
            }
            if (inputDir && data.direccion && !inputDir.value) {
                inputDir.value = data.direccion;
            }

            avisoBox.innerHTML = `
                <div class="p-2 rounded-3 bg-primary-subtle border border-primary-subtle text-primary small d-flex align-items-start gap-2 shadow-sm">
                    <i class="bi bi-person-check-fill fs-5 text-primary mt-0"></i>
                    <div>
                        <div class="fw-bold">¡Hola, ${data.nombres}! Ya compraste antes en nuestra tienda física.</div>
                        <div class="text-dark small mt-1">
                            Tus datos fueron reconocidos. Completa tu <span class="badge bg-primary text-white">Correo</span>, <span class="badge bg-primary text-white">Contraseña</span> y <span class="badge bg-primary text-white">Dirección de Entrega</span> para activar tu cuenta online.
                        </div>
                    </div>
                </div>
            `;
            avisoBox.style.display = 'block';

            if (btnSubmit) {
                btnSubmit.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Activar mi Cuenta Online';
                btnSubmit.disabled = false;
            }

            // Foco en correo
            const inputEmail = document.getElementById('regClienteEmail');
            if (inputEmail && !inputEmail.value) inputEmail.focus();

        } else if (data.registradoEnTienda && data.tieneCuentaWeb) {
            // ESCENARIO 2: Ya tiene cuenta web
            docTieneCuentaWebActiva = true;

            avisoBox.innerHTML = `
                <div class="p-2 rounded-3 bg-warning-subtle border border-warning-subtle text-dark small d-flex align-items-start gap-2 shadow-sm">
                    <i class="bi bi-exclamation-triangle-fill fs-5 text-warning mt-0"></i>
                    <div>
                        <div class="fw-bold">Ya cuentas con una cuenta web activa.</div>
                        <div class="small mt-1">
                            Este documento ya está vinculado a un usuario web. Por favor inicia sesión con tu correo electrónico.
                        </div>
                        <button type="button" class="btn btn-sm btn-dark mt-2 fw-semibold px-3" onclick="abrirModalAuthCliente('login')">
                            <i class="bi bi-box-arrow-in-right me-1"></i> Ir a Iniciar Sesión
                        </button>
                    </div>
                </div>
            `;
            avisoBox.style.display = 'block';

            if (btnSubmit) {
                btnSubmit.innerHTML = '<i class="bi bi-lock-fill me-1"></i> Ya Registrado (Inicia Sesión)';
                btnSubmit.disabled = true;
            }

        } else {
            // ESCENARIO 3: Cliente nuevo
            docTieneCuentaWebActiva = false;
            desbloquearNombresRegistro();

            avisoBox.innerHTML = `
                <div class="p-2 rounded-3 bg-light border text-muted small d-flex align-items-center gap-2">
                    <i class="bi bi-sparkles text-warning fs-5"></i>
                    <span>Nuevo cliente: Completa los campos para crear tu cuenta y comprar.</span>
                </div>
            `;
            avisoBox.style.display = 'block';

            if (btnSubmit) {
                btnSubmit.innerHTML = '<i class="bi bi-person-plus-fill me-1"></i> Crear Cuenta y Continuar';
                btnSubmit.disabled = false;
            }
        }
    } catch (e) {
        console.error('Error al verificar documento:', e);
    } finally {
        if (icono) icono.className = 'bi bi-search';
    }
}

function desbloquearNombresRegistro() {
    const inputNom = document.getElementById('regClienteNombres');
    const inputApe = document.getElementById('regClienteApellidos');
    const btnEditar = document.getElementById('btnEditarNombresReg');

    if (inputNom) {
        inputNom.readOnly = false;
        inputNom.classList.remove('bg-light');
    }
    if (inputApe) {
        inputApe.readOnly = false;
        inputApe.classList.remove('bg-light');
    }
    if (btnEditar) btnEditar.style.display = 'none';
}

async function ejecutarRegistroCliente(e) {
    e.preventDefault();

    if (docTieneCuentaWebActiva) {
        mostrarToastCliente('Cuenta Existente', 'Ya cuentas con una cuenta web activa. Inicia sesión con tu correo.', 'warning');
        abrirModalAuthCliente('login');
        return;
    }

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
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Procesando...';

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
                nombre: data.nombre || nombres,
                email: data.email || email,
                rol: data.rol || 'CLIENTE'
            };

            localStorage.setItem('jwt_token', tokenJWT);
            localStorage.setItem('jwt_user', JSON.stringify(usuarioActual));

            mostrarToastCliente('¡Bienvenido!', `Hola ${usuarioActual.nombre}, tu cuenta ha sido activada correctamente.`, 'success');

            const modalEl = document.getElementById('modalAuthCliente');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            actualizarUINavbarUsuario();

            // Si había un producto que el usuario intentó agregar sin registrarse, agregarlo ahora
            if (productoPendienteId) {
                const prodId = productoPendienteId;
                productoPendienteId = null;
                setTimeout(() => {
                    agregarAlCarritoCliente(prodId, true);
                    const offcanvasEl = document.getElementById('offcanvasCarrito');
                    if (offcanvasEl) {
                        const offcanvas = new bootstrap.Offcanvas(offcanvasEl);
                        offcanvas.show();
                    }
                }, 400);
            } else if (carritoCliente.length > 0) {
                setTimeout(() => abrirCheckoutModal(), 600);
            }
        } else {
            mostrarToastCliente('Error en Registro', data.mensaje || 'No se pudo crear o activar la cuenta.', 'danger');
        }
    } catch (err) {
        mostrarToastCliente('Error de Red', 'Fallo al comunicar con el servidor.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = '<i class="bi bi-person-plus-fill me-1"></i> Crear Cuenta y Continuar';
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
                        <strong class="text-dark">${usuarioActual.email || usuarioActual.nombre || 'Cliente'}</strong><br>
                        <span class="badge ${esCliente ? 'bg-success' : 'bg-primary'}">${usuarioActual.rol}</span>
                    </li>
                    ${esCliente ? `
                    <li><button class="dropdown-item py-2" onclick="abrirModalMiPerfil()"><i class="bi bi-person-lines-fill me-2 text-danger"></i> Mi Perfil y Dirección</button></li>
                    <li><button class="dropdown-item py-2" onclick="abrirModalMisCompras()"><i class="bi bi-bag-check-fill me-2 text-danger"></i> Mis Pedidos / Compras</button></li>
                    ` : `
                    <li><a class="dropdown-item py-2" href="../interno/"><i class="bi bi-speedometer2 me-2 text-primary"></i> Ir al Panel de Personal</a></li>
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
