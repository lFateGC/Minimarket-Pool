// ==============================================================================
// MODULO DE ADMINISTRACIÓN: GESTIÓN DE USUARIOS Y PERSONAL (usuarios.js)
// ==============================================================================

let listaUsuariosGlobal = [];

/**
 * Carga la lista de usuarios del sistema (GET /api/auth/usuarios)
 */
async function cargarUsuariosAdmin() {
    try {
        const res = await fetch(`${API_BASE}/auth/usuarios`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaUsuariosGlobal = await res.json();
            renderizarTablaUsuarios(listaUsuariosGlobal);
        }
    } catch (err) {
        mostrarToast('Error', 'No se pudo cargar la lista de usuarios', 'danger');
    }
}

/**
 * Renderiza la tabla de usuarios en el panel de administración.
 */
function renderizarTablaUsuarios(usuarios) {
    const tbody = document.querySelector('#tablaUsuariosAdmin tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (usuarios.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No hay usuarios registrados.</td></tr>';
        return;
    }

    const emailActual = usuarioActual ? usuarioActual.email : '';

    usuarios.forEach((u, index) => {
        const esAdmin = u.rol === 'ADMIN';
        const esPropioUsuario = u.email === emailActual;

        const badgeRol = esAdmin
            ? '<span class="badge bg-danger text-white fw-bold"><i class="bi bi-shield-lock me-1"></i> Administrador</span>'
            : '<span class="badge bg-primary-subtle text-primary border border-primary fw-bold"><i class="bi bi-person me-1"></i> Vendedor (Caja)</span>';

        const btnEliminar = esPropioUsuario
            ? `<button class="btn btn-sm btn-outline-secondary py-0 px-2 opacity-50" title="No puedes eliminar tu propia cuenta" disabled><i class="bi bi-trash"></i></button>`
            : `<button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="eliminarUsuarioAdmin(${u.id}, '${u.email}')" title="Eliminar Usuario"><i class="bi bi-trash"></i></button>`;

        tbody.innerHTML += `
            <tr>
                <td><span class="badge bg-light text-dark border fw-bold">#${index + 1}</span></td>
                <td>
                    <div class="d-flex align-items-center gap-2">
                        <div class="avatar-circulo" style="width: 32px; height: 32px; font-size: 0.85rem; background: ${esAdmin ? 'var(--pool-rojo)' : '#0284c7'};">
                            ${u.nombre ? u.nombre.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <strong>${u.nombre}</strong>
                        ${esPropioUsuario ? '<span class="badge bg-secondary-subtle text-secondary" style="font-size: 0.68rem;">Tú</span>' : ''}
                    </div>
                </td>
                <td><code>${u.email}</code></td>
                <td>${badgeRol}</td>
                <td><span class="badge bg-success-subtle text-success"><span class="punto-verde me-1 d-inline-block"></span> Activo</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-primary py-0 px-2 me-1" onclick="abrirModalEditarUsuario(${u.id})" title="Editar Usuario">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    ${btnEliminar}
                </td>
            </tr>
        `;
    });
}

/**
 * Abre el modal para registrar un nuevo empleado / usuario desde el panel de administración.
 */
function abrirModalNuevoUsuario() {
    document.getElementById('modalUsuarioAdminTitle').innerHTML = '<i class="bi bi-person-plus me-2"></i> Registrar Empleado / Usuario';
    document.getElementById('adminRegId').value = '';
    document.getElementById('adminRegNombre').value = '';
    document.getElementById('adminRegEmail').value = '';
    document.getElementById('adminRegPassword').value = '';
    document.getElementById('adminRegPassword').required = true;
    document.getElementById('adminRegRol').value = 'VENDEDOR';
    
    document.getElementById('helpAdminPassword')?.classList.add('d-none');
    document.getElementById('btnSubmitUsuarioAdmin').innerText = 'Guardar Usuario';

    const modal = new bootstrap.Modal(document.getElementById('modalNuevoUsuarioAdmin'));
    modal.show();
}

/**
 * Abre el modal para editar un usuario existente.
 */
function abrirModalEditarUsuario(id) {
    const usuario = listaUsuariosGlobal.find(u => u.id === id);
    if (!usuario) return;

    document.getElementById('modalUsuarioAdminTitle').innerHTML = '<i class="bi bi-pencil-square me-2"></i> Editar Usuario';
    document.getElementById('adminRegId').value = usuario.id;
    document.getElementById('adminRegNombre').value = usuario.nombre;
    document.getElementById('adminRegEmail').value = usuario.email;
    document.getElementById('adminRegPassword').value = '';
    document.getElementById('adminRegPassword').required = false;
    document.getElementById('adminRegRol').value = usuario.rol;

    document.getElementById('helpAdminPassword')?.classList.remove('d-none');
    document.getElementById('btnSubmitUsuarioAdmin').innerText = 'Actualizar Usuario';

    const modal = new bootstrap.Modal(document.getElementById('modalNuevoUsuarioAdmin'));
    modal.show();
}

/**
 * Ejecuta el guardado (creación o edición) de un usuario desde el panel administrativo.
 */
async function guardarUsuarioAdmin(e) {
    e.preventDefault();
    const id = document.getElementById('adminRegId').value;
    const nombre = document.getElementById('adminRegNombre').value.trim();
    const email = document.getElementById('adminRegEmail').value.trim();
    const password = document.getElementById('adminRegPassword').value.trim();
    const rol = document.getElementById('adminRegRol').value;

    const payload = { nombre, email, rol };
    if (password) {
        payload.password = password;
    }

    const url = id ? `${API_BASE}/auth/usuarios/${id}` : `${API_BASE}/auth/registro`;
    const method = id ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const accion = id ? 'actualizado' : 'registrado';
            mostrarToast('Usuario Guardado', `El usuario ${nombre} fue ${accion} con éxito.`, 'success');
            
            // Si el usuario editado es el usuario en sesión activa, actualizar localStorage y UI
            if (id && usuarioActual && String(usuarioActual.id) === String(id)) {
                usuarioActual.nombre = nombre;
                usuarioActual.email = email;
                usuarioActual.rol = rol;
                localStorage.setItem('jwt_user', JSON.stringify(usuarioActual));
                mostrarDashboard();
            }

            bootstrap.Modal.getInstance(document.getElementById('modalNuevoUsuarioAdmin')).hide();
            cargarUsuariosAdmin();
        } else {
            const data = await res.json();
            mostrarToast('Error al Guardar', data.mensaje || 'Datos inválidos o correo ya registrado.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'Error al conectar con el servidor', 'danger');
    }
}

/**
 * Elimina un usuario del sistema (solo administradores).
 */
async function eliminarUsuarioAdmin(id, email) {
    if (usuarioActual && usuarioActual.email === email) {
        mostrarToast('Operación no permitida', 'No puedes eliminar tu propia cuenta en uso.', 'warning');
        return;
    }

    if (!confirm(`¿Está seguro de eliminar al usuario con correo "${email}"? Esta acción no se puede deshacer.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/auth/usuarios/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.ok) {
            mostrarToast('Usuario Eliminado', `Se eliminó al usuario ${email}.`, 'info');
            cargarUsuariosAdmin();
        } else {
            const data = await res.json();
            mostrarToast('Error al Eliminar', data.mensaje || 'No se pudo eliminar el usuario.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo comunicar con el servidor', 'danger');
    }
}
