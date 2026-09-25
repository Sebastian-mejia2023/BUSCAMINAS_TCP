# Proyecto Distribuido Cliente-Servidor - Buscaminas

Proyecto distribuido Cliente-Servidor del juego **Buscaminas**. La aplicación utiliza **sockets TCP** con serialización **JSON** sobre una arquitectura multihilo impulsada por un `ExecutorService` (Thread Pool) en el servidor y una interfaz interactiva de consola ANSI en el cliente.

---

## Arquitectura del Sistema

El sistema implementa una arquitectura desacoplada estructurada mediante un proyecto multimodular de **Gradle**, compuesto por los módulos `server` y `client`.

```text
├── client/          # Submódulo Cliente (Consola interactiva ANSI & Sockets TCP)
├── server/          # Submódulo Servidor (Thread Pool, Controlador TCP & BoardGame)
├── build.gradle     # Configuración de dependencias globales (Gson, etc.)
└── settings.gradle  # Inclusión de submódulos (include 'server', 'client')
```

### Características Clave

- **Conexión Corta TCP (Short-lived):** Apertura, transmisión y cierre explícito por cada comando, enviando tramas JSON delimitadas por salto de línea (`\n`).
- **Concurrencia Sincronizada:** Servidor multihilo basado en `Executors.newFixedThreadPool(5)` con exclusión mutua mediante `synchronized` sobre el recurso crítico `BoardGame` para evitar *Race Conditions*.
- **Framing Robusto:** Comunicación basada en DTOs (`Request` y `Response`) serializados con la librería **Google Gson**.
- **Visualización Dinámica:** Cliente de consola interactivo con soporte de color ANSI, revelado de cascada mediante BFS y notificación de estados de victoria y derrota.

---

## Catálogo del Protocolo JSON-TCP

Todos los mensajes se transmiten como tramas JSON delimitadas por un carácter de salto de línea (`\n`).

### Formato de Petición (Request)

```json
{
  "action": "<NOMBRE_ACCION>",
  "data": {
    "i": "fila",
    "j": "columna"
  }
}
```

### Formato de Respuesta (Response)

```json
{
  "status": "OK | ERROR",
  "data": {
    "board": [
      [
        {
          "isLandMine": false,
          "value": 0,
          "hide": true,
          "showAll": false,
          "isMarked": false
        }
      ]
    ],
    "win": false,
    "gameEnd": false,
    "message": "Mensaje informativo"
  }
}
```

### Acciones Soportadas

| Acción (`action`) | Parámetros (`data`) | Descripción |
|---|---|---|
| `INIT_GAME` | `n, m, minas` | Inicializa un nuevo tablero de `n x m` con la cantidad de minas indicada. |
| `SELECT_CELL` | `i, j` | Destapa una casilla. Si es 0, activa expansión en cascada (BFS). Si es mina, desencadena derrota (`BOOM`). |
| `MARK_CELL` | `i, j` | Alterna el estado de marca/bandera (`isMarked`) de una casilla oculta. |
| `GET_BOARD` | Vacío | Consulta el estado visual actual del tablero sin modificar las celdas. |
| `SOW_ALL` | Vacío | Activa la revelación completa del tablero (`showAll = true`). |

---

## Requisitos Previos e Instalación

- **Java JDK:** Versión 17 o superior instalada y configurada en la variable de entorno `JAVA_HOME`.
- **Gradle:** Opcional, ya que el proyecto incluye el Gradle Wrapper (`./gradlew`).

Para verificar la versión de Java instalada:

```bash
java -version
```

---

# Instrucciones Detalladas de Ejecución

Para poner en marcha el sistema distribuido, seguir los siguientes pasos desde la terminal del sistema operativo.

## Paso 1: Clonar e Ingresar al Repositorio

Abrir la terminal o línea de comandos e ingresar al directorio del proyecto:

```bash
git clone https://github.com/TU_USUARIO/TU_REPOSITORIO.git
cd TU_REPOSITORIO
```

---

## Paso 2: Limpiar y Compilar el Proyecto

Ejecutar la compilación de ambos submódulos (`server` y `client`) para descargar las dependencias y generar los archivos de clase.

### Linux / macOS

```bash
./gradlew clean build
```

### Windows (CMD / PowerShell)

```powershell
gradlew.bat clean build
```

---

## Paso 3: Iniciar el Servidor TCP

Abrir una primera terminal(desde la carpeta clase_tcp_udp) que actuará como servidor del juego.

El servidor se iniciará escuchando en la interfaz `0.0.0.0` y utilizará el puerto `12345` por defecto.

### Linux / macOS

```bash
./gradlew :server:run
```

### Windows (CMD / PowerShell)

```powershell
gradlew.bat :server:run
```

### Cambiar el puerto

De manera opcional, es posible cambiar el puerto predeterminado. Por ejemplo, para utilizar el puerto `8080`:

```bash
./gradlew :server:run --args="8080"
```

---

## Paso 4: Iniciar el Cliente de Consola

Abrir una **segunda terminal**, manteniendo abierta la terminal del servidor, para iniciar la interfaz interactiva del jugador.

### Linux / macOS

```bash
./gradlew :client:run
```

### Windows (CMD / PowerShell)

```powershell
gradlew.bat :client:run
```

---

## Paso 5: Prueba Concurrente Multiusuario

Para validar la capacidad multihilo del servidor:

1. Abrir una tercera o cuarta terminal adicional.
2. Ejecutar en cada una de ellas el comando:

```bash
./gradlew :client:run
```

3. Interactuar de forma simultánea desde los distintos clientes.

En la consola del servidor se podrán observar los hilos del `ThreadPool`, identificados como `pool-1-thread-X`, procesando de forma independiente y concurrente las peticiones de cada jugador.

---

# Simbología en Consola (Cliente)

| Símbolo | Significado |
|---|---|
| `[ . ]` | Casilla oculta que todavía no ha sido destapada. |
| `[ M ]` | Casilla marcada con bandera de advertencia (color amarillo). |
| `[ * ]` | Mina revelada tras una explosión o al finalizar el juego (color rojo). |
| `[0-8]` | Número de minas adyacentes a la casilla (color verde si el valor es mayor que 0). |

---

## Tecnologías Utilizadas

- **Java 17+**
- **Gradle**
- **TCP Sockets**
- **JSON**
- **Google Gson**
- **ExecutorService / Thread Pool**
- **BFS (Breadth-First Search)**
- **ANSI Console**
- **Arquitectura Cliente-Servidor**
