-- Base de datos PostgreSQL para Supabase
-- Minimarket Pool

DROP TABLE IF EXISTS detalles_compra CASCADE;
DROP TABLE IF EXISTS compras CASCADE;
DROP TABLE IF EXISTS detalles_venta CASCADE;
DROP TABLE IF EXISTS ventas CASCADE;
DROP TABLE IF EXISTS turnos_caja CASCADE;
DROP TABLE IF EXISTS movimientos_inventario CASCADE;
DROP TABLE IF EXISTS lotes_inventario CASCADE;
DROP TABLE IF EXISTS productos CASCADE;
DROP TABLE IF EXISTS proveedores CASCADE;
DROP TABLE IF EXISTS categorias_producto CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR', 'CLIENTE'))
);

CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(10) NOT NULL DEFAULT 'DNI' CHECK (tipo_documento IN ('DNI', 'RUC', 'CE')),
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120),
    email VARCHAR(150),
    telefono VARCHAR(25),
    direccion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE TABLE categorias_producto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(20) NOT NULL UNIQUE,
    contacto VARCHAR(150)
);

CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    precio_caja NUMERIC(10, 2),
    unidades_por_caja INT DEFAULT 1,
    stock_minimo INT NOT NULL DEFAULT 5,
    imagen_url TEXT,
    categoria_id BIGINT REFERENCES categorias_producto(id) ON DELETE SET NULL,
    proveedor_id BIGINT REFERENCES proveedores(id) ON DELETE SET NULL
);

CREATE TABLE lotes_inventario (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    cantidad INT NOT NULL CHECK (cantidad >= 0),
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE NOT NULL
);

CREATE TABLE movimientos_inventario (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    lote_id BIGINT REFERENCES lotes_inventario(id) ON DELETE SET NULL,
    usuario_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    tipo_movimiento VARCHAR(20) NOT NULL CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'MERMA')),
    cantidad INT NOT NULL,
    motivo VARCHAR(255),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE turnos_caja (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    monto_apertura NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_ventas_efectivo NUMERIC(12, 2) DEFAULT 0.00,
    total_ventas_digital NUMERIC(12, 2) DEFAULT 0.00,
    total_ventas_tarjeta NUMERIC(12, 2) DEFAULT 0.00,
    total_vendido NUMERIC(12, 2) DEFAULT 0.00,
    monto_cierre_esperado NUMERIC(12, 2),
    monto_cierre_real NUMERIC(12, 2),
    diferencia NUMERIC(12, 2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA')),
    observaciones TEXT
);

CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    cliente_id BIGINT REFERENCES clientes(id) ON DELETE SET NULL,
    turno_caja_id BIGINT REFERENCES turnos_caja(id) ON DELETE SET NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    metodo_pago VARCHAR(25) NOT NULL DEFAULT 'EFECTIVO' CHECK (metodo_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA'))
);

CREATE TABLE detalles_venta (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL CHECK (precio_unitario >= 0)
);

CREATE TABLE compras (
    id BIGSERIAL PRIMARY KEY,
    numero_comprobante VARCHAR(50) NOT NULL UNIQUE,
    tipo_comprobante VARCHAR(30) NOT NULL,
    proveedor_id BIGINT NOT NULL REFERENCES proveedores(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    observaciones TEXT
);

CREATE TABLE detalles_compra (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(12, 2) NOT NULL CHECK (costo_unitario >= 0),
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    fecha_vencimiento DATE NOT NULL,
    lote_id BIGINT REFERENCES lotes_inventario(id) ON DELETE SET NULL
);

CREATE INDEX idx_lotes_fefo ON lotes_inventario (producto_id, fecha_vencimiento ASC);
CREATE INDEX idx_productos_sku ON productos (sku);
CREATE INDEX idx_productos_nombre ON productos (LOWER(nombre));
CREATE INDEX idx_clientes_doc ON clientes (numero_documento);
CREATE INDEX idx_clientes_email ON clientes (email);
CREATE INDEX idx_clientes_usuario ON clientes (usuario_id);
CREATE INDEX idx_ventas_fecha ON ventas (fecha DESC);
CREATE INDEX idx_ventas_turno ON ventas (turno_caja_id);
CREATE INDEX idx_movimientos_fecha ON movimientos_inventario (fecha DESC);
CREATE INDEX idx_turnos_usuario ON turnos_caja (usuario_id, estado);
CREATE INDEX idx_turnos_fecha ON turnos_caja (fecha_apertura DESC);
CREATE INDEX idx_compras_fecha ON compras (fecha DESC);
CREATE INDEX idx_compras_proveedor ON compras (proveedor_id);

-- Usuarios de acceso inicial (password: 123456)
INSERT INTO usuarios (nombre, email, password, rol) 
VALUES 
('Administrador', 'admin@minimarket.com', '$2a$10$wK1F5H8K5mK6Yw7G1YvB7eqK4GZp3c.VpZb7vU3mQ1fW9nZ1oG2eW', 'ADMIN'),
('Cajero', 'vendedor@minimarket.com', '$2a$10$wK1F5H8K5mK6Yw7G1YvB7eqK4GZp3c.VpZb7vU3mQ1fW9nZ1oG2eW', 'VENDEDOR')
ON CONFLICT (email) DO NOTHING;
