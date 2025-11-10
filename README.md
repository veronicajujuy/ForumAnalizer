# Discord Forum Analyzer API

Una aplicación Spring Boot para extraer, analizar e ingestar mensajes de canales de Discord (tanto foros como canales de texto) y almacenarlos en una base de datos para su posterior análisis.

## 🚀 Características

- **Ingesta completa de mensajes**: Extrae todos los mensajes de canales de Discord sin límites
- **Soporte múltiple**: Compatible con foros (threads activos y archivados) y canales de texto
- **Paginación inteligente**: Procesa mensajes en lotes de 50 para optimizar las llamadas a la API
- **Rate limiting avanzado**: Implementa pausas inteligentes y backoff exponencial para evitar límites de la API
- **Reintentos automáticos**: Sistema de retry robusto con manejo de errores
- **Logging detallado**: Seguimiento completo del progreso con emojis para mejor visualización
- **Persistencia JPA**: Almacena mensajes con metadatos completos en base de datos
- **API REST**: Endpoints RESTful para interactuar con el servicio
- **Exportación CSV**: Funcionalidad para exportar datos analizados

## 🛠️ Tecnologías

- **Java 17+**
- **Spring Boot 3.x**
- **JDA (Java Discord API) 5.x**
- **Maven**
- **JPA/Hibernate**
- **Lombok**
- **SLF4J**
- **H2/MySQL/PostgreSQL** (configurable)

## 📋 Prerrequisitos

- Java 17 o superior
- Maven 3.8+
- Token de bot de Discord
- Base de datos compatible con JPA (H2, MySQL, PostgreSQL)

## ⚙️ Configuración

### 1. Clonar el repositorio

```bash
git clone <tu-repo-url>
cd forumAnalizer
```

### 2. Configurar Discord Bot

Crea un bot en [Discord Developer Portal](https://discord.com/developers/applications) y obtén el token.

Configura `src/main/resources/application.yml`:

```yaml
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/discord_analytics?useSSL=false&serverTimezone=UTC}
    username: ${DB_USER:root}
    password: ${DB_PASS:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
    show-sql: true 
    database-platform: org.hibernate.dialect.MySQL8Dialect

  server:
    port: 8080

  app:
    discord:
      token: ${DISCORD_TOKEN}
      guild-id: ${GUILD_ID:}
      tz: ${APP_TZ:America/Argentina/Buenos_Aires}
```
### 3. Configurar Variables de Entorno

Establece las siguientes variables de entorno:
``` bash
DISCORD_TOKEN="tu_token_de_bot_aqui"
FORUM_CHANNEL_ID="id_del_canal_de_foro_a_ingestar"

# Configuración de Base de Datos MySQL
DB_URL="jdbc:mysql://localhost:{tupuerto}/{tu_db}?useSSL=false&serverTimezone=UTC"
DB_USER="root"
DB_PASS="password"

# Configuración de la aplicación
APP_TZ="America/Argentina/Buenos_Aires"
GUILD_ID="opcional_id_del_servidor"
```
### 4. Permisos del Bot

Tu bot necesita los siguientes permisos en Discord:
- ✅ `Read Message History`
- ✅ `View Channel`
- ✅ `Read Messages/View Channels`

### 5. Invitar el Bot

Usa el siguiente enlace (reemplaza CLIENT_ID):
```
https://discord.com/api/oauth2/authorize?client_id=CLIENT_ID&permissions=66560&scope=bot
```

## 🚀 Instalación y Ejecución

### Método 1: Maven

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run
```

### Método 2: JAR ejecutable

```bash
# Compilar JAR
mvn clean package

# Ejecutar JAR
java -jar target/forum-analizer.jar
```

La aplicación se ejecutará en `http://localhost:8080`

## 📡 API Endpoints

### Ingesta de Mensajes

**Ingestar un canal (foro o texto):**
```http
POST /api/ingest/forum/{channelId}
```

**Ejemplo con cURL:**
```bash
# Ingestar un foro
curl -X POST http://localhost:8080/api/ingest/forum/123456789012345678

# Ingestar un canal de texto
curl -X POST http://localhost:8080/api/ingest/forum/987654321098765432
```

### Respuestas

**Para foros:**
```json
{
  "threads_processed": 15,
  "messages_saved": 1250
}
```

**Para canales de texto:**
```json
{
  "messages_saved": 500,
  "channel_name": "general"
}
```

### Logging Detallado

Para ver logs de paginación, habilita DEBUG en `application.yml`:

```yaml
logging:
  level:
    com.verovaldez.discord: DEBUG
```

### Base de Datos

**Para MySQL:**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/discord_analyzer
    username: tu_usuario
    password: tu_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
```

**Para PostgreSQL:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/discord_analyzer
    username: tu_usuario
    password: tu_password
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

## 📝 Logs de la Aplicación

La aplicación proporciona logs detallados con emojis para fácil seguimiento:

- 🚀 **Inicio de ingesta**: Cuando comienza el proceso
- 📊 **Estadísticas**: Información sobre threads encontrados
- 📋 **Canal detectado**: Tipo de canal identificado
- 🔄 **Progreso**: Procesamiento de threads individuales
- 📨 **Mensajes obtenidos**: Cantidad de mensajes por thread
- 📄 **Paginación**: Información detallada de páginas procesadas
- 📭 **Finalización**: Cuando no hay más mensajes
- ✅ **Thread completado**: Thread procesado exitosamente
- 💾 **Guardado**: Lotes de mensajes guardados en BD
- 💤 **Rate limiting**: Pausas para evitar límites de API
- 🎉 **Ingesta completada**: Proceso finalizado con éxito
- ❌ **Errores**: Problemas durante el procesamiento

## 🏗️ Estructura del Proyecto

```
src/main/java/com/verovaldez/discord/
├── DiscordAnaliticsApplication.java    # Clase principal de Spring Boot
├── config/
│   └── DiscordConfig.java             # Configuración del bot de Discord
├── controller/
│   └── ForumController.java           # Controlador REST para la ingesta
├── model/
│   └── DiscordMessage.java           # Entidad JPA para mensajes
├── repository/
│   └── DiscordMessageRepository.java # Repositorio para persistencia
├── service/
│   ├── AnaliticsService.java         # Servicio para análisis de datos
│   └── IngestionService.java         # Servicio principal de ingesta
└── mavenProyect/                     # Utilidades adicionales
    ├── CsvWriter.java                # Escritor de archivos CSV
    ├── ForumExporter.java            # Exportador de datos
    ├── Main.java                     # Clase main alternativa
    └── Util.java                     # Utilidades generales
```

## 🎯 Casos de Uso

### 1. Análisis de Comunidades
- Extraer conversaciones de foros de Discord
- Analizar patrones de participación
- Identificar temas populares

### 2. Moderación
- Revisar históricos de mensajes
- Detectar patrones de comportamiento
- Análisis de contenido

### 3. Investigación
- Estudios de comunicación en línea
- Análisis de redes sociales
- Minería de datos conversacionales

## 📊 Colección Postman

El proyecto incluye `Discord_Forum_Analyzer_API.postman_collection.json` con ejemplos de llamadas a la API.

Importar en Postman:
1. Abrir Postman
2. File → Import
3. Seleccionar el archivo JSON
4. Configurar variables de entorno si es necesario

## ⚠️ Consideraciones Importantes

### Rate Limits de Discord
- Discord tiene límites estrictos de API (50 requests/second)
- La aplicación implementa pausas inteligentes automáticamente
- Para canales muy grandes, el proceso puede tomar tiempo considerable

### Memoria y Rendimiento
- Los mensajes se procesan en lotes para optimizar memoria
- Para canales con millones de mensajes, considera aumentar la memoria JVM:
  ```bash
  java -Xmx2G -jar target/forum-analizer.jar
  ```

### Duplicados
- La aplicación no verifica duplicados automáticamente
- Considera implementar constraint UNIQUE en la BD para evitar duplicados:
  ```sql
  ALTER TABLE discord_message ADD CONSTRAINT uk_message_id UNIQUE (id);
  ```

### Privacidad y Términos de Servicio
- ⚠️ **Importante**: Asegúrate de cumplir con los Términos de Servicio de Discord
- Solo accede a canales donde tienes permisos explícitos
- Respeta la privacidad de los usuarios
- Considera anonimizar datos sensibles

## 🆘 Troubleshooting

### ❌ Error "Missing Access"
```
Solución: Verifica que el bot tenga permisos en el canal/servidor
```

### ❌ Logs DEBUG no aparecen
```yaml
# Agregar en application.yml:
logging:
  level:
    com.verovaldez.discord: DEBUG
```

### ❌ Rate limit exceeded
```
La aplicación maneja automáticamente los rate limits.
Si persiste, aumenta BASE_DELAY_MS en IngestionService.java
```

### ❌ OutOfMemoryError
```bash
# Aumentar memoria JVM:
java -Xmx4G -jar target/forum-analizer.jar
```

### ❌ Connection timeout
```yaml
# Agregar en application.yml:
spring:
  datasource:
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 10
```

## 📈 Roadmap

- [ ] **Dashboard web** para visualización de datos
- [ ] **Análisis de sentimientos** con NLP
- [ ] **Detección de temas** automática
- [ ] **Exportación a múltiples formatos** (JSON, Excel)
- [ ] **API de consultas** avanzadas
- [ ] **Métricas de engagement** automáticas
- [ ] **Integración con Grafana** para dashboards
- [ ] **Soporte para Discord Slash Commands**

## 🤝 Contribución

1. Fork el repositorio
2. Crea una rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -am 'Agrega nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crea un Pull Request

### Estilo de Código
- Usar Lombok para reducir boilerplate
- Logs con emojis para mejor legibilidad
- Documentar métodos públicos con JavaDoc
- Seguir convenciones de Spring Boot

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

## 👥 Autor

**Vero Valdez** - [GitHub Profile](https://github.com/tu-usuario)

## 🙏 Agradecimientos

- [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA) - Biblioteca para interactuar con Discord
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework de aplicación
- [Lombok](https://projectlombok.org/) - Reduce boilerplate en Java

---

⭐ **¡Si este proyecto te resulta útil, no olvides darle una estrella!** ⭐
