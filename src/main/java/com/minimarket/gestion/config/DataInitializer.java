package com.minimarket.gestion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    record ProdSeed(
            String sku,
            String nombre,
            double precioUnitario,
            double precioCaja,
            int unidadesCaja,
            int stockMinimo,
            String imagenUrl,
            String categoria,
            String proveedor,
            int stockInicial,
            int mesesVenc
    ) {}

    @Override
    public void run(String... args) {
        try {
            log.info("Verificando compatibilidad de roles en tabla usuarios...");
            try {
                jdbcTemplate.execute("ALTER TABLE usuarios ALTER COLUMN rol TYPE VARCHAR(20)");
            } catch (Exception ex) {
                jdbcTemplate.execute("ALTER TABLE usuarios ALTER COLUMN rol VARCHAR(20) NOT NULL");
            }
            log.info("Columna 'usuarios.rol' adaptada exitosamente a VARCHAR(20).");
        } catch (Exception e) {
            log.warn("Nota al adaptar columna usuarios.rol: {}", e.getMessage());
        }

        try {
            inicializarCatalogo100Productos();
        } catch (Exception e) {
            log.error("Error al inicializar catálogo de 100 productos: {}", e.getMessage(), e);
        }

        try {
            inicializarUsuariosPorDefecto();
        } catch (Exception e) {
            log.warn("Nota al inicializar usuarios por defecto: {}", e.getMessage());
        }

        log.info("Sistema iniciado correctamente.");
    }

    private void inicializarUsuariosPorDefecto() {
        Integer countAdmin = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE email = 'admin@minimarket.com'", Integer.class);
        if (countAdmin == null || countAdmin == 0) {
            jdbcTemplate.update(
                    "INSERT INTO usuarios (nombre, email, password, rol) VALUES (?, ?, ?, ?)",
                    "Administrador Principal", "admin@minimarket.com", passwordEncoder.encode("admin123"), "ADMIN"
            );
            log.info("Usuario inicial admin@minimarket.com creado exitosamente.");
        }

        Integer countCajero = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE email = 'vendedor@minimarket.com'", Integer.class);
        if (countCajero == null || countCajero == 0) {
            jdbcTemplate.update(
                    "INSERT INTO usuarios (nombre, email, password, rol) VALUES (?, ?, ?, ?)",
                    "Cajero / Vendedor", "vendedor@minimarket.com", passwordEncoder.encode("cajero123"), "VENDEDOR"
            );
            log.info("Usuario inicial vendedor@minimarket.com creado exitosamente.");
        }
    }

    private void inicializarCatalogo100Productos() {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM productos", Integer.class);
        if (total != null && total >= 90) {
            log.info("El catálogo ya cuenta con {} productos. No se requiere repoblar.", total);
            return;
        }

        log.info("Inicializando catálogo maestro con 100 productos variados para Minimarket Pool...");

        // 1. Garantizar Categorías
        String[] categorias = {
                "Lácteos", "Bebidas", "Abarrotes", "Snacks", "Limpieza",
                "Cuidado Personal", "Golosinas", "Panadería", "Conservas", "Embutidos"
        };
        for (String cat : categorias) {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM categorias_producto WHERE nombre = ?", Integer.class, cat);
            if (existe == null || existe == 0) {
                jdbcTemplate.update("INSERT INTO categorias_producto (nombre) VALUES (?)", cat);
            }
        }

        // 2. Garantizar Proveedores
        record ProvInfo(String nombre, String ruc, String contacto) {}
        List<ProvInfo> proveedores = List.of(
                new ProvInfo("Leche Gloria S.A.", "20100190797", "contacto@gloria.com.pe"),
                new ProvInfo("Arca Continental Lindley S.A.", "20102917716", "ventas@lindley.pe"),
                new ProvInfo("Alicorp S.A.A.", "20100055237", "pedidos@alicorp.com.pe"),
                new ProvInfo("Costeño Alimentos S.A.C.", "20100345685", "ventas@costeno.pe"),
                new ProvInfo("San Fernando S.A.", "20100154308", "atencion@san-fernando.com.pe"),
                new ProvInfo("Nestlé Perú S.A.", "20100177260", "corporativo@nestle.com.pe"),
                new ProvInfo("Procter & Gamble Perú S.R.L.", "20100152941", "ventas@pg.com"),
                new ProvInfo("Kimberly-Clark Perú S.R.L.", "20297869389", "contacto@kcc.com"),
                new ProvInfo("Molitalia S.A.", "20100035121", "pedidos@molitalia.com.pe"),
                new ProvInfo("Backus y Johnston S.A.A.", "20100113610", "distribucion@backus.pe"),
                new ProvInfo("Mondelez Perú S.A.", "20100141672", "servicio@mondelez.com")
        );
        for (ProvInfo pr : proveedores) {
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM proveedores WHERE ruc = ?", Integer.class, pr.ruc);
            if (existe == null || existe == 0) {
                jdbcTemplate.update("INSERT INTO proveedores (nombre, ruc, contacto) VALUES (?, ?, ?)",
                        pr.nombre, pr.ruc, pr.contacto);
            }
        }

        // 3. Catálogo de 100 Productos
        List<ProdSeed> catalogo = List.of(
                // 1-10: Lácteos
                new ProdSeed("7750151000101", "Leche Gloria Entera 400g", 4.20, 96.00, 24, 15, "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300", "Lácteos", "Leche Gloria S.A.", 60, 8),
                new ProdSeed("7750151000102", "Leche Gloria Sin Lactosa 400g", 4.50, 102.00, 24, 15, "https://images.unsplash.com/photo-1563636619-e9143da7973b?w=300", "Lácteos", "Leche Gloria S.A.", 45, 7),
                new ProdSeed("7750151000103", "Leche Gloria Light 400g", 4.30, 98.00, 24, 15, "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300", "Lácteos", "Leche Gloria S.A.", 40, 8),
                new ProdSeed("7750151000104", "Yogurt Gloria Fresa 1L", 6.50, 72.00, 12, 10, "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300", "Lácteos", "Leche Gloria S.A.", 30, 4),
                new ProdSeed("7750151000105", "Yogurt Gloria Vainilla Francesa 1L", 6.50, 72.00, 12, 10, "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300", "Lácteos", "Leche Gloria S.A.", 25, 4),
                new ProdSeed("7750151000106", "Yogurt Gloria Durazno 1L", 6.50, 72.00, 12, 10, "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300", "Lácteos", "Leche Gloria S.A.", 28, 4),
                new ProdSeed("7750151000107", "Queso Fresco Laive 500g", 14.50, 140.00, 10, 8, "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=300", "Lácteos", "Leche Gloria S.A.", 20, 2),
                new ProdSeed("7750151000108", "Mantequilla Gloria con Sal 200g", 8.20, 92.00, 12, 10, "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=300", "Lácteos", "Leche Gloria S.A.", 35, 6),
                new ProdSeed("7750151000109", "Huevos Pardos La Calera x15", 10.50, 120.00, 12, 10, "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=300", "Lácteos", "San Fernando S.A.", 50, 1),
                new ProdSeed("7750151000110", "Crema de Leche Nestlé 295g", 6.80, 78.00, 12, 8, "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=300", "Lácteos", "Nestlé Perú S.A.", 32, 6),

                // 11-22: Bebidas
                new ProdSeed("7750243000201", "Inca Kola 500ml Retornable", 2.50, 28.00, 12, 20, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300", "Bebidas", "Arca Continental Lindley S.A.", 80, 10),
                new ProdSeed("7750243000202", "Inca Kola 1.5L Sin Azúcar", 6.20, 36.00, 6, 12, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300", "Bebidas", "Arca Continental Lindley S.A.", 45, 10),
                new ProdSeed("7750243000203", "Inca Kola 3L Mega", 11.50, 44.00, 4, 10, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=300", "Bebidas", "Arca Continental Lindley S.A.", 35, 10),
                new ProdSeed("7750243000204", "Coca Cola 500ml Original", 2.80, 30.00, 12, 20, "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300", "Bebidas", "Arca Continental Lindley S.A.", 90, 10),
                new ProdSeed("7750243000205", "Coca Cola 1.5L Original", 6.50, 38.00, 6, 12, "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300", "Bebidas", "Arca Continental Lindley S.A.", 50, 10),
                new ProdSeed("7750243000206", "Coca Cola 3L Original", 12.00, 46.00, 4, 10, "https://images.unsplash.com/photo-1554866585-cd94860890b7?w=300", "Bebidas", "Arca Continental Lindley S.A.", 40, 10),
                new ProdSeed("7750243000207", "Agua San Mateo con Gas 600ml", 2.00, 22.00, 12, 15, "https://images.unsplash.com/photo-1523362628745-0c100150b504?w=300", "Bebidas", "Backus y Johnston S.A.A.", 60, 12),
                new ProdSeed("7750243000208", "Agua Cielo sin Gas 625ml", 1.50, 16.00, 12, 20, "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=300", "Bebidas", "Arca Continental Lindley S.A.", 80, 12),
                new ProdSeed("7750243000209", "Agua San Luis Bidón 7L", 8.50, 16.00, 2, 8, "https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=300", "Bebidas", "Arca Continental Lindley S.A.", 25, 12),
                new ProdSeed("7750243000210", "Jugo Frugos del Valle Durazno 1L", 4.80, 54.00, 12, 10, "https://images.unsplash.com/photo-1613478223719-2ab802602423?w=300", "Bebidas", "Arca Continental Lindley S.A.", 40, 8),
                new ProdSeed("7750243000211", "Bebida Energizante Monster Energy 473ml", 8.90, 98.00, 12, 10, "https://images.unsplash.com/photo-1622543925917-763c34d1a86e?w=300", "Bebidas", "Arca Continental Lindley S.A.", 35, 12),
                new ProdSeed("7750243000212", "Cifrut Granadilla 3L", 5.50, 21.00, 4, 8, "https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=300", "Bebidas", "Arca Continental Lindley S.A.", 30, 8),

                // 23-38: Abarrotes
                new ProdSeed("7750335000301", "Arroz Costeño Extra 1kg", 4.80, 45.00, 10, 15, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300", "Abarrotes", "Costeño Alimentos S.A.C.", 70, 12),
                new ProdSeed("7750335000302", "Arroz Faraón Extra 750g", 4.20, 39.00, 10, 10, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300", "Abarrotes", "Costeño Alimentos S.A.C.", 50, 12),
                new ProdSeed("7750335000303", "Azúcar Rubia Cartavio 1kg", 4.20, 40.00, 10, 15, "https://images.unsplash.com/photo-1581441363689-1f3c3c414635?w=300", "Abarrotes", "Costeño Alimentos S.A.C.", 65, 18),
                new ProdSeed("7750335000304", "Azúcar Blanca Dulfina 1kg", 4.60, 44.00, 10, 12, "https://images.unsplash.com/photo-1581441363689-1f3c3c414635?w=300", "Abarrotes", "Costeño Alimentos S.A.C.", 45, 18),
                new ProdSeed("7750335000305", "Aceite Primor Clásico 900ml", 9.50, 108.00, 12, 10, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300", "Abarrotes", "Alicorp S.A.A.", 55, 12),
                new ProdSeed("7750335000306", "Aceite Cocinero Vegetal 900ml", 8.20, 94.00, 12, 10, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300", "Abarrotes", "Alicorp S.A.A.", 45, 12),
                new ProdSeed("7750335000307", "Aceite de Oliva Olivos del Sur 250ml", 16.50, 95.00, 6, 5, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=300", "Abarrotes", "Alicorp S.A.A.", 20, 18),
                new ProdSeed("7750335000308", "Fideos Don Vittorio Spaghetti 450g", 2.80, 52.00, 20, 15, "https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300", "Abarrotes", "Alicorp S.A.A.", 75, 18),
                new ProdSeed("7750335000309", "Fideos Don Vittorio Tornillo 450g", 2.80, 52.00, 20, 15, "https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300", "Abarrotes", "Alicorp S.A.A.", 60, 18),
                new ProdSeed("7750335000310", "Fideos Molitalia Canuto 450g", 2.90, 54.00, 20, 12, "https://images.unsplash.com/photo-1612927601601-6638404737ce?w=300", "Abarrotes", "Molitalia S.A.", 55, 18),
                new ProdSeed("7750335000311", "Avena 3 Ositos Tradicional 300g", 3.20, 72.00, 24, 10, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=300", "Abarrotes", "Molitalia S.A.", 45, 12),
                new ProdSeed("7750335000312", "Sal de Mesa Marina Emsal 1kg", 1.80, 32.00, 20, 15, "https://images.unsplash.com/photo-1518110925495-5fe2fda0442c?w=300", "Abarrotes", "Costeño Alimentos S.A.C.", 60, 24),
                new ProdSeed("7750335000313", "Vinagre Blanco Menoyo 500ml", 2.40, 26.00, 12, 8, "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=300", "Abarrotes", "Alicorp S.A.A.", 35, 18),
                new ProdSeed("7750335000314", "Mayonesa Alacena Doypack 475g", 8.90, 98.00, 12, 10, "https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=300", "Abarrotes", "Alicorp S.A.A.", 50, 6),
                new ProdSeed("7750335000315", "Crema de Ají Tarí Alacena 400g", 8.50, 94.00, 12, 10, "https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=300", "Abarrotes", "Alicorp S.A.A.", 40, 6),
                new ProdSeed("7750335000316", "Salsa de Tomate Pomarola Molitalia 150g", 2.20, 48.00, 24, 12, "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=300", "Abarrotes", "Molitalia S.A.", 50, 12),

                // 39-50: Snacks
                new ProdSeed("7750559000401", "Papas Lays Clásicas 160g", 6.50, 72.00, 12, 12, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300", "Snacks", "Alicorp S.A.A.", 45, 6),
                new ProdSeed("7750559000402", "Papas Lays Onduladas 150g", 6.50, 72.00, 12, 10, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300", "Snacks", "Alicorp S.A.A.", 38, 6),
                new ProdSeed("7750559000403", "Doritos Mega Queso 155g", 6.20, 70.00, 12, 12, "https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300", "Snacks", "Alicorp S.A.A.", 40, 6),
                new ProdSeed("7750559000404", "Chizitos Fiesta 150g", 4.50, 50.00, 12, 10, "https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300", "Snacks", "Alicorp S.A.A.", 35, 6),
                new ProdSeed("7750559000405", "Cuates Picantes 150g", 3.80, 42.00, 12, 10, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300", "Snacks", "Alicorp S.A.A.", 40, 6),
                new ProdSeed("7750559000406", "Tortees Picantes 160g", 4.00, 44.00, 12, 10, "https://images.unsplash.com/photo-1600952841320-db92ec4047ca?w=300", "Snacks", "Alicorp S.A.A.", 30, 6),
                new ProdSeed("7750559000407", "Chifles Piuranos Salados 100g", 3.50, 38.00, 12, 10, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300", "Snacks", "Costeño Alimentos S.A.C.", 35, 6),
                new ProdSeed("7750559000408", "Maní Salado Karinto 100g", 3.20, 36.00, 12, 10, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=300", "Snacks", "Alicorp S.A.A.", 40, 6),
                new ProdSeed("7750559000409", "Galletas Casino Fresa 6pk", 1.20, 26.00, 24, 15, "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300", "Snacks", "Alicorp S.A.A.", 70, 8),
                new ProdSeed("7750559000410", "Galletas Casino Menta 6pk", 1.20, 26.00, 24, 15, "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300", "Snacks", "Alicorp S.A.A.", 60, 8),
                new ProdSeed("7750559000411", "Galletas Oreo Original 6pk", 2.50, 54.00, 24, 15, "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300", "Snacks", "Mondelez Perú S.A.", 75, 8),
                new ProdSeed("7750559000412", "Galletas Soda San Jorge 6pk", 2.20, 48.00, 24, 15, "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300", "Snacks", "Molitalia S.A.", 65, 10),

                // 51-60: Golosinas
                new ProdSeed("7750667000501", "Chocolate Sublime Clásico 30g", 2.00, 44.00, 24, 20, "https://images.unsplash.com/photo-1511381939415-e44015466834?w=300", "Golosinas", "Nestlé Perú S.A.", 80, 10),
                new ProdSeed("7750667000502", "Chocolate Sublime Almendras 30g", 2.20, 48.00, 24, 15, "https://images.unsplash.com/photo-1511381939415-e44015466834?w=300", "Golosinas", "Nestlé Perú S.A.", 60, 10),
                new ProdSeed("7750667000503", "Chocolate Princesa 30g", 2.20, 48.00, 24, 15, "https://images.unsplash.com/photo-1511381939415-e44015466834?w=300", "Golosinas", "Nestlé Perú S.A.", 55, 10),
                new ProdSeed("7750667000504", "Chocolate Triángulo D'Onofrio 30g", 2.00, 44.00, 24, 15, "https://images.unsplash.com/photo-1511381939415-e44015466834?w=300", "Golosinas", "Nestlé Perú S.A.", 50, 10),
                new ProdSeed("7750667000505", "Caramelo Halls Mentol 34g", 1.50, 28.00, 20, 15, "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300", "Golosinas", "Mondelez Perú S.A.", 70, 12),
                new ProdSeed("7750667000506", "Chupetín Bon o Bon 18g", 1.20, 26.00, 24, 15, "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300", "Golosinas", "Nestlé Perú S.A.", 80, 12),
                new ProdSeed("7750667000507", "Gomitas Ambrosoli Ositos 100g", 3.00, 32.00, 12, 10, "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300", "Golosinas", "Molitalia S.A.", 45, 10),
                new ProdSeed("7750667000508", "Wafer Nick Vainilla 6pk", 1.80, 38.00, 24, 12, "https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=300", "Golosinas", "Molitalia S.A.", 50, 8),
                new ProdSeed("7750667000509", "Lentejitas Chin Chin 30g", 1.50, 32.00, 24, 15, "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300", "Golosinas", "Molitalia S.A.", 60, 10),
                new ProdSeed("7750667000510", "Marshmallows Millows 150g", 4.50, 48.00, 12, 8, "https://images.unsplash.com/photo-1582058091505-f87a2e55a40f?w=300", "Golosinas", "Molitalia S.A.", 35, 10),

                // 61-70: Panadería
                new ProdSeed("7750778000601", "Pan de Molde Bimbo Blanco 480g", 7.20, 68.00, 10, 8, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300", "Panadería", "Alicorp S.A.A.", 30, 1),
                new ProdSeed("7750778000602", "Pan de Molde Bimbo Integral 480g", 7.80, 74.00, 10, 8, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300", "Panadería", "Alicorp S.A.A.", 25, 1),
                new ProdSeed("7750778000603", "Tostadas Clásicas Bimbo 210g", 5.50, 52.00, 10, 8, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300", "Panadería", "Alicorp S.A.A.", 35, 4),
                new ProdSeed("7750778000604", "Pan Hamburguesa Bimbo x4", 4.80, 44.00, 10, 6, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300", "Panadería", "Alicorp S.A.A.", 24, 1),
                new ProdSeed("7750778000605", "Mermelada Fanny Fresa Pote 310g", 4.90, 54.00, 12, 10, "https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=300", "Panadería", "Molitalia S.A.", 40, 12),
                new ProdSeed("7750778000606", "Café Nescafé Tradición 50g", 6.50, 72.00, 12, 10, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=300", "Panadería", "Nestlé Perú S.A.", 45, 12),
                new ProdSeed("7750778000607", "Café Altomayo Gourmet 50g", 7.80, 86.00, 12, 8, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=300", "Panadería", "Costeño Alimentos S.A.C.", 35, 12),
                new ProdSeed("7750778000608", "Té Herbi Canela y Clavo x25", 3.50, 38.00, 12, 10, "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=300", "Panadería", "Alicorp S.A.A.", 50, 18),
                new ProdSeed("7750778000609", "Manzanilla Herbi Filtro x25", 3.50, 38.00, 12, 10, "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=300", "Panadería", "Alicorp S.A.A.", 45, 18),
                new ProdSeed("7750778000610", "Cocoa D'Onofrio 160g", 4.80, 52.00, 12, 8, "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=300", "Panadería", "Nestlé Perú S.A.", 40, 12),

                // 71-82: Limpieza
                new ProdSeed("7750889000701", "Detergente Bolívar Floral 800g", 7.90, 88.00, 12, 12, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 50, 24),
                new ProdSeed("7750889000702", "Detergente Ariel Doble Poder 850g", 8.90, 98.00, 12, 12, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Procter & Gamble Perú S.R.L.", 45, 24),
                new ProdSeed("7750889000703", "Suavizante Downy Floral 800ml", 9.50, 104.00, 12, 10, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Procter & Gamble Perú S.R.L.", 35, 24),
                new ProdSeed("7750889000704", "Lejía Clorox Tradicional 1L", 3.80, 42.00, 12, 15, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 60, 24),
                new ProdSeed("7750889000705", "Lavavajilla Sapolio Limón Pasta 350g", 3.20, 36.00, 12, 15, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 55, 24),
                new ProdSeed("7750889000706", "Lavavajilla Ayudín Limón Líquido 500ml", 5.80, 64.00, 12, 12, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Procter & Gamble Perú S.R.L.", 45, 24),
                new ProdSeed("7750889000707", "Limpiador Líquido Poett Lavanda 900ml", 4.20, 46.00, 12, 12, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 50, 24),
                new ProdSeed("7750889000708", "Limpiador Pinesol Original 900ml", 5.50, 60.00, 12, 10, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 35, 24),
                new ProdSeed("7750889000709", "Esponja Scotch Brite Verde 3M", 2.50, 26.00, 12, 20, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Procter & Gamble Perú S.R.L.", 70, 36),
                new ProdSeed("7750889000710", "Bolsas de Basura San Remo 50L x10", 4.80, 52.00, 12, 15, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 40, 36),
                new ProdSeed("7750889000711", "Paños Absorbentes Virutex x3", 5.20, 56.00, 12, 10, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Alicorp S.A.A.", 38, 36),
                new ProdSeed("7750889000712", "Aromatizador Glade Lavanda Aerosol 360ml", 8.50, 94.00, 12, 8, "https://images.unsplash.com/photo-1585421514738-01798e348b17?w=300", "Limpieza", "Procter & Gamble Perú S.R.L.", 30, 24),

                // 83-92: Cuidado Personal
                new ProdSeed("7750991000801", "Jabón de Tocador Dove Blanco 90g", 4.20, 46.00, 12, 15, "https://images.unsplash.com/photo-1608248597359-07b1a8f6dff4?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 60, 24),
                new ProdSeed("7750991000802", "Jabón Rexona Antibacterial 90g", 3.50, 38.00, 12, 15, "https://images.unsplash.com/photo-1608248597359-07b1a8f6dff4?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 55, 24),
                new ProdSeed("7750991000803", "Shampoo Head & Shoulders Limpieza 375ml", 18.50, 105.00, 6, 8, "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 30, 24),
                new ProdSeed("7750991000804", "Shampoo Pantene Restauración 375ml", 17.90, 102.00, 6, 8, "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 28, 24),
                new ProdSeed("7750991000805", "Crema Dental Colgate Total 12 75ml", 6.50, 72.00, 12, 15, "https://images.unsplash.com/photo-1559671261-26c6d05f3192?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 50, 18),
                new ProdSeed("7750991000806", "Crema Dental Kolynos Triple 75ml", 3.80, 42.00, 12, 15, "https://images.unsplash.com/photo-1559671261-26c6d05f3192?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 55, 18),
                new ProdSeed("7750991000807", "Papel Higiénico Suave Rindemax x4", 5.50, 60.00, 12, 15, "https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300", "Cuidado Personal", "Kimberly-Clark Perú S.R.L.", 70, 36),
                new ProdSeed("7750991000808", "Papel Higiénico Elite Doble Hoja x4", 6.20, 68.00, 12, 15, "https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300", "Cuidado Personal", "Kimberly-Clark Perú S.R.L.", 65, 36),
                new ProdSeed("7750991000809", "Desodorante Rexona Clinical Men 48g", 16.50, 94.00, 6, 6, "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?w=300", "Cuidado Personal", "Procter & Gamble Perú S.R.L.", 25, 24),
                new ProdSeed("7750991000810", "Toallas Femeninas Nosotras Noche x8", 6.80, 74.00, 12, 10, "https://images.unsplash.com/photo-1584556812952-905ffd0c611a?w=300", "Cuidado Personal", "Kimberly-Clark Perú S.R.L.", 40, 24),

                // 93-100: Conservas y Embutidos
                new ProdSeed("7750112000901", "Filete de Atún Campomar en Aceite 170g", 6.20, 140.00, 24, 15, "https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300", "Conservas", "Alicorp S.A.A.", 60, 24),
                new ProdSeed("7750112000902", "Filete de Atún Primor 170g", 6.50, 148.00, 24, 15, "https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300", "Conservas", "Alicorp S.A.A.", 55, 24),
                new ProdSeed("7750112000903", "Trozos de Atún Fanny Agua y Sal 170g", 4.80, 110.00, 24, 12, "https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300", "Conservas", "Molitalia S.A.", 45, 24),
                new ProdSeed("7750112000904", "Grated de Pescado A-1 170g", 3.80, 86.00, 24, 12, "https://images.unsplash.com/photo-1534483509719-3feaee7c30da?w=300", "Conservas", "Costeño Alimentos S.A.C.", 40, 24),
                new ProdSeed("7750112000905", "Duraznos en Almíbar A-1 820g", 9.80, 112.00, 12, 8, "https://images.unsplash.com/photo-1595231712325-9df03d32fa88?w=300", "Conservas", "Costeño Alimentos S.A.C.", 30, 24),
                new ProdSeed("7750112000906", "Maíz Dulce en Grano Bonduelle 300g", 4.50, 50.00, 12, 8, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?w=300", "Conservas", "Costeño Alimentos S.A.C.", 35, 24),
                new ProdSeed("7750112000907", "Arvejas con Zanahoria A-1 300g", 3.90, 44.00, 12, 8, "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=300", "Conservas", "Costeño Alimentos S.A.C.", 35, 24),
                new ProdSeed("7750112000908", "Jamonada Especial San Fernando 250g", 7.50, 85.00, 12, 10, "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=300", "Embutidos", "San Fernando S.A.", 30, 2)
        );

        int insertados = 0;
        for (ProdSeed p : catalogo) {
            Integer existeProd = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM productos WHERE sku = ?", Integer.class, p.sku);

            Long prodId;
            if (existeProd == null || existeProd == 0) {
                Long catId = jdbcTemplate.queryForObject(
                        "SELECT id FROM categorias_producto WHERE nombre = ? LIMIT 1", Long.class, p.categoria);
                Long provId = jdbcTemplate.queryForObject(
                        "SELECT id FROM proveedores WHERE nombre = ? LIMIT 1", Long.class, p.proveedor);

                jdbcTemplate.update(
                        "INSERT INTO productos (sku, nombre, precio_unitario, precio_caja, unidades_por_caja, stock_minimo, imagen_url, categoria_id, proveedor_id) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        p.sku, p.nombre, p.precioUnitario, p.precioCaja, p.unidadesCaja, p.stockMinimo, p.imagenUrl, catId, provId
                );
                prodId = jdbcTemplate.queryForObject(
                        "SELECT id FROM productos WHERE sku = ?", Long.class, p.sku);
                insertados++;
            } else {
                prodId = jdbcTemplate.queryForObject(
                        "SELECT id FROM productos WHERE sku = ?", Long.class, p.sku);
            }

            // Asegurar que tenga lote con stock
            if (prodId != null) {
                Integer cantLotes = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM lotes_inventario WHERE producto_id = ?", Integer.class, prodId);
                if (cantLotes == null || cantLotes == 0) {
                    LocalDate fVenc = LocalDate.now().plusMonths(p.mesesVenc);
                    jdbcTemplate.update(
                            "INSERT INTO lotes_inventario (producto_id, cantidad, fecha_ingreso, fecha_vencimiento) VALUES (?, ?, CURRENT_DATE, ?)",
                            prodId, p.stockInicial, fVenc
                    );
                }
            }
        }

        log.info("Catálogo maestro inicializado con éxito. Nuevos productos insertados: {}.", insertados);
    }
}
