# GymProgress

Aplicación web para el registro y seguimiento del progreso de entrenamiento en el gimnasio. Permite gestionar ejercicios, rutinas, sesiones de entrenamiento y visualizar el progreso del usuario a lo largo del tiempo.

Proyecto del curso SC-403 — Desarrollo de Aplicaciones Web y Patrones, Universidad Fidélitas.

## Descripción

GymProgress busca ofrecer a las personas usuarias una herramienta centralizada para:

- Registrar y consultar un catálogo de ejercicios, filtrable por tipo de entrenamiento (Calistenia, Cardio, Gimnasio) y grupo muscular.
- Crear y administrar rutinas de entrenamiento, con sus ejercicios asociados (series, repeticiones, peso sugerido, tempo y descanso).
- Seleccionar un programa de entrenamiento y consultar la rutina activa del día ('/rutinas/hoy'), incluyendo una estimación de la duración de la sesión.
- Ejecutar el entrenamiento paso a paso, registrando cada serie con su peso, repeticiones y si se llegó al fallo muscular.
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
server.port=8084
spring.datasource.url=jdbc:mysql://localhost:3306/gymprogress
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

Hay dos formas de preparar la base de datos:

Opción A — con datos de ejemplo (recomendada para probar la aplicación).
Ejecutar el script 'GymProgress/src/main/resources/baseDeDatos.sql' en MySQL Workbench. El script crea la base completa (10 tablas) y la llena con:

- 2 usuarios listos para entrar
- 10 ejercicios en el catálogo, cada uno con su equivalente sin equipo
- 2 rutinas armadas con sus ejercicios
- rachas de 21 días y 21 días de entrenamientos con sus series
- registros de peso corporal

Usuarios de prueba que deja el script:

| Correo | Contraseña |
|---|---|
| 'kata@gmail.com' | '12345678' |
| 'ana@gmail.com' | '12345678' |

> El script empieza con `DROP DATABASE IF EXISTS gymprogress`, así que borra y recrea la base entera.

Opción B — base vacía.

1. Crear la base de datos en MySQL:
   ```sql
   CREATE DATABASE gymprogress;
   ```
2. Ajustar 'spring.datasource.username' y 'spring.datasource.password' según la configuración local de MySQL.
3. Con 'ddl-auto=update', Hibernate crea y actualiza las tablas automáticamente al iniciar la aplicación.

En este caso hay que registrarse desde la pantalla de registro y armar la rutina desde cero.

### Ejecución del proyecto

```bash
cd GymProgress
mvn spring-boot:run
```

La aplicación queda disponible en [http://localhost:8084](http://localhost:8084).

## Funcionalidades implementadas

### Cuentas y perfil

- Registro con encuesta de onboarding (objetivo, nivel, días por semana, minutos por sesión, equipamiento disponible y lesiones).
- Inicio y cierre de sesión, con la sesión guardada del lado del servidor.
- Perfil editable, con foto y preferencia de unidad de peso (kg o lb).
- Configuración de apariencia (modo oscuro / claro) y del comportamiento del descanso entre series.

### Catálogo de ejercicios

- Listado con búsqueda por nombre o grupo muscular y filtro por tipo de entrenamiento.
- Alta, edición y eliminación de ejercicios.
- Marcado de favoritos.
- Selección múltiple de grupos musculares.
- Imagen o GIF demostrativo por ejercicio.
- Guía visual de tempo (HU9): cada ejercicio define los segundos de sus cuatro fases (excéntrico, pausa abajo, concéntrico, pausa arriba). La pantalla de detalle anima un círculo que crece y se encoge siguiendo ese ritmo. Si el ejercicio no tiene tempo propio, se sugiere uno según el programa activo.

### Rutinas y programas

- Tres programas de entrenamiento: Fuerza, Hipertrofia y Resistencia
- Al cambiar de programa se conservan los mismos ejercicios (para no perder el historial de progreso de cada uno) pero se reajustan automáticamente las repeticiones objetivo y el descanso entre series:

  | Programa | Repeticiones | Descanso |
  |---|---|---|
  | Fuerza | 5 | 180 s |
  | Hipertrofia | 10 | 90 s |
  | Resistencia | 18 | 45 s |

  La pantalla avisa cuántos ejercicios se van a ajustar antes de confirmar el cambio, y solo reajusta cuando el programa cambia de verdad, para no pisar los valores que la persona haya personalizado a mano.
- Rutina del día en '/rutinas/hoy', con sus ejercicios, series, repeticiones y descansos.
- Duración estimada de la sesión, calculada con el tempo real de cada ejercicio, los descansos entre series y un minuto de transición por ejercicio.
- Agregar y quitar ejercicios de la rutina del día.

### Ejecución del entrenamiento

- Sesión activa con registro de series: ejercicio, número de serie, peso, repeticiones y marca de fallo muscular.
- Temporizador de descanso, que arranca solo al guardar una serie o de forma manual, según lo que la persona elija en Configuración. Se le pueden sumar 15 segundos o saltarlo.
- Ajustar la rutina sobre la marcha (HU31): durante el entrenamiento se puede omitir un ejercicio o sustituirlo por otro del mismo grupo muscular. Cada cambio queda registrado en la sesión.
- Resumen al finalizar: volumen total levantado, duración, series completadas, récords personales superados y el detalle de todas las series.

### Alternativas sin equipo (HU7)

- Opción "Hoy no voy al gimnasio": propone, para cada ejercicio de la rutina que necesite equipo, alternativas del mismo grupo muscular que se pueden hacer en casa.
- Los ejercicios que ya se pueden hacer sin equipo se marcan como tales y no se proponen para cambio.

### Semana de descarga (deload)

La aplicación detecta sobreentrenamiento y sugiere bajar el ritmo una semana.

- Analiza las últimas semanas de entrenamiento y sugiere la descarga cuando se llega al fallo en más del 30% de las series dos semanas seguidas, o cuando el volumen total cae más de un 15%.
- La sugerencia se muestra con la tabla de datos que la motivó (sesiones, series, series al fallo y volumen por semana), para que la decisión se entienda.
- La persona puede aceptar, posponer una semana o ignorar la sugerencia (en cuyo caso no se vuelve a proponer en un mes).
- Al aceptar, durante 7 días la rutina baja al 60 % del volumen: las series de cada ejercicio se reducen y la pantalla muestra el valor original tachado junto al ajustado.

### Constancia y progreso

- Racha de días entrenados (HU22): se muestra destacada en el inicio y en la rutina del día, en verde si ya se entrenó hoy y en morado si todavía no. Guarda además la racha máxima histórica.
- Historial completo de entrenamientos, con el detalle de las series de cada sesión.
- Registro de peso corporal, con recordatorio cuando pasa una semana sin registrar, y conversión entre kg y lb.
- Gráfico de evolución del peso corporal.

### Modo sin conexión (HU2)

- Service worker que guarda en caché la rutina del día y los recursos estáticos, de modo que la pantalla sigue funcionando sin Internet.
- Las series registradas sin conexión se guardan en una cola local ('localStorage'), que sobrevive a recargas y a cerrar el navegador.
- La cola se sincroniza sola cuando vuelve la conexión.
- Una barra fija arriba avisa cuando se está trabajando sin conexión.

### Interfaz

- Diseño responsivo con Bootstrap 5.3, con menú lateral en escritorio y menú desplegable en móvil.
- Modo oscuro y modo claro, con la preferencia guardada en el navegador para que no parpadee al cargar.
- El nombre y la foto de la persona aparecen en el menú lateral de todas las pantallas.

## Arquitectura

El proyecto sigue una arquitectura en capas:

```
controller/   →  atiende las peticiones web y arma el modelo de cada pantalla
service/      →  reglas de negocio (qué se puede hacer y qué no)
repository/   →  acceso a datos con Spring Data JPA
domain/       →  entidades, el espejo en Java de las tablas
templates/    →  vistas Thymeleaf
```

Las entidades no llevan lógica de negocio: esa vive en los servicios. Los controladores no consultan la base directamente, siempre pasan por un servicio.

Entidades del dominio (10 tablas): 'Usuario', 'Ejercicio', 'Sesion', 'SerieRegistrada', 'Racha', 'PesoCorporal', 'CambioEjercicio', 'Rutina', 'RutinaEjercicio', 'SemanaDescarga'.

Patrones y decisiones de diseño usadas:

- POST-Redirect-GET con atributos flash en todos los formularios, para que recargar la página no repita la operación.
- Inyección por constructor en controladores y servicios, con los campos 'final'.
- '@ControllerAdvice' para dejar el usuario activo disponible en todas las pantallas sin repetirlo en cada controlador.
- Consultas derivadas de Spring Data ('findByUsuarioIdAndFecha') y JPQL con '@Query' para las agregaciones.
- Todas las consultas filtran por el usuario dueño del dato, para que nadie pueda leer o borrar registros ajenos cambiando el número del formulario.

## Tecnologías

| Tecnología | Versión / uso |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.3 |
| Spring Data JPA / Hibernate | acceso a datos |
| Thymeleaf | motor de plantillas |
| MySQL | 8+ |
| H2 | base en memoria, solo para las pruebas |
| Bootstrap | 5.3.3 |
| Maven | gestión del proyecto |

## Estructura del repositorio

```
GymProgress/
├── src/main/java/com/fidelitas/gymprogress/
│   ├── controller/          controladores web
│   ├── service/             reglas de negocio
│   ├── repository/          acceso a datos
│   └── domain/              entidades
├── src/main/resources/
│   ├── templates/           vistas Thymeleaf
│   ├── static/              css, js, imágenes y service worker
│   ├── baseDeDatos.sql      esquema completo con datos de ejemplo
│   └── application.properties
└── src/test/                pruebas
```

## Estado del avance

El proyecto se encuentra en desarrollo activo.

**Implementado y funcionando:**

- Estructura base del proyecto con Spring Boot (controladores, servicios, repositorios y entidades).
- Registro, inicio de sesión, perfil y configuración.
- Módulo de ejercicios: listado con filtros, creación, edición, eliminación, favoritos y guía de tempo.
- Módulo de rutinas: programas de entrenamiento con reajuste automático de repeticiones y descansos, rutina del día y cálculo estimado de duración.
- Ejecución de la sesión de entrenamiento con registro de series, temporizador de descanso y resumen final con récords personales.
- Alternativas sin equipo y ajuste de la rutina durante el entrenamiento.
- Semana de descarga con detección de sobreentrenamiento.
- Rachas de constancia, historial y seguimiento de peso corporal.
- Modo sin conexión con service worker y cola de sincronización.
- Navegación general de la aplicación (dashboard, historial, progreso, perfil, configuración) con vistas Thymeleaf.
- Script 'baseDeDatos.sql' con el esquema completo y datos de ejemplo.

**Pendiente:**

- Cifrado de contraseñas (hoy se guardan en texto plano).
- Ampliar la cobertura de pruebas automatizadas.

## Equipo

Proyecto desarrollado para el curso SC-403 — Desarrollo de Aplicaciones Web y Patrones, Universidad Fidélitas.

- Arias Morera Ana Virginia
- Catón Zúñiga Katalina
- García Rojas Santiago Andrés
- Jiménez Calderón Daniel Alejandro
