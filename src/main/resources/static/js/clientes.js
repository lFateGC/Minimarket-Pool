// ==============================================================================
// MODULO: GESTIÓN DE CLIENTES (clientes.js)
// Minimarket Pool — CRUD, Búsquedas y Vinculación con POS
// ==============================================================================

let listaClientesGlobal = [];
let clienteEditandoId = null;

/**
 * Carga todos los clientes desde el backend Spring Boot (GET /api/clientes).
 */
async function cargarClientes() {
    try {
        const res = await fetch(`${API_BASE}/clientes`, { headers: getAuthHeaders() });
        const resProc = await procesarRespuestaHttp(res);
        if (!resProc) return;

        if (resProc.ok) {
            listaClientesGlobal = await resProc.json();
            renderizarTablaClientes(listaClientesGlobal);
        } else {
            mostrarToast('Error', 'No se pudo cargar la lista de clientes.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Conexión', 'No se pudo conectar con el servidor para obtener clientes.', 'danger');
    }
}

/**
 * Renderiza la tabla HTML de clientes en el panel de administración.
 */
function renderizarTablaClientes(clientes) {
    const tbody = document.querySelector('#tablaClientes tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!clientes || clientes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-4">No hay clientes registrados o coincidentes.</td></tr>';
        return;
    }

    const esAdmin = usuarioActual && usuarioActual.rol === 'ADMIN';

    clientes.forEach((c, idx) => {
        const badgeTipo = c.tipoDocumento === 'RUC' 
            ? '<span class="badge bg-primary-subtle text-primary border">RUC</span>'
            : c.tipoDocumento === 'CE'
                ? '<span class="badge bg-info-subtle text-info border">C.E.</span>'
                : '<span class="badge bg-secondary-subtle text-dark border">DNI</span>';

        const badgeEstado = c.activo 
            ? '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-check-circle me-1"></i>Activo</span>'
            : '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-x-circle me-1"></i>Inactivo</span>';

        const totalCompras = c.totalCompras !== undefined ? c.totalCompras : 0;
        const nombreCompleto = c.nombreCompleto || `${c.nombres} ${c.apellidos || ''}`.trim();

        tbody.innerHTML += `
            <tr>
                <td class="text-center text-muted">${idx + 1}</td>
                <td>${badgeTipo}</td>
                <td><code class="fw-bold fs-6 text-dark">${c.numeroDocumento}</code></td>
                <td>
                    <div class="fw-bold text-dark">${nombreCompleto}</div>
                    <small class="text-muted"><i class="bi bi-geo-alt me-1"></i>${c.direccion || 'Sin dirección registrada'}</small>
                </td>
                <td>${c.telefono ? `<i class="bi bi-telephone me-1 text-muted"></i>${c.telefono}` : '<span class="text-muted">-</span>'}</td>
                <td>${c.email ? `<small class="text-muted">${c.email}</small>` : '<span class="text-muted">-</span>'}</td>
                <td class="text-center"><span class="badge bg-light text-dark border fw-bold">${totalCompras} compras</span></td>
                <td class="text-center">${badgeEstado}</td>
                <td class="text-center">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" onclick="abrirModalEditarCliente(${c.id})" title="Editar cliente">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        ${esAdmin ? `
                        <button class="btn btn-outline-danger" onclick="eliminarCliente(${c.id}, '${nombreCompleto}')" title="Eliminar o desactivar cliente">
                            <i class="bi bi-trash"></i>
                        </button>` : ''}
                    </div>
                </td>
            </tr>
        `;
    });
}

/**
 * Filtra los clientes en tiempo real por número de documento o nombre.
 */
function filtrarClientes() {
    const q = (document.getElementById('inputBuscarCliente')?.value || '').toLowerCase().trim();
    if (!q) {
        renderizarTablaClientes(listaClientesGlobal);
        return;
    }

    const filtrados = listaClientesGlobal.filter(c => {
        const nom = (c.nombreCompleto || `${c.nombres} ${c.apellidos || ''}`).toLowerCase();
        const doc = (c.numeroDocumento || '').toLowerCase();
        const dir = (c.direccion || '').toLowerCase();
        return nom.includes(q) || doc.includes(q) || dir.includes(q);
    });

    renderizarTablaClientes(filtrados);
}

/**
 * Abre el modal para registrar un nuevo cliente desde la pestaña Clientes.
 */
function abrirModalNuevoCliente() {
    clienteEditandoId = null;
    const form = document.getElementById('formCliente');
    if (form) form.reset();
    document.getElementById('modalClienteTitulo').innerText = 'Registrar Nuevo Cliente';
    document.getElementById('clienteTipoDoc').value = 'DNI';
    actualizarReglasDocumento();

    const modal = new bootstrap.Modal(document.getElementById('modalCliente'));
    modal.show();
}

/**
 * Abre el modal de nuevo cliente directamente desde el punto de venta (POS).
 */
function abrirModalNuevoClienteDesdePOS() {
    abrirModalNuevoCliente();
    document.getElementById('modalClienteTitulo').innerText = 'Registrar Cliente (Punto de Venta)';
}

/**
 * Carga los datos de un cliente existente y abre el modal para su edición.
 */
function abrirModalEditarCliente(id) {
    const cliente = listaClientesGlobal.find(c => c.id === id);
    if (!cliente) return;

    clienteEditandoId = id;
    document.getElementById('modalClienteTitulo').innerText = 'Editar Datos del Cliente';
    document.getElementById('clienteTipoDoc').value = cliente.tipoDocumento || 'DNI';
    document.getElementById('clienteNumDoc').value = cliente.numeroDocumento || '';
    document.getElementById('clienteNombres').value = cliente.nombres || '';
    document.getElementById('clienteApellidos').value = cliente.apellidos || '';
    document.getElementById('clienteTelefono').value = cliente.telefono || '';
    document.getElementById('clienteEmail').value = cliente.email || '';
    document.getElementById('clienteDireccion').value = cliente.direccion || '';

    actualizarReglasDocumento();
    const modal = new bootstrap.Modal(document.getElementById('modalCliente'));
    modal.show();
}

/**
 * Ajusta los placeholders y longitudes según el tipo de documento seleccionado (DNI/RUC/CE).
 */
function actualizarReglasDocumento() {
    const tipo = document.getElementById('clienteTipoDoc')?.value || 'DNI';
    const inputDoc = document.getElementById('clienteNumDoc');
    const labelNom = document.getElementById('labelClienteNombres');
    const colApe = document.getElementById('colClienteApellidos');

    if (!inputDoc) return;

    if (tipo === 'DNI') {
        inputDoc.maxLength = 8;
        inputDoc.minLength = 8;
        inputDoc.placeholder = '8 dígitos (Ej: 72345678)';
        if (labelNom) labelNom.innerText = 'Nombres *';
        if (colApe) colApe.style.display = 'block';
    } else if (tipo === 'RUC') {
        inputDoc.maxLength = 11;
        inputDoc.minLength = 11;
        inputDoc.placeholder = '11 dígitos (Ej: 20601234567)';
        if (labelNom) labelNom.innerText = 'Razón Social *';
        if (colApe) colApe.style.display = 'none';
    } else {
        inputDoc.maxLength = 12;
        inputDoc.minLength = 8;
        inputDoc.placeholder = 'N° Carné Extranjería';
        if (labelNom) labelNom.innerText = 'Nombres *';
        if (colApe) colApe.style.display = 'block';
    }
}

/**
 * Guarda o actualiza el cliente enviando la petición al backend.
 */
async function guardarCliente(e) {
    e.preventDefault();

    const tipoDocumento = document.getElementById('clienteTipoDoc').value;
    const numeroDocumento = document.getElementById('clienteNumDoc').value.trim();
    const nombres = document.getElementById('clienteNombres').value.trim();
    const apellidos = tipoDocumento === 'RUC' ? '' : (document.getElementById('clienteApellidos').value.trim() || null);
    const telefono = document.getElementById('clienteTelefono').value.trim() || null;
    const email = document.getElementById('clienteEmail').value.trim() || null;
    const direccion = document.getElementById('clienteDireccion').value.trim() || null;

    if (tipoDocumento === 'DNI' && numeroDocumento.length !== 8) {
        mostrarToast('Validación', 'El DNI debe tener exactamente 8 dígitos.', 'warning');
        return;
    }

    if (tipoDocumento === 'RUC' && numeroDocumento.length !== 11) {
        mostrarToast('Validación', 'El RUC debe tener exactamente 11 dígitos.', 'warning');
        return;
    }

    const payload = {
        tipoDocumento,
        numeroDocumento,
        nombres,
        apellidos,
        telefono,
        email,
        direccion
    };

    const url = clienteEditandoId ? `${API_BASE}/clientes/${clienteEditandoId}` : `${API_BASE}/clientes`;
    const metodo = clienteEditandoId ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method: metodo,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (res.ok) {
            mostrarToast('Éxito', clienteEditandoId ? 'Cliente actualizado correctamente.' : 'Cliente registrado exitosamente.', 'success');
            
            // Cerrar modal
            const modalEl = document.getElementById('modalCliente');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            // Refrescar listados
            await cargarClientes();
            await poblarSelectClientesPOS(data.id); // Si estábamos en POS, seleccionarlo automáticamente
        } else {
            mostrarToast('Error al Guardar', data.mensaje || 'No se pudo guardar la información del cliente.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'Fallo al comunicar con el servidor Spring Boot.', 'danger');
    }
}

/**
 * Elimina o desactiva un cliente (DELETE /api/clientes/{id}).
 */
async function eliminarCliente(id, nombre) {
    if (!confirm(`¿Estás seguro de que deseas eliminar o dar de baja al cliente "${nombre}"?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/clientes/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.ok) {
            mostrarToast('Cliente Eliminado', `El cliente "${nombre}" ha sido procesado con éxito.`, 'info');
            await cargarClientes();
            await poblarSelectClientesPOS();
        } else {
            const data = await res.json().catch(() => ({}));
            mostrarToast('Error al Eliminar', data.mensaje || 'No se pudo eliminar el cliente.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo conectar con el servidor.', 'danger');
    }
}

/**
 * Puebla el dropdown del POS con los clientes activos disponibles.
 */
async function poblarSelectClientesPOS(seleccionarId = null) {
    const sel = document.getElementById('posSelectCliente');
    if (!sel) return;

    try {
        const res = await fetch(`${API_BASE}/clientes/activos`, { headers: getAuthHeaders() });
        if (res.ok) {
            const clientes = await res.json();
            sel.innerHTML = '<option value="">Público General (Sin documento)</option>';
            clientes.forEach(c => {
                const nombre = c.nombreCompleto || `${c.nombres} ${c.apellidos || ''}`.trim();
                const selAttr = (seleccionarId && c.id === seleccionarId) ? 'selected' : '';
                sel.innerHTML += `<option value="${c.id}" ${selAttr}>${c.tipoDocumento}: ${c.numeroDocumento} — ${nombre}</option>`;
            });
        }
    } catch (e) {
        console.warn('Error al cargar clientes para POS:', e);
    }
}
