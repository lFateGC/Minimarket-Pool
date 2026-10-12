// ==============================================================================
// PUNTO DE ENTRADA PRINCIPAL (app.js)
// ==============================================================================

document.addEventListener('DOMContentLoaded', async () => {
    const tokenGuardado = localStorage.getItem('jwt_token');
    const userGuardado = localStorage.getItem('jwt_user');

    if (tokenGuardado && userGuardado) {
        tokenJWT = tokenGuardado;
        usuarioActual = JSON.parse(userGuardado);

        // Validar si el token sigue siendo válido contra el backend
        try {
            const res = await fetch(`${API_BASE}/auth/perfil`, {
                headers: getAuthHeaders()
            });

            if (res.ok) {
                mostrarDashboard();
            } else {
                console.warn('Token guardado no válido en el servidor. Reiniciando sesión...');
                cerrarSesion();
            }
        } catch (err) {
            // Error de red, permitir entrar y mostrar error si falla la API
            mostrarDashboard();
        }
    } else {
        mostrarLogin();
    }
});
