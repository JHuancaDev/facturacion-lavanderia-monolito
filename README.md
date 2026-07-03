# Sistema de Gestión para Lavandería con Facturación Electrónica

![Logo de ejemplo](/img/login.png)


Sistema web desarrollado para administrar una lavandería, permitiendo gestionar pedidos, servicio y emitir comprobantes electrónicos (Boletas) mediante la SUNAT.


# 📋 Características

- ✅ Inicio de sesión con autenticación.
- ✅ Gestión de usuarios y roles.
- ✅ Gestión de categorías de prendas.
- ✅ Gestión de servicios.
- ✅ Registro de órdenes de lavandería.
- ✅ Control del estado de cada orden.
- ✅ Cálculo automático del total.
- ✅ Emisión de Boletas Electrónicas.
- ✅ Generación de XML según UBL 2.1.
- ✅ Firma digital del comprobante.
- ✅ Envío a SUNAT (Beta y Producción).
- ✅ Generación de PDF del comprobante.
- ✅ Consulta del historial de comprobantes.
- ✅ Panel administrativo.


# Tecnologías Utilizadas

## Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

## Frontend

- HTML5
- CSS3
- Bootstrap 5
- JavaScript
- Thymeleaf

## Base de Datos

- MySQL

## Facturación Electrónica

- UBL 2.1
- XML
- Firma Digital (.pfx)
- SOAP Web Services SUNAT
- OpenUBL


# 📂 Estructura del Proyecto

```
src
├── main
│   ├── java
│   │   └── dev
│   │       └── jhuanca
│   │           └── facturacion
│   │               ├── enums
│   │               │   └── EstadoPedido.java
│   │               ├── repository
│   │               │   ├── ClienteRepository.java
│   │               │   ├── ServiciosRepository.java
│   │               │   ├── UbicacionRopaRepository.java
│   │               │   ├── UserRepository.java
│   │               │   ├── DetallePedidoRepository.java
│   │               │   ├── BoletaRepository.java
│   │               │   └── PedidoRepository.java
│   │               ├── config
│   │               │   ├── AppConfig.java
│   │               │   ├── SecurityConfig.java
│   │               │   └── DataInitializer.java
│   │               ├── controller
│   │               │   ├── DashboardController.java
│   │               │   ├── ServiciosController.java
│   │               │   ├── BoletaController.java
│   │               │   ├── ApiClienteController.java
│   │               │   ├── AuthC.java
│   │               │   └── PedidoController.java
│   │               ├── FacturacionApplication.java
│   │               ├── entity
│   │               │   ├── Servicios.java
│   │               │   ├── Boleta.java
│   │               │   ├── Cliente.java
│   │               │   ├── Pedido.java
│   │               │   ├── DetallePedido.java
│   │               │   ├── UbicacionRopa.java
│   │               │   └── User.java
│   │               └── service
│   │                   ├── CustomUserDetailsService.java
│   │                   ├── BoletaPdfService.java
│   │                   ├── SunatService.java
│   │                   └── WhatsAppService.java
│   └── resources
│       ├── certificados
│       │   ├── sunat.crt
│       │   └── sunat.pfx
│       ├── static
│       │   ├── css
│       │   │   ├── bootstrap-custom.css
│       │   │   └── bootstrap-custom.css.map
│       │   ├── image
│       │   │   └── login_fondo.svg
│       │   └── js
│       │       ├── bootstrap.bundle.js
│       │       ├── bootstrap.bundle.min.js
│       │       ├── bootstrap.bundle.min.js.map
│       │       ├── bootstrap.esm.js
│       │       ├── bootstrap.esm.js.map
│       │       ├── bootstrap.esm.min.js
│       │       ├── bootstrap.esm.min.js.map
│       │       ├── bootstrap.js
│       │       ├── bootstrap.js.map
│       │       ├── bootstrap.min.js
│       │       ├── bootstrap.min.js.map
│       │       └── bootstrap.bundle.js.map
│       ├── templates
│       │   ├── auth
│       │   │   └── login.html
│       │   ├── layout
│       │   │   ├── base.html
│       │   │   └── sidebar.html
│       │   ├── services
│       │   │   └── list.html
│       │   ├── dashboard
│       │   │   └── index.html
│       │   ├── pedido
│       │   │   ├── ubicacion.html
│       │   │   ├── detalle.html
│       │   │   ├── form.html
│       │   │   └── list.html
│       │   └── boleta
│       │       └── list.html
│       └── application.properties
└── test
    └── java
        └── dev
            └── jhuanca
                └── facturacion
                    └── FacturacionApplicationTests.java
```



# Requisitos

- Java JDK 21
- Maven 3.9+
- MySQL 8+
- Git


# Instalación

## 1. Clonar el repositorio

```bash
git clone https://github.com/JHuancaDev/facturacion-lavanderia-monolito.git
```

Entrar al proyecto

```bash
cd facturacion-lavanderia-monolito
```


## 2. Crear la Base de Datos

```sql
CREATE DATABASE lavanderia;
```


## 3. Configurar application.properties

```properties
# application.properties
spring.application.name=facturacion

spring.datasource.url=jdbc:mysql://127.0.0.1:3306/facturacion_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima
spring.datasource.username=javier
spring.datasource.password=developer99
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver


spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true


server.address=0.0.0.0
server.port=8080
server.servlet.session.timeout=30m


sunat.ruc=
sunat.razon-social=
sunat.usuario=
sunat.password=
sunat.url-beta=https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService
sunat.ambiente=BETA
sunat.serie-boleta=B001

sunat.certificado.ruta= PREFERENTE .PFX
sunat.certificado.password=
```


## 4. Ejecutar

Con Maven

```bash
mvn spring-boot:run
```

o desde NetBeans / IntelliJ.

---

# Usuario

| Rol | Descripción |
|------|-------------|
| ADMIN | Control total del sistema |



# 🧺 Flujo del Sistema

```text
Cliente
   │
   ▼
Registrar Cliente
   │
   ▼
Registrar Orden
   │
   ▼
Agregar Prendas
   │
   ▼
Calcular Total
   │
   ▼
Registrar Pago
   │
   ▼
Emitir Boleta / Factura
   │
   ▼
Generar XML UBL
   │
   ▼
Firmar XML
   │
   ▼
Enviar a SUNAT
   │
   ▼
Generar PDF
```



# Estados de la Orden

- REGISTRADO
- EN_PROCESO
- FINALIZADO
- ENTREGADO

> Una orden **Finalizada** únicamente puede cambiar al estado **Entregado**, evitando modificaciones posteriores.

---

# Facturación Electrónica

El sistema permite:

- Emitir Boletas Electrónicas.
- Generar XML UBL 2.1.
- Firmar digitalmente los comprobantes.
- Enviar comprobantes a SUNAT.
- Obtener CDR.
- Generar representación impresa (PDF).



# Seguridad

- Inicio de sesión con Spring Security.
- Protección de rutas.
- Encriptación de contraseñas con BCrypt.
