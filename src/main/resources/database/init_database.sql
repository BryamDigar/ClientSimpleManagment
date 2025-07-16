-- ========================================
-- Script de inicialización de Base de Datos
-- Plataforma de Gestión de Clientes
-- ========================================

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS plataforma_clientes
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Usar la base de datos creada
USE plataforma_clientes;

-- Eliminar tabla si existe (para recrear en caso de cambios)
DROP TABLE IF EXISTS clientes;

-- Crear tabla de clientes
CREATE TABLE clientes (
    numero_documento VARCHAR(20) NOT NULL PRIMARY KEY COMMENT 'Número de documento del cliente',
    nombre VARCHAR(100) NOT NULL COMMENT 'Nombre del cliente',
    apellidos VARCHAR(150) NOT NULL COMMENT 'Apellidos del cliente',
    fecha_nacimiento DATE NOT NULL COMMENT 'Fecha de nacimiento del cliente',
    ciudad VARCHAR(100) NOT NULL COMMENT 'Ciudad de residencia',
    correo_electronico VARCHAR(255) NOT NULL UNIQUE COMMENT 'Correo electrónico del cliente',
    telefono VARCHAR(20) NOT NULL COMMENT 'Número de teléfono',
    ocupacion ENUM('Empleado', 'Independiente', 'Pensionado') NOT NULL COMMENT 'Tipo de ocupación',
    es_viable BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Indica si el cliente está en edad productiva (18-65 años)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
    
    -- Índices para mejorar rendimiento
    INDEX idx_nombre (nombre),
    INDEX idx_apellidos (apellidos),
    INDEX idx_ciudad (ciudad),
    INDEX idx_correo (correo_electronico)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Tabla de clientes del sistema';

-- Insertar datos de ejemplo para pruebas
INSERT INTO clientes (
    numero_documento, 
    nombre, 
    apellidos, 
    fecha_nacimiento, 
    ciudad, 
    correo_electronico, 
    telefono, 
    ocupacion,
    es_viable
) VALUES 
(
    '12345678', 
    'Juan Carlos', 
    'García Pérez', 
    '1985-03-15', 
    'Bogotá', 
    'juan.garcia@email.com', 
    '+57 300 123 4567', 
    'Empleado',
    TRUE
),
(
    '87654321', 
    'María Elena', 
    'Rodríguez López', 
    '1978-08-22', 
    'Medellín', 
    'maria.rodriguez@email.com', 
    '+57 310 987 6543', 
    'Independiente',
    TRUE
),
(
    '11223344', 
    'Carlos Alberto', 
    'Martínez Silva', 
    '1993-12-10', 
    'Cali', 
    'carlos.martinez@email.com', 
    '+57 320 456 7890', 
    'Pensionado',
    TRUE
),
(
    '55667788', 
    'Ana Sofía', 
    'Hernández Castro', 
    '1992-06-05', 
    'Barranquilla', 
    'ana.hernandez@email.com', 
    '+57 315 234 5678', 
    'Empleado',
    TRUE
),
(
    '99887766', 
    'Luis Fernando', 
    'Gómez Vargas', 
    '1983-09-18', 
    'Cartagena', 
    'luis.gomez@email.com', 
    '+57 312 567 8901', 
    'Independiente',
    TRUE
),
(
    '13579024', 
    'Pedro', 
    'González Ruiz', 
    '1950-05-20', 
    'Bucaramanga', 
    'pedro.gonzalez@email.com', 
    '+57 318 111 2222', 
    'Pensionado',
    FALSE
),
(
    '24681357', 
    'Sofía', 
    'Morales Vega', 
    '2010-11-15', 
    'Pereira', 
    'sofia.morales@email.com', 
    '+57 319 333 4444', 
    'Empleado',
    FALSE
);

-- Mostrar estructura de la tabla creada
DESCRIBE clientes;

-- Mostrar los datos insertados
SELECT 
    numero_documento,
    nombre,
    apellidos,
    fecha_nacimiento,
    ciudad,
    correo_electronico,
    telefono,
    ocupacion,
    es_viable,
    YEAR(CURDATE()) - YEAR(fecha_nacimiento) - (DATE_FORMAT(CURDATE(), '%m%d') < DATE_FORMAT(fecha_nacimiento, '%m%d')) AS edad
FROM clientes
ORDER BY nombre;

-- Mensaje de confirmación
SELECT 'Base de datos plataforma_clientes creada exitosamente' AS status;
