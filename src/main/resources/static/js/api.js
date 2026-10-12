// ==============================================================================
// MODULO: CLIENTE HTTP, TOASTS, NAVEGACIÓN Y UTILIDADES GLOBALES (api.js)
// ==============================================================================

const API_BASE = '/api';
let tokenJWT = localStorage.getItem('jwt_token') || null;
let usuarioActual = JSON.parse(localStorage.getItem('jwt_user') || 'null');

/**
 * Genera los headers estándar para las peticiones al backend Spring Boot.
 */
function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (tokenJWT) {
        headers['Authorization'] = `Bearer ${tokenJWT}`;
    }
    return headers;
}

/**
 * Manejo centralizado de respuestas HTTP para detectar tokens expirados o inválidos.
 */
async function procesarRespuestaHttp(res) {
    if (res.status === 401 || res.status === 403) {
        console.warn('Sesión expirada o token inválido (401/403). Redirigiendo a Login...');
        if (typeof cerrarSesion === 'function') {
            cerrarSesion();
        }
        return null;
    }
    return res;
}

/**
 * Notificaciones flotantes estilizadas (Toast).
 */
function mostrarToast(titulo, mensaje, tipo = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const colorBorder = tipo === 'success' ? '#10b981' : tipo === 'danger' ? '#ef4444' : tipo === 'warning' ? '#f59e0b' : '#0d6efd';
    const icono = tipo === 'success' ? 'bi-check-circle-fill' : tipo === 'danger' ? 'bi-exclamation-octagon-fill' : tipo === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill';

    const toastEl = document.createElement('div');
    toastEl.style.cssText = `
        background: #ffffff;
        color: #1e293b;
        border-left: 4px solid ${colorBorder};
        border-radius: 10px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.12);
        padding: 14px 18px;
        min-width: 280px;
        max-width: 360px;
        display: flex;
        gap: 12px;
        align-items: flex-start;
        border: 1px solid #e2e8f0;
    `;

    toastEl.innerHTML = `
        <i class="bi ${icono} fs-5" style="color: ${colorBorder}"></i>
        <div style="flex: 1;">
            <strong style="display: block; font-size: 0.88rem; font-family: 'Plus Jakarta Sans', sans-serif;">${titulo}</strong>
            <small style="color: #64748b; font-size: 0.8rem; line-height: 1.3; display: block; margin-top: 2px;">${mensaje}</small>
        </div>
        <button style="background:none; border:none; color:#94a3b8; font-size:1.1rem; cursor:pointer;" onclick="this.parentElement.remove()">×</button>
    `;

    container.appendChild(toastEl);
    setTimeout(() => {
        toastEl.style.opacity = '0';
        toastEl.style.transform = 'translateX(20px)';
        toastEl.style.transition = 'all 0.4s';
        setTimeout(() => toastEl.remove(), 400);
    }, 4000);
}

/**
 * Títulos y subtítulos por módulo.
 */
const TITULOS_PAGINAS = {
    'tab-panel-kpis': { titulo: 'Dashboard', subtitulo: 'Resumen general del sistema' },
    'tab-panel-catalogo': { titulo: 'Inventario / Catálogo', subtitulo: 'Catálogo maestro de productos y existencias' },
    'tab-panel-pos': { titulo: 'Ventas (POS)', subtitulo: 'Punto de venta y facturación con control de caducidad' },
    'tab-panel-boletas': { titulo: 'Boletas', subtitulo: 'Historial y comprobantes de venta emitidos' },
    'tab-panel-clientes': { titulo: 'Clientes', subtitulo: 'Directorio y gestión comercial de clientes' },
    'tab-panel-inventario': { titulo: 'Compras & Lotes', subtitulo: 'Recepción de compras a proveedores, mermas y Kardex' },
    'tab-panel-categorias': { titulo: 'Categorías', subtitulo: 'Gestión y clasificación taxonómica de productos' },
    'tab-panel-proveedores': { titulo: 'Proveedores', subtitulo: 'Empresas distribuidoras y abastecimiento' },
    'tab-panel-usuarios': { titulo: 'Usuarios / Personal', subtitulo: 'Gestión de personal de tienda y niveles de acceso' },
    'tab-panel-reportes': { titulo: 'Reportes & Analítica', subtitulo: 'Métricas de rendimiento y ranking para toma de decisiones' }
};

/**
 * Navegación y cambio de vista activa entre módulos.
 */
function irATab(tabPanelId, callback) {
    document.querySelectorAll('.panel-modulo-sm').forEach(el => el.classList.add('d-none'));
    document.querySelectorAll('.nav-item-btn').forEach(el => el.classList.remove('activo'));

    const panel = document.getElementById(tabPanelId);
    if (panel) panel.classList.remove('d-none');

    const navBtn = document.querySelector(`[data-target-panel="${tabPanelId}"]`);
    if (navBtn) navBtn.classList.add('activo');

    const info = TITULOS_PAGINAS[tabPanelId];
    if (info) {
        const t = document.getElementById('topbarTitulo');
        const s = document.getElementById('topbarSubtitulo');
        if (t) t.innerText = info.titulo;
        if (s) s.innerText = info.subtitulo;
    }

    if (callback && typeof window[callback] === 'function') {
        window[callback]();
    }
}
