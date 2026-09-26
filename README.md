# EduSistem – Backend

Backend REST (Java 21 · Spring Boot 3.5 · Maven · PostgreSQL · Flyway) de una plataforma educativa para profesores:
gestión de grupos y asignaturas, evaluaciones, **calificación automática de exámenes de selección múltiple mediante hoja
de respuestas fotografiada**, actividades, asistencia, importación/exportación a Excel, auditoría y autenticación JWT.

El frontend (Flutter) consumirá esta API; la documentación interactiva está en **`/swagger-ui.html`**.

## Ejecución

Requisitos: JDK 21, Maven 3.8+, PostgreSQL 14+ con una base de datos vacía.

```bash
createdb edusistem                          # o la base que prefieras
cp .env.example .env                        # edita credenciales y JWT_SECRET (≥ 32 caracteres)
set -a; source .env; set +a
mvn spring-boot:run                         # Flyway crea el esquema; Hibernate solo lo valida (ddl-auto=validate)
# Swagger: http://localhost:8080/swagger-ui.html      Salud: http://localhost:8080/actuator/health
```

Tests: `mvn clean test` (no requiere PostgreSQL ni Docker: los de integración usan un PostgreSQL embebido).  
Empaquetado: `mvn clean package` → `target/edusistem-backend-0.1.0.jar` (`java -jar`, con las mismas variables de entorno).

### Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | no (`jdbc:postgresql://localhost:5432/edusistem`) | URL JDBC |
| `DB_USERNAME`, `DB_PASSWORD` | **sí** | Credenciales de la base |
| `JWT_SECRET` | **sí** | Clave HMAC (≥ 32 caracteres). Sin ella la app no arranca |
| `JWT_EXPIRATION` | no (`3600`) | Vigencia del access token en segundos |
| `JWT_REFRESH_EXPIRATION_DAYS` | no (`30`) | Vigencia del refresh token |
| `CORS_ALLOWED_ORIGINS` | no (vacío = CORS desactivado) | Orígenes permitidos separados por coma; admite patrones (`https://*.miapp.com`). Necesario para Flutter web |
| `LOGIN_MAX_ATTEMPTS_PER_EMAIL`, `LOGIN_MAX_ATTEMPTS_PER_IP`, `LOGIN_LOCK_WINDOW_MINUTES` | no (`5`, `50`, `15`) | Límite de intentos de login fallidos |
| `STORAGE_PATH` | no (`./storage`) | Carpeta de fotos de hojas, Excel importados y reportes de error |
| `MAIL_ENABLED` | no (`false`) | Si es `true` envía el código de recuperación por SMTP |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | solo con correo | SMTP |
| `SERVER_PORT` | no (`8080`) | Puerto HTTP |

Umbrales del lector de hojas (`application.yml`, `edusistem.omr.*`): `review-confidence-threshold` (0.90), `mark-threshold` (0.45), `ambiguity-threshold` (0.20).

## Arquitectura (hexagonal, monolito modular)

```
com.edusistem.core.<módulo>
├── domain          entity · enums · exceptions · inputports · outputports · vo · service   (POJOs; sin Spring/JPA/HTTP)
├── application     use_case/dtos (commands) · use_case/service (implementan los input ports; Java puro, sin Spring; @UseCaseTransactional solo en escrituras multi-tabla)
├── infrastructure  entity (@Entity JPA) · repository (Spring Data) · mapper (MapStruct) · adapter (implementan output ports) · config (<Módulo>BeanConfig registra los services de application como beans)
└── presentation    controllers (delgados) · dtos (request/response con Jakarta Validation)
```

Módulos: `auth`, `authorization`, `user`, `academic` (grados, grupos, periodos, asignaciones docentes, teaching periods),
`student`, `subject`, `grading` (escalas, pesos, nota del periodo), `evaluation`, `exam` (preguntas, hoja PDF, OMR, revisión),
`activity`, `attendance`, `imports`, `exports`, `audit`, `shared`.
Las reglas se pueden comprobar con `grep`: el paquete `domain` no importa `org.springframework`, `jakarta.persistence` ni `javax`.

## Modelo de datos

Migraciones Flyway en `src/main/resources/db/migration`:

* `V1`–`V27`: exactamente el DBML entregado (orden de dependencias, uniques, índices, tipos) + datos de referencia (roles `TEACHER`/`ADMIN`, categorías `EXAMS`/`ACTIVITIES`/`ATTENDANCE`, escalas 0-5, 0-10, 0-100).
* `V28`–`V35`: **extensiones fuera del DBML** (ver "Decisiones"): `audit_logs`, `password_reset_tokens`, `exam_answers.detection_status`, `exam_submissions.status_detail`, secuencia `student_code_seq`, índices en claves foráneas, `users.token_version` y `refresh_tokens`.

## Endpoints (`/api/v1`)

| Área | Endpoints |
|---|---|
| Auth | `POST /auth/register` · `/auth/login` · `/auth/refresh` · `/auth/logout` · `GET /auth/me` · `POST /auth/change-password` · `/auth/forgot-password` · `/auth/verify-code` · `/auth/reset-password` |
| Usuario | `GET/PUT /users/me` |
| Académico | CRUD `/grades`, `/groups`, `/subjects`, `/academic-periods` · `/teaching-assignments` (POST, GET, GET id, `PATCH /{id}/active`, DELETE) · `/teaching-periods` (POST, GET, GET id, DELETE) |
| Estudiantes | `GET /students`, `GET /students/{id}`, `POST /students/{studentId}/groups/{groupId}/withdrawal` |
| Calificación | `/grading-scales` (POST, GET) · `PUT/GET /teaching-periods/{id}/grading-configuration` · `GET /teaching-periods/{id}/period-grades` |
| Evaluaciones | `GET /evaluation-categories` · `GET /evaluations?teachingPeriodId=` · `GET/PUT /evaluations/{id}` |
| Exámenes | `POST/GET /exams` · `GET/PUT/DELETE /exams/{id}` · `PUT /exams/{id}/questions` |
| Hojas | `GET /exams/{id}/answer-sheet/{studentId}` (PDF) · `GET /exams/{id}/answer-sheets` (PDF del grupo) |
| Submissions | `POST /exams/{id}/submissions` (multipart `image`, `studentId?`, `replace?`) · `GET /exams/{id}/submissions` · `GET …/{submissionId}` · `GET …/{submissionId}/image` (foto original) · `PUT …/{submissionId}/answers/{questionNumber}` · `PUT …/{submissionId}/final-grade` |
| Actividades | CRUD `/activities` · `GET/PUT /activities/{id}/grades` · `PUT /activities/{id}/grades/{studentId}` |
| Asistencia | `/attendance-sessions` (POST, GET, GET id, DELETE) · `PUT /attendance-sessions/{id}/records` |
| Excel | `POST /imports/students` · `GET /imports/students/template` · `GET /imports`, `/imports/{id}`, `/imports/{id}/error-report` · `GET /exports/students`, `/exports/grades`, `/exports/attendance` · `GET /exports/teaching-periods/{id}/full` (Students+Grades+Attendance en un solo Excel; también sirve de plantilla) · `POST /imports/teaching-periods/{id}` (reimporta ese mismo Excel) |
| Auditoría | `GET /audit-logs` |

Listas paginadas: `?page=0&size=20` (máx. 100) → `{content, page, size, totalElements, totalPages}`.
Errores: `{timestamp, status, code, message, path[, errors]}` (sin trazas).

## Cómo funciona la calificación automática

1. **Hoja determinista**: `AnswerSheetLayout` define en puntos PDF (A4) la posición exacta de 4 marcadores de esquina (el de arriba-izquierda, más grande, fija la orientación), del QR y de cada burbuja (hasta 100 preguntas y 6 opciones, en columnas de 25).
2. **QR** `EDU1|examId|studentCode`: solo identifica examen y estudiante (sin datos personales). El backend lo valida contra la base: examen existe y es del profesor, estudiante existe, está activo en el grupo del teaching period.
3. **Lectura** (`AnswerSheetProcessorPort`, implementación Java pura con ZXing): localiza los marcadores → homografía → mide el relleno de cada burbuja frente al nivel de papel local (robusto a sombras, rotación y foto boca abajo).
4. **Decisión** (`BubbleClassifier`, dominio): una marca clara → `MARKED`; ninguna → `EMPTY` (no se marca incorrecta); varias → `MULTIPLE_MARK` (no se elige una); marca débil/dudosa → `REVIEW_REQUIRED` (se sugiere la candidata). Guarda `detection_confidence`.
5. **Puntuación bruta** (suma de puntos de correctas) ≠ **nota final** (`GradingScale.convert`: `min + score/máximo × (max−min)`) ≠ **nota del periodo** (`PeriodGradeCalculator`, según `grading_weights`).
6. **Estados**: `PROCESSED` · `REVIEW_REQUIRED` (hay respuestas por revisar) · `FAILED` (imagen no legible; motivo en `status_detail`). El profesor corrige respuestas o la nota final; cada edición queda en `audit_logs` con valor anterior y nuevo.

## Decisiones técnicas relevantes

* **Inconsistencias DBML ↔ requisitos** (aplicadas como migraciones separadas): faltaban tablas de auditoría y de recuperación de contraseña; `exam_answers` no distinguía vacía/múltiple/revisión (`detection_status`); `exam_submissions` no guardaba el motivo del fallo (`status_detail`); `student_code` es obligatorio pero el Excel no lo trae (se genera con `student_code_seq` si no se indica).
* **Aislamiento de datos**: no hay `owner` en estudiantes/grupos/asignaturas (así lo define el DBML). La propiedad se deriva **siempre** de `teaching_assignments.teacher_id` (`OwnershipGuard`); el profesor sale del JWT, nunca del cliente. Un recurso ajeno responde **404** (no revela existencia). Los catálogos compartidos (grados, grupos, asignaturas, periodos) solo se modifican si ningún *otro* profesor los usa; su borrado se bloquea si tienen dependencias.
* **Importar estudiantes exige tener una asignación docente sobre el grupo destino** (si no, el estudiante quedaría invisible para el profesor y se podría contaminar el grupo de otro). El año académico por defecto es el actual (columna opcional `academic_year`). Un estudiante existente solo se actualiza si el profesor ya puede verlo; siempre se matricula (o reactiva).
* **Importación**: se validan todas las filas, las válidas se aplican en una sola transacción y el resultado (`total/successful/failed`, errores por fila y reporte `.xlsx`) queda en `import_batches`.
* **Pesos**: se permiten configuraciones parciales al guardar (suma ≤ 100); para calcular la nota del periodo deben sumar exactamente 100 %. Una categoría sin evaluaciones aporta 0; asistencia con excusa (o sin registro) no cuenta; examen/actividad sin nota cuenta 0.
* **Escala**: no se puede cambiar la escala de un periodo si ya hay exámenes con nota final; para procesar exámenes debe existir configuración de calificación.
* **Un examen por (examen, estudiante)**: reenviar una hoja ya procesada exige `replace=true`; una `FAILED` se puede reintentar directamente.
* **Transacciones**: procesar una hoja (submission + respuestas + resultado) y la aplicación de una importación son una unidad; un fallo de lectura no propaga error, deja la submission `FAILED`. Las auditorías de fallo se guardan en transacción independiente para sobrevivir al rollback.
* **Sesiones**: access token JWT (HS256, 1 h) + refresh token opaco y rotativo (30 días, solo se guarda su SHA-256). Cada petición valida contra la base que el usuario siga activo y que `users.token_version` coincida con el del token; `logout`, cambio de contraseña, restablecimiento y reuso de un refresh token ya rotado incrementan la versión y revocan todo (todos los dispositivos). Un usuario desactivado pierde acceso de inmediato. `POST /auth/change-password` devuelve una sesión nueva para el dispositivo actual.
* **Fuerza bruta**: 5 logins fallidos por correo (50 por IP) en 15 min → `429 TOO_MANY_LOGIN_ATTEMPTS` con `Retry-After`. El contador está en memoria (válido para un solo nodo) y usa la IP de la conexión; detrás de un proxy inverso configura `server.forward-headers-strategy`.
* **CORS**: solo los orígenes de `CORS_ALLOWED_ORIGINS`, sin cookies (Bearer). Expone `Content-Disposition` y `Retry-After` para las descargas y el 429.
* Recuperación de contraseña: código de 6 dígitos guardado como hash BCrypt, 15 min de vigencia, 5 intentos; `forgot-password` responde igual exista o no el correo. Con `MAIL_ENABLED=false` no se envía nada ni se registra el código.

## Pendientes / fuera de alcance del MVP

* Contador de intentos de login compartido entre varios nodos (hoy es en memoria) y notificación al usuario de bloqueos.
* Rol `ADMIN` para gestionar los catálogos compartidos: existe el rol, pero no hay forma de crear administradores ni reglas que lo usen; requiere decidir cómo se otorga (no se implementó para no inventar el modelo).
* Instituciones y multi-tenant (el diseño no lo impide).
* Lectura OMR: probada con hojas generadas (rotadas, boca abajo, con sombra/ruido/desenfoque) pero no con fotos reales de papel impreso; conviene calibrar `edusistem.omr.*` con muestras reales. No corrige distorsión de lente ni pliegues, y no aplica la orientación EXIF de la cámara (la detección de marcadores tolera giros).
* El almacenamiento es un directorio local (`STORAGE_PATH`).
