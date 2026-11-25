-- ==============================================================================
-- BASE DE DATOS POSTGRESQL — SUPABASE (PROYECTO: MINIMARKET POOL)
-- SCRIPT COMPLETO DDL + DATOS DE PRUEBA (100 PRODUCTOS + LOTES + USUARIOS)
-- ==============================================================================

-- 1. LIMPIEZA INICIAL EN ORDEN RELACIONAL
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

-- 2. TABLA: USUARIOS (ADMIN, VENDEDOR, CLIENTE)
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR', 'CLIENTE'))
);

-- 3. TABLA: CLIENTES
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

-- 4. TABLA: CATEGORIAS_PRODUCTO
CREATE TABLE categorias_producto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- 5. TABLA: PROVEEDORES
CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(20) NOT NULL UNIQUE,
    contacto VARCHAR(150)
);

-- 6. TABLA: PRODUCTOS
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

-- 7. TABLA: LOTES_INVENTARIO (CONTROL FEFO)
CREATE TABLE lotes_inventario (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    cantidad INT NOT NULL CHECK (cantidad >= 0),
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento DATE NOT NULL
);

-- 8. TABLA: MOVIMIENTOS_INVENTARIO (KARDEX)
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

-- 9. TABLA: TURNOS_CAJA
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

-- 10. TABLA: VENTAS
CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    cliente_id BIGINT REFERENCES clientes(id) ON DELETE SET NULL,
    turno_caja_id BIGINT REFERENCES turnos_caja(id) ON DELETE SET NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    metodo_pago VARCHAR(25) NOT NULL DEFAULT 'EFECTIVO' CHECK (metodo_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA'))
);

-- 11. TABLA: DETALLES_VENTA
CREATE TABLE detalles_venta (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL CHECK (precio_unitario >= 0)
);

-- 12. TABLA: COMPRAS
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

-- 13. TABLA: DETALLES_COMPRA
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

-- 14. ÍNDICES DE RENDIMIENTO
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

-- ==============================================================================
-- 15. SEED DATA: USUARIOS INICIALES
-- Contraseña compatible con '123456' y 'admin123' / 'cajero123'
-- Hash BCrypt para '123456': $2a$10$wK1F5H8K5mK6Yw7G1YvB7eqK4GZp3c.VpZb7vU3mQ1fW9nZ1oG2eW
-- ==============================================================================
INSERT INTO usuarios (nombre, email, password, rol) VALUES
('Administrador Principal', 'admin@minimarket.com', '$2a$10$wK1F5H8K5mK6Yw7G1YvB7eqK4GZp3c.VpZb7vU3mQ1fW9nZ1oG2eW', 'ADMIN'),
('Cajero / Vendedor', 'vendedor@minimarket.com', '$2a$10$wK1F5H8K5mK6Yw7G1YvB7eqK4GZp3c.VpZb7vU3mQ1fW9nZ1oG2eW', 'VENDEDOR')
ON CONFLICT (email) DO NOTHING;

-- 16. SEED DATA: CATEGORÍAS
INSERT INTO categorias_producto (nombre) VALUES
('Lácteos'),
('Bebidas'),
('Abarrotes'),
('Snacks'),
('Limpieza'),
('Cuidado Personal'),
('Golosinas'),
('Panadería'),
('Conservas'),
('Embutidos')
ON CONFLICT (nombre) DO NOTHING;

-- 17. SEED DATA: PROVEEDORES
INSERT INTO proveedores (nombre, ruc, contacto) VALUES
('Leche Gloria S.A.', '20100190797', 'contacto@gloria.com.pe'),
('Arca Continental Lindley S.A.', '20102917716', 'ventas@lindley.pe'),
('Alicorp S.A.A.', '20100055237', 'pedidos@alicorp.com.pe'),
('Costeño Alimentos S.A.C.', '20100345685', 'ventas@costeno.pe'),
('San Fernando S.A.', '20100154308', 'atencion@san-fernando.com.pe'),
('Nestlé Perú S.A.', '20100177260', 'corporativo@nestle.com.pe'),
('Procter & Gamble Perú S.R.L.', '20100152941', 'ventas@pg.com'),
('Kimberly-Clark Perú S.R.L.', '20297869389', 'contacto@kcc.com'),
('Molitalia S.A.', '20100035121', 'pedidos@molitalia.com.pe'),
('Backus y Johnston S.A.A.', '20100113610', 'distribucion@backus.pe'),
('Mondelez Perú S.A.', '20100141672', 'servicio@mondelez.com')
ON CONFLICT (ruc) DO NOTHING;

-- 18. SEED DATA: 100 PRODUCTOS MAESTROS
INSERT INTO productos (sku, nombre, precio_unitario, precio_caja, unidades_por_caja, stock_minimo, imagen_url, categoria_id, proveedor_id) VALUES
('7750151000101', 'Leche Gloria Entera 400g', 4.20, 96.00, 24, 15, 'https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000102', 'Leche Gloria Sin Lactosa 400g', 4.50, 102.00, 24, 15, 'https://images.unsplash.com/photo-1563636619-e9143da7973b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000103', 'Leche Gloria Light 400g', 4.30, 98.00, 24, 15, 'https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000104', 'Yogurt Gloria Fresa 1L', 6.50, 72.00, 12, 10, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000105', 'Yogurt Gloria Vainilla Francesa 1L', 6.50, 72.00, 12, 10, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000106', 'Yogurt Gloria Durazno 1L', 6.50, 72.00, 12, 10, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000107', 'Queso Fresco Laive 500g', 14.50, 140.00, 10, 8, 'https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000108', 'Mantequilla Gloria con Sal 200g', 8.20, 92.00, 12, 10, 'https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Leche Gloria S.A.')),
('7750151000109', 'Huevos Pardos La Calera x15', 10.50, 120.00, 12, 10, 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'San Fernando S.A.')),
('7750151000110', 'Crema de Leche Nestlé 295g', 6.80, 78.00, 12, 8, 'https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Lácteos'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750243000201', 'Inca Kola 500ml Retornable', 2.50, 28.00, 12, 20, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000202', 'Inca Kola 1.5L Sin Azúcar', 6.20, 36.00, 6, 12, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000203', 'Inca Kola 3L Mega', 11.50, 44.00, 4, 10, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000204', 'Coca Cola 500ml Original', 2.80, 30.00, 12, 20, 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000205', 'Coca Cola 1.5L Original', 6.50, 38.00, 6, 12, 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000206', 'Coca Cola 3L Original', 12.00, 46.00, 4, 10, 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000207', 'Agua San Mateo con Gas 600ml', 2.00, 22.00, 12, 15, 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Backus y Johnston S.A.A.')),
('7750243000208', 'Agua Cielo sin Gas 625ml', 1.50, 16.00, 12, 20, 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000209', 'Agua San Luis Bidón 7L', 8.50, 16.00, 2, 8, 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000210', 'Jugo Frugos del Valle Durazno 1L', 4.80, 54.00, 12, 10, 'https://images.unsplash.com/photo-1613478223719-2ab802602423?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000211', 'Bebida Energizante Monster Energy 473ml', 8.90, 98.00, 12, 10, 'https://images.unsplash.com/photo-1622543925917-763c34d1a86e?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750243000212', 'Cifrut Granadilla 3L', 5.50, 21.00, 4, 8, 'https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Bebidas'), (SELECT id FROM proveedores WHERE nombre = 'Arca Continental Lindley S.A.')),
('7750335000301', 'Arroz Costeño Extra 1kg', 4.80, 45.00, 10, 15, 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750335000302', 'Arroz Faraón Extra 750g', 4.20, 39.00, 10, 10, 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750335000303', 'Azúcar Rubia Cartavio 1kg', 4.20, 40.00, 10, 15, 'https://images.unsplash.com/photo-1581441363689-1f3c3c414635?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750335000304', 'Azúcar Blanca Dulfina 1kg', 4.60, 44.00, 10, 12, 'https://images.unsplash.com/photo-1581441363689-1f3c3c414635?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750335000305', 'Aceite Primor Clásico 900ml', 9.50, 108.00, 12, 10, 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000306', 'Aceite Cocinero Vegetal 900ml', 8.20, 94.00, 12, 10, 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000307', 'Aceite de Oliva Olivos del Sur 250ml', 16.50, 95.00, 6, 5, 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000308', 'Fideos Don Vittorio Spaghetti 450g', 2.80, 52.00, 20, 15, 'https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000309', 'Fideos Don Vittorio Tornillo 450g', 2.80, 52.00, 20, 15, 'https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000310', 'Fideos Molitalia Canuto 450g', 2.90, 54.00, 20, 12, 'https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750335000311', 'Avena 3 Ositos Tradicional 300g', 3.20, 72.00, 24, 10, 'https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750335000312', 'Sal de Mesa Marina Emsal 1kg', 1.80, 32.00, 20, 15, 'https://images.unsplash.com/photo-1518110925495-5fe2fda0442c?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750335000313', 'Vinagre Blanco Menoyo 500ml', 2.40, 26.00, 12, 8, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000314', 'Mayonesa Alacena Doypack 475g', 8.90, 98.00, 12, 10, 'https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000315', 'Crema de Ají Tarí Alacena 400g', 8.50, 94.00, 12, 10, 'https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750335000316', 'Salsa de Tomate Pomarola Molitalia 150g', 2.20, 48.00, 24, 12, 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Abarrotes'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750559000401', 'Papas Lays Clásicas 160g', 6.50, 72.00, 12, 12, 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000402', 'Papas Lays Onduladas 150g', 6.50, 72.00, 12, 10, 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000403', 'Doritos Mega Queso 155g', 6.20, 70.00, 12, 12, 'https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000404', 'Chizitos Fiesta 150g', 4.50, 50.00, 12, 10, 'https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000405', 'Cuates Picantes 150g', 3.80, 42.00, 12, 10, 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000406', 'Tortees Picantes 160g', 4.00, 44.00, 12, 10, 'https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000407', 'Chifles Piuranos Salados 100g', 3.50, 38.00, 12, 10, 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750559000408', 'Maní Salado Karinto 100g', 3.20, 36.00, 12, 10, 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000409', 'Galletas Casino Fresa 6pk', 1.20, 26.00, 24, 15, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000410', 'Galletas Casino Menta 6pk', 1.20, 26.00, 24, 15, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750559000411', 'Galletas Oreo Original 6pk', 2.50, 54.00, 24, 15, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Mondelez Perú S.A.')),
('7750559000412', 'Galletas Soda San Jorge 6pk', 2.20, 48.00, 24, 15, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Snacks'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750667000501', 'Chocolate Sublime Clásico 30g', 2.00, 44.00, 24, 20, 'https://images.unsplash.com/photo-1511381939415-e44015466834?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750667000502', 'Chocolate Sublime Almendras 30g', 2.20, 48.00, 24, 15, 'https://images.unsplash.com/photo-1511381939415-e44015466834?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750667000503', 'Chocolate Princesa 30g', 2.20, 48.00, 24, 15, 'https://images.unsplash.com/photo-1511381939415-e44015466834?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750667000504', 'Chocolate Triángulo D''Onofrio 30g', 2.00, 44.00, 24, 15, 'https://images.unsplash.com/photo-1511381939415-e44015466834?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750667000505', 'Caramelo Halls Mentol 34g', 1.50, 28.00, 20, 15, 'https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Mondelez Perú S.A.')),
('7750667000506', 'Chupetín Bon o Bon 18g', 1.20, 26.00, 24, 15, 'https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750667000507', 'Gomitas Ambrosoli Ositos 100g', 3.00, 32.00, 12, 10, 'https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750667000508', 'Wafer Nick Vainilla 6pk', 1.80, 38.00, 24, 12, 'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750667000509', 'Lentejitas Chin Chin 30g', 1.50, 32.00, 24, 15, 'https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750667000510', 'Marshmallows Millows 150g', 4.50, 48.00, 12, 8, 'https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Golosinas'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750778000601', 'Pan de Molde Bimbo Blanco 480g', 7.20, 68.00, 10, 8, 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000602', 'Pan de Molde Bimbo Integral 480g', 7.80, 74.00, 10, 8, 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000603', 'Tostadas Clásicas Bimbo 210g', 5.50, 52.00, 10, 8, 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000604', 'Pan Hamburguesa Bimbo x4', 4.80, 44.00, 10, 6, 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000605', 'Mermelada Fanny Fresa Pote 310g', 4.90, 54.00, 12, 10, 'https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750778000606', 'Café Nescafé Tradición 50g', 6.50, 72.00, 12, 10, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750778000607', 'Café Altomayo Gourmet 50g', 7.80, 86.00, 12, 8, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750778000608', 'Té Herbi Canela y Clavo x25', 3.50, 38.00, 12, 10, 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000609', 'Manzanilla Herbi Filtro x25', 3.50, 38.00, 12, 10, 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750778000610', 'Cocoa D''Onofrio 160g', 4.80, 52.00, 12, 8, 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Panadería'), (SELECT id FROM proveedores WHERE nombre = 'Nestlé Perú S.A.')),
('7750889000701', 'Detergente Bolívar Floral 800g', 7.90, 88.00, 12, 12, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000702', 'Detergente Ariel Doble Poder 850g', 8.90, 98.00, 12, 12, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750889000703', 'Suavizante Downy Floral 800ml', 9.50, 104.00, 12, 10, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750889000704', 'Lejía Clorox Tradicional 1L', 3.80, 42.00, 12, 15, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000705', 'Lavavajilla Sapolio Limón Pasta 350g', 3.20, 36.00, 12, 15, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000706', 'Lavavajilla Ayudín Limón Líquido 500ml', 5.80, 64.00, 12, 12, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750889000707', 'Limpiador Líquido Poett Lavanda 900ml', 4.20, 46.00, 12, 12, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000708', 'Limpiador Pinesol Original 900ml', 5.50, 60.00, 12, 10, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000709', 'Esponja Scotch Brite Verde 3M', 2.50, 26.00, 12, 20, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750889000710', 'Bolsas de Basura San Remo 50L x10', 4.80, 52.00, 12, 15, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000711', 'Paños Absorbentes Virutex x3', 5.20, 56.00, 12, 10, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750889000712', 'Aromatizador Glade Lavanda Aerosol 360ml', 8.50, 94.00, 12, 8, 'https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Limpieza'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000801', 'Jabón de Tocador Dove Blanco 90g', 4.20, 46.00, 12, 15, 'https://images.unsplash.com/photo-1608248597359-07b1a8f6dff4?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000802', 'Jabón Rexona Antibacterial 90g', 3.50, 38.00, 12, 15, 'https://images.unsplash.com/photo-1608248597359-07b1a8f6dff4?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000803', 'Shampoo Head & Shoulders Limpieza 375ml', 18.50, 105.00, 6, 8, 'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000804', 'Shampoo Pantene Restauración 375ml', 17.90, 102.00, 6, 8, 'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000805', 'Crema Dental Colgate Total 12 75ml', 6.50, 72.00, 12, 15, 'https://images.unsplash.com/photo-1559671261-26c6d05f3192?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000806', 'Crema Dental Kolynos Triple 75ml', 3.80, 42.00, 12, 15, 'https://images.unsplash.com/photo-1559671261-26c6d05f3192?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000807', 'Papel Higiénico Suave Rindemax x4', 5.50, 60.00, 12, 15, 'https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Kimberly-Clark Perú S.R.L.')),
('7750991000808', 'Papel Higiénico Elite Doble Hoja x4', 6.20, 68.00, 12, 15, 'https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Kimberly-Clark Perú S.R.L.')),
('7750991000809', 'Desodorante Rexona Clinical Men 48g', 16.50, 94.00, 6, 6, 'https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Procter & Gamble Perú S.R.L.')),
('7750991000810', 'Toallas Femeninas Nosotras Noche x8', 6.80, 74.00, 12, 10, 'https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Cuidado Personal'), (SELECT id FROM proveedores WHERE nombre = 'Kimberly-Clark Perú S.R.L.')),
('7750112000901', 'Filete de Atún Campomar en Aceite 170g', 6.20, 140.00, 24, 15, 'https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750112000902', 'Filete de Atún Primor 170g', 6.50, 148.00, 24, 15, 'https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Alicorp S.A.A.')),
('7750112000903', 'Trozos de Atún Fanny Agua y Sal 170g', 4.80, 110.00, 24, 12, 'https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Molitalia S.A.')),
('7750112000904', 'Grated de Pescado A-1 170g', 3.80, 86.00, 24, 12, 'https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750112000905', 'Duraznos en Almíbar A-1 820g', 9.80, 112.00, 12, 8, 'https://images.unsplash.com/photo-1595231712325-9df03d32fa88?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750112000906', 'Maíz Dulce en Grano Bonduelle 300g', 4.50, 50.00, 12, 8, 'https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750112000907', 'Arvejas con Zanahoria A-1 300g', 3.90, 44.00, 12, 8, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Conservas'), (SELECT id FROM proveedores WHERE nombre = 'Costeño Alimentos S.A.C.')),
('7750112000908', 'Jamonada Especial San Fernando 250g', 7.50, 85.00, 12, 10, 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=300', (SELECT id FROM categorias_producto WHERE nombre = 'Embutidos'), (SELECT id FROM proveedores WHERE nombre = 'San Fernando S.A.'))
ON CONFLICT (sku) DO NOTHING;

-- 19. SEED DATA: LOTES DE INVENTARIO INICIALES (STOCK DISPONIBLE PARA CADA PRODUCTO)
INSERT INTO lotes_inventario (producto_id, cantidad, fecha_ingreso, fecha_vencimiento) VALUES
((SELECT id FROM productos WHERE sku = '7750151000101'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750151000102'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '7 month'),
((SELECT id FROM productos WHERE sku = '7750151000103'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750151000104'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '4 month'),
((SELECT id FROM productos WHERE sku = '7750151000105'), 25, CURRENT_DATE, CURRENT_DATE + INTERVAL '4 month'),
((SELECT id FROM productos WHERE sku = '7750151000106'), 28, CURRENT_DATE, CURRENT_DATE + INTERVAL '4 month'),
((SELECT id FROM productos WHERE sku = '7750151000107'), 20, CURRENT_DATE, CURRENT_DATE + INTERVAL '2 month'),
((SELECT id FROM productos WHERE sku = '7750151000108'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750151000109'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month'),
((SELECT id FROM productos WHERE sku = '7750151000110'), 32, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750243000201'), 80, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000202'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000203'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000204'), 90, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000205'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000206'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750243000207'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750243000208'), 80, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750243000209'), 25, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750243000210'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750243000211'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750243000212'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750335000301'), 70, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750335000302'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750335000303'), 65, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000304'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000305'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750335000306'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750335000307'), 20, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000308'), 75, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000309'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000310'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000311'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750335000312'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750335000313'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750335000314'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750335000315'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750335000316'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750559000401'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000402'), 38, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000403'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000404'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000405'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000406'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000407'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000408'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 month'),
((SELECT id FROM productos WHERE sku = '7750559000409'), 70, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750559000410'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750559000411'), 75, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750559000412'), 65, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000501'), 80, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000502'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000503'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000504'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000505'), 70, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750667000506'), 80, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750667000507'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000508'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '8 month'),
((SELECT id FROM productos WHERE sku = '7750667000509'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750667000510'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '10 month'),
((SELECT id FROM productos WHERE sku = '7750778000601'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month'),
((SELECT id FROM productos WHERE sku = '7750778000602'), 25, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month'),
((SELECT id FROM productos WHERE sku = '7750778000603'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '4 month'),
((SELECT id FROM productos WHERE sku = '7750778000604'), 24, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month'),
((SELECT id FROM productos WHERE sku = '7750778000605'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750778000606'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750778000607'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750778000608'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750778000609'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750778000610'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '12 month'),
((SELECT id FROM productos WHERE sku = '7750889000701'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000702'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000703'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000704'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000705'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000706'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000707'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000708'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750889000709'), 70, CURRENT_DATE, CURRENT_DATE + INTERVAL '36 month'),
((SELECT id FROM productos WHERE sku = '7750889000710'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '36 month'),
((SELECT id FROM productos WHERE sku = '7750889000711'), 38, CURRENT_DATE, CURRENT_DATE + INTERVAL '36 month'),
((SELECT id FROM productos WHERE sku = '7750889000712'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000801'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000802'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000803'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000804'), 28, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000805'), 50, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750991000806'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '18 month'),
((SELECT id FROM productos WHERE sku = '7750991000807'), 70, CURRENT_DATE, CURRENT_DATE + INTERVAL '36 month'),
((SELECT id FROM productos WHERE sku = '7750991000808'), 65, CURRENT_DATE, CURRENT_DATE + INTERVAL '36 month'),
((SELECT id FROM productos WHERE sku = '7750991000809'), 25, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750991000810'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000901'), 60, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000902'), 55, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000903'), 45, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000904'), 40, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000905'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000906'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000907'), 35, CURRENT_DATE, CURRENT_DATE + INTERVAL '24 month'),
((SELECT id FROM productos WHERE sku = '7750112000908'), 30, CURRENT_DATE, CURRENT_DATE + INTERVAL '2 month');

-- FIN DEL SCRIPT SUPABASE
SELECT 'Base de datos Minimarket Pool inicializada con éxito. Total productos: ' || COUNT(*) FROM productos;
