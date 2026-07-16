# GymProgress
Aplicación web para el registro y seguimiento del progreso de entrenamiento en el gimnasio. Permite gestionar ejercicios, rutinas, sesiones de entrenamiento y visualizar el progreso del usuario a lo largo del tiempo.

## Descripción
GymProgress busca ofrecer a las personas usuarias una herramienta centralizada para:

- Registrar y consultar un catálogo de ejercicios, filtrable por tipo de entrenamiento (Calistenia, Cardio, Gimnasio) y grupo muscular.
- Crear y administrar rutinas de entrenamiento, con sus ejercicios asociados (series, repeticiones, peso sugerido, tempo y descanso).
- Seleccionar un programa de entrenamiento y consultar la rutina activa del día (`/rutinas/hoy`), incluyendo una estimación de la duración de la sesión.
- Llevar un historial de sesiones y visualizar el progreso (gráficos, rachas de constancia, peso corporal).
- Gestionar el perfil de usuario y la configuración general de la aplicación.

## Requisitos de ejecución

Para ejecutar el proyecto localmente se necesita:
1. JDK 17 o superior.
2. Maven 3.9+ 
3. MySQL 8+ en ejecución local, con una base de datos llamada `gymprogress`.

### Configuración de la base de datos
El proyecto se conecta por defecto a una instancia local de MySQL, configurada en `GymProgress/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gymprogress
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

Antes de ejecutar la aplicación:

1. Crear la base de datos en MySQL:
   ```sql
   CREATE DATABASE gymprogress;
   ```
2. Ajustar `spring.datasource.username` y `spring.datasource.password` según la configuración local de MySQL.
3. Con `ddl-auto=update`, Hibernate crea y actualiza las tablas automáticamente al iniciar la aplicación.

### Ejecución del proyecto
La aplicación queda disponible en [http://localhost:8080](http://localhost:8080).

## Estado del avance
El proyecto se encuentra en desarrollo activo.

Implementado:
- Estructura base del proyecto con Spring Boot (controladores, servicios, repositorios y entidades).
- Módulo de ejercicios: listado con filtros, creación, edición, eliminación y marcado como favorito, con persistencia en base de datos.
- Módulo de rutinas (en curso): entidades `Rutina` y `RutinaEjercicio`, listado de rutinas, selección de programa y vista de la rutina activa del día, con cálculo estimado de duración.
- Navegación general de la aplicación (dashboard, historial, progreso, perfil, configuración) con vistas Thymeleaf ya creadas.
