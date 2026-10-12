// ==============================================================================
// MODULO: AUTENTICACIÓN, SESIÓN Y ROLES RBAC (auth.js)
// Estilo y Flujo inspirados en StockMaster
// ==============================================================================

/**
 * Procesa el inicio de sesión contra el backend Spring Boot (POST /api/auth/login).
 */
async function ejecutarLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    const btnSubmit = document.getElementById('btnLoginSubmit');

    try {
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Autenticando...';

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

            mostrarToast('Bienvenido a StockMaster', `Sesión iniciada como ${usuarioActual.nombre}`, 'success');
            mostrarDashboard();
        } else {
            mostrarToast('Error de Autenticación', data.mensaje || 'Credenciales incorrectas.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo conectar con el servidor Spring Boot', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = 'Iniciar Sesión';
    }
}

/**
 * Registra un nuevo usuario en la base de datos (POST /api/auth/registro).
 */
async function ejecutarRegistro(e) {
    e.preventDefault();
    const nombre = document.getElementById('regNombre').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value.trim();
    const rol = document.getElementById('regRol').value;

    try {
        const res = await fetch(`${API_BASE}/auth/registro`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, email, password, rol })
        });

        const data = await res.json();

        if (res.ok) {
            mostrarToast('Cuenta Creada', `Usuario ${nombre} registrado exitosamente. Ya puedes iniciar sesión.`, 'success');
            document.getElementById('tab-login').click();
            document.getElementById('loginEmail').value = email;
            document.getElementById('loginPassword').value = '';
        } else {
            mostrarToast('Error de Registro', data.mensaje || 'No se pudo crear la cuenta', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo comunicar con el servidor', 'danger');
    }
}

/**
 * Cierra la sesión activa y regresa al formulario de acceso.
 */
function cerrarSesion() {
    tokenJWT = null;
    usuarioActual = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_user');
    mostrarToast('Sesión Finalizada', 'Has cerrado sesión correctamente.', 'info');
    mostrarLogin();
}

/**
 * Cambia la vista hacia el Dashboard principal de StockMaster.
 */
function mostrarDashboard() {
    document.getElementById('vistaAuth').classList.add('d-none');
    document.getElementById('vistaArmazon').classList.remove('d-none');

    if (usuarioActual) {
        const inicial = usuarioActual.nombre ? usuarioActual.nombre.charAt(0).toUpperCase() : 'U';
        document.getElementById('sidebarUserAvatar').innerText = inicial;
        document.getElementById('sidebarUserName').innerText = usuarioActual.nombre || 'Usuario';
        
        const esAdmin = usuarioActual.rol === 'ADMIN';
        const rolTexto = esAdmin ? '👑 Admin' : '🔧 Vendedor';
        document.getElementById('sidebarUserRole').innerText = rolTexto;

        const headerRoleBadge = document.getElementById('headerRoleBadge');
        if (headerRoleBadge) {
            headerRoleBadge.innerText = esAdmin ? '👑 Admin' : '🔧 Vendedor';
            headerRoleBadge.style.background = esAdmin ? 'var(--azul-sm)' : 'var(--rojo-marca)';
        }

        const saludoEl = document.getElementById('saludoUsuario');
        if (saludoEl) {
            saludoEl.innerText = `Bienvenido, ${usuarioActual.nombre} 👏`;
        }

        // Control de visibilidad RBAC
        document.querySelectorAll('.solo-admin').forEach(el => {
            if (esAdmin) {
                el.classList.remove('d-none');
            } else {
                el.classList.add('d-none');
            }
        });
    }

    irATab('tab-panel-kpis', 'cargarDashboard');
}

/**
 * Muestra la vista de inicio de sesión / registro.
 */
function mostrarLogin() {
    document.getElementById('vistaArmazon').classList.add('d-none');
    document.getElementById('vistaAuth').classList.remove('d-none');
}
