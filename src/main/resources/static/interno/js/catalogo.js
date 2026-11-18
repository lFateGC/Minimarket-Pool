// ==============================================================================
// MODULO: CATÁLOGO, PRODUCTOS, CATEGORÍAS Y PROVEEDORES (catalogo.js)
// Vistas Dedicadas Completas con CRUD
// ==============================================================================

let listaProductosGlobal = [];
let listaCategoriasGlobal = [];
let listaProveedoresGlobal = [];

/**
 * Carga todos los catálogos y llena las vistas correspondientes.
 */
async function cargarCatalogo() {
    await cargarCategorias();
    await cargarProveedores();
    await cargarProductos();
}

// ==============================================================================
// 1. PRODUCTOS / INVENTARIO MAESTRO
// ==============================================================================

async function cargarProductos() {
    try {
        const res = await fetch(`${API_BASE}/productos`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaProductosGlobal = await res.json();
            filtrarTablaProductos();
        }
    } catch (err) {
        mostrarToast('Error', 'No se pudieron cargar los productos', 'danger');
    }
}

function filtrarTablaProductos() {
    const query = (document.getElementById('busquedaProductoTabla')?.value || '').toLowerCase();
    const catFiltro = document.getElementById('filtroCategoriaTabla')?.value || '';
    const tbody = document.querySelector('#tablaProductos tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    const filtrados = listaProductosGlobal.filter(p => {
        const matchTexto = p.nombre.toLowerCase().includes(query) || p.sku.toLowerCase().includes(query);
        const matchCat = catFiltro === '' || p.categoriaNombre === catFiltro;
        return matchTexto && matchCat;
    });

    if (filtrados.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No se encontraron productos.</td></tr>';
        return;
    }

    const esAdmin = usuarioActual && usuarioActual.rol === 'ADMIN';

    filtrados.forEach(p => {
        const img = p.imagenUrl ? p.imagenUrl : 'https://placehold.co/100x100/f1f5f9/94a3b8?text=Sin+Foto';
        const badgeStock = p.stockActual <= 0
            ? '<span class="badge bg-danger">Agotado</span>'
            : p.stockActual <= (p.stockMinimo || 5)
            ? `<span class="badge bg-warning text-dark">${p.stockActual} (Bajo)</span>`
            : `<span class="badge bg-success">${p.stockActual} unid.</span>`;

        const acciones = esAdmin ? `
            <td class="text-center">
                <button class="btn btn-sm btn-outline-primary py-0 px-2 me-1" onclick="abrirModalEditarProducto(${p.id})" title="Editar">
                    <i class="bi bi-pencil-square"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="eliminarProducto(${p.id})" title="Eliminar">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        ` : '';

        tbody.innerHTML += `
            <tr>
                <td>
                    <img src="${img}" class="rounded-2 border" style="width: 44px; height: 44px; object-fit: cover;" onerror="this.src='https://placehold.co/100x100/f1f5f9/94a3b8?text=Sin+Foto'">
                </td>
                <td><code>${p.sku}</code></td>
                <td>
                    <strong>${p.nombre}</strong><br>
                    <small class="text-muted">Mínimo: ${p.stockMinimo || 0} | Caja: ${p.unidadesPorCaja || 1}u</small>
                </td>
                <td><span class="badge bg-light text-dark border">${p.categoriaNombre || 'Sin categoría'}</span></td>
                <td><small>${p.proveedorNombre || 'Sin proveedor'}</small></td>
                <td class="text-end fw-bold" style="color: var(--pool-rojo);">S/ ${parseFloat(p.precioUnitario).toFixed(2)}</td>
                <td class="text-center">${badgeStock}</td>
                ${acciones}
            </tr>
        `;
    });
}

function abrirModalProducto() {
    document.getElementById('modalProdTitle').innerHTML = '<i class="bi bi-box-seam me-2"></i> Nuevo Producto';
    document.getElementById('prodId').value = '';
    document.getElementById('prodNombre').value = '';
    document.getElementById('prodSku').value = '';
    document.getElementById('prodPrecioUnit').value = '';
    document.getElementById('prodStockMin').value = '10';
    document.getElementById('prodUnidadesCaja').value = '1';
    document.getElementById('prodImagenUrl').value = '';
    
    const fileInput = document.getElementById('prodImagenFile');
    if (fileInput) fileInput.value = '';

    const statusEl = document.getElementById('imgUploadStatus');
    if (statusEl) statusEl.innerHTML = '<i class="bi bi-shield-check text-success me-1"></i> La foto se almacena en Supabase Storage (bucket <code>productos-minimarket</code>).';

    actualizarPrevisualizacionImagen('');

    poblarSelectsFormularioProducto();

    const modal = new bootstrap.Modal(document.getElementById('modalProducto'));
    modal.show();
}

function abrirModalEditarProducto(id) {
    const prod = listaProductosGlobal.find(p => p.id === id);
    if (!prod) return;

    document.getElementById('modalProdTitle').innerHTML = '<i class="bi bi-pencil-square me-2"></i> Editar Producto';
    document.getElementById('prodId').value = prod.id;
    document.getElementById('prodNombre').value = prod.nombre;
    document.getElementById('prodSku').value = prod.sku;
    document.getElementById('prodPrecioUnit').value = prod.precioUnitario;
    document.getElementById('prodStockMin').value = prod.stockMinimo;
    document.getElementById('prodUnidadesCaja').value = prod.unidadesPorCaja || 1;
    document.getElementById('prodImagenUrl').value = prod.imagenUrl || '';
    
    const fileInput = document.getElementById('prodImagenFile');
    if (fileInput) fileInput.value = '';

    const statusEl = document.getElementById('imgUploadStatus');
    if (statusEl) {
        if (prod.imagenUrl) {
            statusEl.innerHTML = '<i class="bi bi-check-circle-fill text-success me-1"></i> Imagen vinculada en Supabase Storage / Web.';
        } else {
            statusEl.innerHTML = '<i class="bi bi-info-circle text-muted me-1"></i> Sin imagen vinculada. Selecciona una foto o pega una URL.';
        }
    }

    actualizarPrevisualizacionImagen(prod.imagenUrl);

    // Determinar ID de categoría (por ID o por coincidencia de nombre)
    let catId = prod.categoriaId;
    if (!catId && prod.categoriaNombre && listaCategoriasGlobal.length > 0) {
        const cMatch = listaCategoriasGlobal.find(c => c.nombre.trim().toLowerCase() === prod.categoriaNombre.trim().toLowerCase());
        if (cMatch) catId = cMatch.id;
    }

    // Determinar ID de proveedor (por ID o por coincidencia de nombre)
    let provId = prod.proveedorId;
    if (!provId && prod.proveedorNombre && listaProveedoresGlobal.length > 0) {
        const prMatch = listaProveedoresGlobal.find(pr => pr.nombre.trim().toLowerCase() === prod.proveedorNombre.trim().toLowerCase());
        if (prMatch) provId = prMatch.id;
    }

    poblarSelectsFormularioProducto(catId, provId);

    const modal = new bootstrap.Modal(document.getElementById('modalProducto'));
    modal.show();
}

function poblarSelectsFormularioProducto(selectedCatId = null, selectedProvId = null) {
    const selCat = document.getElementById('prodCategoriaId');
    const selProv = document.getElementById('prodProveedorId');

    if (selCat) {
        selCat.innerHTML = '<option value="">Seleccione categoría...</option>';
        listaCategoriasGlobal.forEach(c => {
            const isSelected = selectedCatId && String(selectedCatId) === String(c.id);
            const sel = isSelected ? 'selected' : '';
            selCat.innerHTML += `<option value="${c.id}" ${sel}>${c.nombre}</option>`;
        });
        if (selectedCatId) {
            selCat.value = String(selectedCatId);
        }
    }

    if (selProv) {
        selProv.innerHTML = '<option value="">Seleccione proveedor...</option>';
        listaProveedoresGlobal.forEach(pr => {
            const isSelected = selectedProvId && String(selectedProvId) === String(pr.id);
            const sel = isSelected ? 'selected' : '';
            selProv.innerHTML += `<option value="${pr.id}" ${sel}>${pr.nombre} (RUC: ${pr.ruc})</option>`;
        });
        if (selectedProvId) {
            selProv.value = String(selectedProvId);
        }
    }
}

function actualizarPrevisualizacionImagen(url) {
    const imgEl = document.getElementById('previewImgProducto');
    if (!imgEl) return;
    if (url && (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:image/'))) {
        imgEl.src = url;
    } else {
        imgEl.src = 'https://placehold.co/100x100/f1f5f9/94a3b8?text=Vista+Previa';
    }
}

/**
 * Maneja la subida en vivo de la foto seleccionada a Supabase Storage
 */
async function manejarSeleccionImagen(input) {
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];

    // Validación de tamaño (máximo 5MB)
    if (file.size > 5 * 1024 * 1024) {
        mostrarToast('Imagen muy pesada', 'El archivo no debe superar los 5MB', 'warning');
        input.value = '';
        return;
    }

    // 1. Mostrar previsualización instantánea local
    const reader = new FileReader();
    reader.onload = function(e) {
        actualizarPrevisualizacionImagen(e.target.result);
    };
    reader.readAsDataURL(file);

    // 2. Mostrar indicador de subida en progreso
    const spinner = document.getElementById('spinnerUploadImg');
    const statusEl = document.getElementById('imgUploadStatus');
    if (spinner) spinner.classList.remove('d-none');
    if (statusEl) statusEl.innerHTML = '<i class="bi bi-arrow-repeat spin me-1 text-primary"></i> Subiendo imagen a Supabase Storage...';

    // 3. Subir archivo al backend -> Supabase Storage
    const formData = new FormData();
    formData.append('file', file);

    try {
        const token = localStorage.getItem('jwt_token');
        const headers = {};
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const res = await fetch(`${API_BASE}/productos/upload-imagen`, {
            method: 'POST',
            headers: headers,
            body: formData
        });

        if (res.ok) {
            const data = await res.json();
            const urlFinal = data.url;
            document.getElementById('prodImagenUrl').value = urlFinal;
            actualizarPrevisualizacionImagen(urlFinal);
            if (statusEl) {
                statusEl.innerHTML = '<span class="text-success fw-bold"><i class="bi bi-check-circle-fill me-1"></i> ¡Imagen guardada en Supabase Storage!</span>';
            }
            mostrarToast('Imagen Subida', 'La foto se almacenó correctamente en Supabase Storage.', 'success');
        } else {
            const err = await res.json().catch(() => ({}));
            if (statusEl) {
                statusEl.innerHTML = '<span class="text-danger"><i class="bi bi-exclamation-triangle-fill me-1"></i> Error al subir imagen a Supabase.</span>';
            }
            mostrarToast('Error al Subir', err.mensaje || err.error || 'No se pudo subir la imagen a Supabase Storage', 'danger');
        }
    } catch (error) {
        console.error('Error subiendo imagen:', error);
        if (statusEl) {
            statusEl.innerHTML = '<span class="text-warning"><i class="bi bi-exclamation-circle me-1"></i> Imagen cargada en vista previa local.</span>';
        }
    } finally {
        if (spinner) spinner.classList.add('d-none');
    }
}

async function guardarProducto(e) {
    e.preventDefault();
    const id = document.getElementById('prodId').value;
    const nombre = document.getElementById('prodNombre').value.trim();
    const sku = document.getElementById('prodSku').value.trim();
    const categoriaId = parseInt(document.getElementById('prodCategoriaId').value);
    const proveedorId = parseInt(document.getElementById('prodProveedorId').value);
    const precioUnitario = parseFloat(document.getElementById('prodPrecioUnit').value);
    const stockMinimo = parseInt(document.getElementById('prodStockMin').value);
    const unidadesPorCaja = parseInt(document.getElementById('prodUnidadesCaja').value) || 1;
    const imagenUrl = document.getElementById('prodImagenUrl').value.trim();

    const payload = {
        nombre,
        sku,
        categoriaId,
        proveedorId,
        precioUnitario,
        stockMinimo,
        unidadesPorCaja,
        imagenUrl: imagenUrl || null
    };

    const url = id ? `${API_BASE}/productos/${id}` : `${API_BASE}/productos`;
    const method = id ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast('Producto Guardado', `El producto ${nombre} se guardó con su imagen en Supabase.`, 'success');
            bootstrap.Modal.getInstance(document.getElementById('modalProducto')).hide();
            cargarProductos();
            if (typeof cargarPosProductos === 'function') {
                cargarPosProductos();
            }
        } else {
            const err = await res.json();
            mostrarToast('Error al Guardar', err.mensaje || 'Datos duplicados o incorrectos.', 'danger');
        }
    } catch (err) {
        mostrarToast('Error de Red', 'No se pudo comunicar con el backend Spring Boot', 'danger');
    }
}

async function eliminarProducto(id) {
    if (!confirm('¿Está seguro de eliminar este producto? Esta acción no se puede deshacer.')) return;

    try {
        const res = await fetch(`${API_BASE}/productos/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.ok) {
            mostrarToast('Producto Eliminado', 'Se removió el producto del catálogo.', 'info');
            cargarProductos();
        } else {
            const err = await res.json();
            mostrarToast('No se puede eliminar', err.mensaje || 'El producto posee lotes o ventas asociadas.', 'danger');
        }
    } catch (e) {
        mostrarToast('Error', 'Error de red al eliminar', 'danger');
    }
}

// ==============================================================================
// 2. CATEGORÍAS (VISTA DEDICADA COMPLETA)
// ==============================================================================

async function cargarCategorias() {
    try {
        const res = await fetch(`${API_BASE}/categorias`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaCategoriasGlobal = await res.json();
            renderizarVistaCategorias();
            
            // Llenar filtro de categorías en la tabla de productos y POS
            poblarFiltroCategoriasProductos();
        }
    } catch (e) {}
}

function renderizarVistaCategorias() {
    const tbody = document.querySelector('#tablaCategoriasAdmin tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (listaCategoriasGlobal.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No hay categorías registradas.</td></tr>';
        return;
    }

    listaCategoriasGlobal.forEach((cat, index) => {
        // Contar productos pertenecientes a esta categoría
        const cantProds = listaProductosGlobal.filter(p => p.categoriaId === cat.id || p.categoriaNombre === cat.nombre).length;

        tbody.innerHTML += `
            <tr>
                <td><span class="badge bg-light text-dark border fw-bold">#${index + 1}</span></td>
                <td>
                    <span class="fs-6 fw-bold text-dark"><i class="bi bi-tag-fill me-2" style="color: var(--pool-amarillo);"></i>${cat.nombre}</span>
                </td>
                <td class="text-center"><span class="badge bg-light text-dark border">${cantProds} productos</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="eliminarCategoriaDirecto(${cat.id})" title="Eliminar">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

function poblarFiltroCategoriasProductos() {
    const selFiltro = document.getElementById('filtroCategoriaTabla');
    const selPos = document.getElementById('posFiltroCategoria');

    if (selFiltro) {
        selFiltro.innerHTML = '<option value="">Todas las Categorías</option>';
        listaCategoriasGlobal.forEach(c => {
            selFiltro.innerHTML += `<option value="${c.nombre}">${c.nombre}</option>`;
        });
    }

    if (selPos) {
        selPos.innerHTML = '<option value="">Todas las Categorías</option>';
        listaCategoriasGlobal.forEach(c => {
            selPos.innerHTML += `<option value="${c.nombre}">${c.nombre}</option>`;
        });
    }
}

async function crearCategoriaDirecto(e) {
    e.preventDefault();
    const nombre = document.getElementById('inputNombreCategoriaDirecto').value.trim();
    if (!nombre) return;

    try {
        const res = await fetch(`${API_BASE}/categorias`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ nombre })
        });

        if (res.ok) {
            mostrarToast('Categoría Creada', `Categoría "${nombre}" registrada con éxito.`, 'success');
            document.getElementById('inputNombreCategoriaDirecto').value = '';
            cargarCategorias();
        } else {
            const err = await res.json();
            mostrarToast('Error', err.mensaje || 'La categoría ya existe.', 'danger');
        }
    } catch (e) {
        mostrarToast('Error de Red', 'No se pudo crear la categoría', 'danger');
    }
}

async function eliminarCategoriaDirecto(id) {
    if (!confirm('¿Desea eliminar esta categoría?')) return;

    try {
        const res = await fetch(`${API_BASE}/categorias/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.ok) {
            mostrarToast('Categoría Eliminada', 'Se eliminó la categoría correctamente.', 'info');
            cargarCategorias();
        } else {
            const err = await res.json();
            mostrarToast('No se puede eliminar', err.mensaje || 'Tiene productos asociados.', 'danger');
        }
    } catch (e) {
        mostrarToast('Error', 'Error de red', 'danger');
    }
}

// ==============================================================================
// 3. PROVEEDORES (VISTA DEDICADA COMPLETA)
// ==============================================================================

async function cargarProveedores() {
    try {
        const res = await fetch(`${API_BASE}/proveedores`, { headers: getAuthHeaders() });
        if (res.ok) {
            listaProveedoresGlobal = await res.json();
            renderizarVistaProveedores();
        }
    } catch (e) {}
}

function renderizarVistaProveedores() {
    const tbody = document.querySelector('#tablaProveedoresAdmin tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (listaProveedoresGlobal.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No hay proveedores registrados.</td></tr>';
        return;
    }

    listaProveedoresGlobal.forEach((prov, index) => {
        const cantProds = listaProductosGlobal.filter(p => p.proveedorId === prov.id || p.proveedorNombre === prov.nombre).length;

        tbody.innerHTML += `
            <tr>
                <td><span class="badge bg-light text-dark border fw-bold">#${index + 1}</span></td>
                <td>
                    <strong><i class="bi bi-truck me-2 text-danger"></i>${prov.nombre}</strong>
                </td>
                <td><code>${prov.ruc}</code></td>
                <td>${prov.contacto || '<span class="text-muted">No especificado</span>'}</td>
                <td class="text-center"><span class="badge bg-light text-dark border">${cantProds} suministrados</span></td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="eliminarProveedorDirecto(${prov.id})" title="Eliminar">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });
}

async function crearProveedorDirecto(e) {
    e.preventDefault();
    const nombre = document.getElementById('inputProvNombreDirecto').value.trim();
    const ruc = document.getElementById('inputProvRucDirecto').value.trim();
    const contacto = document.getElementById('inputProvContactoDirecto').value.trim();

    if (!nombre || !ruc) return;

    try {
        const res = await fetch(`${API_BASE}/proveedores`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ nombre, ruc, contacto })
        });

        if (res.ok) {
            mostrarToast('Proveedor Registrado', `Proveedor ${nombre} registrado exitosamente.`, 'success');
            document.getElementById('inputProvNombreDirecto').value = '';
            document.getElementById('inputProvRucDirecto').value = '';
            document.getElementById('inputProvContactoDirecto').value = '';
            cargarProveedores();
        } else {
            const err = await res.json();
            mostrarToast('Error', err.mensaje || 'El RUC ya se encuentra registrado.', 'danger');
        }
    } catch (e) {
        mostrarToast('Error de Red', 'No se pudo guardar el proveedor', 'danger');
    }
}

async function eliminarProveedorDirecto(id) {
    if (!confirm('¿Desea eliminar este proveedor?')) return;

    try {
        const res = await fetch(`${API_BASE}/proveedores/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.ok) {
            mostrarToast('Proveedor Eliminado', 'Se eliminó el proveedor.', 'info');
            cargarProveedores();
        } else {
            const err = await res.json();
            mostrarToast('No se puede eliminar', err.mensaje || 'Tiene productos vinculados.', 'danger');
        }
    } catch (e) {
        mostrarToast('Error', 'Error de red', 'danger');
    }
}
