# ProyectoLP · Fases 0 a 6

Requiere JDK 21, Maven Wrapper, las tablas creadas con `supabase/00_schema.sql` y `config.properties` en la raíz. Si falta, copia `config.example.properties` y completa `supabase.url` y `supabase.key` con la URL del proyecto y una publishable key.

Ejecuta en Windows desde la raíz:

```powershell
.\mvnw.cmd clean javafx:run
.\mvnw.cmd test
```

En macOS/Linux usa `./mvnw clean javafx:run` para abrir la aplicación y `./mvnw test` para ejecutar las pruebas.

## Módulos existentes

- **Dashboard (Fase 1):** resumen de tareas, proyectos, hábitos, racha productiva y balance financiero.
- **Tareas (Fase 2):** CRUD, estado, prioridad, fecha y progreso.
- **Proyectos (Fase 3):** proyectos con etapas y porcentaje de avance.
- **Hábitos (Fase 4):** registro diario, historial, rachas y cumplimiento.
- **Finanzas (Fase 6):** ingresos, gastos, balance de flujo, estadísticas mensuales y metas económicas.

## Hábitos

Desde el Dashboard abre **Hábitos**. Crea y edita hábitos, actívalos o desactívalos (no se borran para conservar su historial) y selecciona una fecha para marcar o desmarcar su cumplimiento, incluidos días pasados. La tabla muestra racha actual, mejor racha y cumplimiento en los últimos 30 días; el historial muestra cada uno de esos días. La tarjeta del Dashboard permite marcar el día de hoy y actualiza la racha general al guardar.

El upsert de `habit_logs` usa la restricción única `(habit_id, log_date)`. La racha actual considera activa una racha que termina hoy o ayer; el porcentaje usa los 30 días calendario desde hoy menos 29 días hasta hoy, inclusive.

## Datos de prueba

Ejecuta en Supabase SQL Editor después de aplicar `supabase/00_schema.sql`. Crea una racha larga, una racha rota y un hábito sin historial:

```sql
insert into public.habits (name, description, active)
values ('Racha larga', 'Cumplimiento diario de prueba', true),
       ('Racha rota', 'Historial con una interrupción', true),
       ('Sin historial', 'Aún no tiene registros', true);

insert into public.habit_logs (habit_id, log_date, completed)
select h.id, d::date, true
from public.habits h
cross join generate_series(current_date - interval '9 days', current_date, interval '1 day') as d
where h.name = 'Racha larga';

insert into public.habit_logs (habit_id, log_date, completed)
select h.id, d::date, true
from public.habits h
cross join (values (current_date - 10), (current_date - 9), (current_date - 8),
                   (current_date - 4), (current_date - 3)) as days(log_date)
where h.name = 'Racha rota';
```

Pulsa **Actualizar** si agregas registros manualmente. Las políticas `anon` configuradas en la Fase 0 son temporales de desarrollo; deben endurecerse al agregar autenticación.

## Errores comunes

- **401**: revisa URL y publishable key en `config.properties`.
- **404 / tabla no encontrada**: ejecuta `supabase/00_schema.sql` en el proyecto configurado.
- **403 o no guarda el registro**: revisa RLS, políticas de escritura y que exista la restricción única `habit_logs(habit_id, log_date)`.
- **No aparece un hábito inactivo**: activa **Mostrar inactivos**.
- **La app no compila**: configura `JAVA_HOME` con JDK 21 antes de ejecutar Maven Wrapper.

## Finanzas (Fase 6)

Finanzas registra ingresos (salario, trabajos independientes, ventas, bonificaciones, regalos u otro) y gastos (transporte, mercado, comida, universidad, entretenimiento, tecnología, salud, servicios u otro). Muestra el balance de flujo acumulado, el ahorro neto del mes actual, gastos por categoría y evolución mensual de ingresos frente a gastos. También permite crear metas, editar/eliminar registros y agregar aportes; el progreso visual se limita al 100%.

Aplica de nuevo `supabase/00_schema.sql` en Supabase SQL Editor. Las tablas `income`, `expenses` y `financial_goals` se crean idempotentemente y sus políticas para `anon` están marcadas como temporales, igual que el resto del esquema. La tarjeta «Estado financiero» del Dashboard usa el mismo servicio de cálculo que el módulo y se recarga al volver desde Finanzas.

Para probar, utiliza varios ingresos y gastos fechados en distintos meses y categorías (por ejemplo salario/ventas y mercado/transporte), además de una meta como «Portátil» con objetivo 2.000.000 y uno o más aportes parciales. Desde Dashboard abre **Finanzas**, crea/edita/elimina movimientos, filtra por tipo/categoría/mes, revisa los gráficos y prueba un aporte que complete o supere la meta. Para ejecutar pruebas locales: `./mvnw test` (Windows: `.\mvnw.cmd test`).

Todos los FXML se pueden abrir desde SceneBuilder con **File > Open** dentro de `src/main/resources/com/example/proyectolp/ui`. La pantalla principal de Finanzas es `finances.fxml`; los formularios modales son `movement-form.fxml` y `goal-form.fxml`.

### Estructura

- `config/`: configuración externa de Supabase (`config.properties`, ignorado por Git; partir de `config.example.properties`).
- `model/`: entidades de tareas, proyectos, hábitos y movimientos/metas financieras.
- `repository/`: acceso PostgREST a cada tabla mediante `SupabaseService`.
- `service/`: validación y cálculos de progreso, rachas, dashboard y finanzas.
- `controller/` y `src/main/resources/.../ui/`: controladores MVC y vistas FXML.
- `supabase/00_schema.sql`: esquema idempotente, índices, RLS y políticas temporales para desarrollo.
- `src/test/`: pruebas JUnit locales, sin conexión a Supabase.

Las políticas `anon` son exclusivamente temporales de desarrollo. Antes de desplegar, configura autenticación, `user_id` y políticas RLS por usuario.
# Fase 7: Entrenamiento físico

El módulo de Entrenamiento permite mantener rutinas con ejercicios, registrar sesiones y series, calcular volumen como `peso × repeticiones` por cada serie, consultar progresión y revisar distribución muscular. La pantalla incluye descanso configurable de 90 segundos con pausa y reinicio. El Dashboard comparte el cálculo de sesiones y volumen semanal mediante `TrainingStatsService`.

## Preparar y probar

1. Ejecuta el SQL idempotente `supabase/00_schema.sql` en el editor SQL de Supabase. Las políticas `dev_anon_all` son temporales y solo apropiadas para desarrollo.
2. Inicia la aplicación con `./mvnw javafx:run` (Windows: `mvnw.cmd javafx:run`) y abre **Entrenamiento** desde el Dashboard.
3. Crea o edita dos rutinas con varios ejercicios. Inicia una sesión, registra cada serie y finalízala; revisa las pestañas de Progresión y Estadísticas y vuelve al Dashboard para confirmar el resumen semanal.
4. En la pestaña Sesión, cambia el descanso, inicia, pausa y reinicia el temporizador.
5. Las pruebas sin conexión cubren volúmenes, período, comparación de progresión, distribución y estado inicial/reinicio del temporizador: `mvn test`.

## Datos de prueba recomendados

Crea al menos 2 rutinas y 4–6 sesiones distribuidas en distintas semanas. Usa ejercicios repetidos entre sesiones (por ejemplo, Press banca y Sentadilla) y sube gradualmente peso, repeticiones o series. Incluye pecho, espalda, piernas y core para que el PieChart muestre proporciones distintas. Una serie de 40 kg × 8 repeticiones aporta 320 kg·reps.
# Fase 8: créditos, progreso y recompensas

La equivalencia es 1 crédito = $2.000 COP. El saldo se calcula sumando `credit_ledger`; los eventos usan `dedupe_key` y no otorgan crédito retroactivo. La racha se actualiza desde el listener central de escrituras de `SupabaseService`, con fecha en `America/Bogota`. Recompensas incluye historial, equivalente COP, XP, nivel, rango, insignias y misiones diarias. La lista de comercios es informativa; no existe canje real.

## Probar la Fase 8

1. Ejecuta `supabase/00_schema.sql` en Supabase; las políticas `dev_anon_all` siguen siendo temporales para desarrollo.
2. Configura `config.properties` localmente y ejecuta `mvnw.cmd javafx:run` en Windows (o `./mvnw javafx:run`).
3. Completa un hábito con fecha de hoy, una tarea con y sin proyecto, y una etapa. Comprueba el saldo en el Dashboard y en **Recompensas**. Desmarca/reabre y vuelve a completar para confirmar la idempotencia.
4. Aporta a una meta de al menos $60.000 COP cruzando varios hitos. Revisa el historial para cada crédito otorgado.
5. Registra entrenamientos, movimientos financieros y eventos para progresar las misiones. Los créditos no se otorgan por XP, misiones, niveles ni insignias.
6. Pruebas puras sin red: `mvn test`.

Para simular una racha perdida en un entorno de desarrollo, reemplaza las fechas con una fecha de hace tres días y establece la racha deseada. Al abrir la app o hacer una acción se ejecuta la penalización una sola vez:

```sql
update public.user_stats
set current_streak = 12,
    best_streak = greatest(best_streak, 12),
    last_activity_date = ((now() at time zone 'America/Bogota')::date - 3),
    last_penalty_for = null
where user_id = '00000000-0000-0000-0000-000000000000'::uuid;
```

Reemplaza el UUID por el `id` de la cuenta que estás probando (puedes consultarlo en Authentication → Users).

La regla de penalización divide el saldo entre 3 para una racha de 12 y redondea hacia abajo. El registro aparece como movimiento negativo en el historial de créditos.

# Fase 9: autenticación y aislamiento por usuario

La app usa Supabase Auth por REST y la anon/publishable key configurada en `config.properties`. Al llamar PostgREST agrega el access token del usuario como `Authorization: Bearer …`; la base aplica RLS por `user_id = auth.uid()`. La contraseña nunca se guarda. **Recordarme** persiste solo el refresh token en `~/.proyectolp/session.refresh` con permisos restringidos cuando el sistema operativo lo admite.

## Configuración de Supabase y migración

1. En Supabase abre **Authentication → Sign In / Providers → Email** y activa el proveedor Email. Decide el valor de **Confirm email**: desactivado permite entrar inmediatamente; activado solicita confirmar el correo antes de iniciar sesión.
2. Ejecuta `supabase/00_schema.sql` solo si preparas una base nueva. Para una base existente que ya ejecuta la fase anterior, ejecuta `supabase/09_auth_multiusuario.sql` en el editor SQL.
3. En `09_auth_multiusuario.sql`, revisa los bloques comentados A (reasignar datos heredados a un UUID de `auth.users`) y B (eliminarlos). Si quieres conservarlos, descomenta A y cambia el UUID; el script asigna todos los registros huérfanos de las tablas cubiertas a esa cuenta. Si quieres empezar limpio, descomenta B.
4. Crea la primera cuenta en la aplicación. Con confirmación activa, revisa el correo y vuelve al login.
5. Cuando no queden registros con `user_id` nulo, ejecuta `supabase/09_auth_multiusuario_cierre.sql`. El script informa la tabla y cantidad que impiden cerrar la migración.
6. Configura `supabase.url` y `supabase.key` en `config.properties` con la clave pública/anon del proyecto; no uses secretos privilegiados.

## Prueba de dos cuentas

Registra las cuentas A y B. Con A crea una tarea, un proyecto, un hábito marcado, un movimiento y meta financiera, una rutina/sesión, y valida créditos, XP, racha, insignias y misiones. Cierra sesión y entra como B: su Dashboard y módulos deben iniciar vacíos. Crea datos B, vuelve a entrar con A y verifica que solo reaparecen los de A.

Para la comprobación directa, toma el access token de cada sesión desde una herramienta de desarrollo sin publicarlo. Envía a `GET /rest/v1/tasks?select=*` los headers `apikey: <clave pública>` y `Authorization: Bearer <token B>`: no debe devolver tareas de A. Un `PATCH`/`DELETE` filtrado por el id de una fila de A bajo el token B no debe cambiarla. Con solo `apikey` y sin `Authorization`, las tablas deben rechazar o devolver cero filas por las políticas RLS y permisos anon. No compartas tokens ni los guardes en logs.

La limitación conocida es que una FK que apunta solo a un `id` todavía puede referenciar un registro de otra cuenta aunque RLS impida leerlo; validar pertenencia en FKs compuestas queda como trabajo futuro. También quedan para trabajo futuro la recuperación de contraseña, OAuth, ranking y la validación de pertenencia de todas las FKs. Fase 9 no incorpora agenda, calendario, Pomodoro, recordatorios ni voz.

Las pruebas de autenticación usan un transporte HTTP simulado y no requieren red: `mvnw.cmd test` (Windows) o `./mvnw test`.

## TRM (d&oacute;lar)

La pesta&ntilde;a **D&oacute;lar (TRM)** consulta el conjunto oficial `32sa-8pi3` de [datos.gov.co](https://www.datos.gov.co/resource/32sa-8pi3.json), con datos calculados y certificados por la Superintendencia Financiera de Colombia. La API SODA es p&uacute;blica; no requiere token de aplicaci&oacute;n. La configuraci&oacute;n opcional `trm.app.token` puede usarse como cabecera `X-App-Token` si el proveedor la recomienda o exige en el futuro.

El valor se selecciona por su rango de vigencia en la zona `America/Bogota`; para fines de semana y festivos se conserva la vigencia que los cubre. Si a&uacute;n no existe un valor para hoy se muestra el &uacute;ltimo publicado. El cach&eacute; persistente est&aacute; en `~/.proyectolp/trm-cache.json`; sin conexi&oacute;n se presenta el &uacute;ltimo dato guardado, incluso vencido, y si no hay cach&eacute; se muestran equivalentes como `?`.

Las conversiones y equivalencias usan la TRM actual, no la tasa del d&iacute;a de cada movimiento. La TRM es una tasa de referencia y no necesariamente el precio de compra o venta de una casa de cambio.

## Adjuntos

Se pueden adjuntar a tareas, proyectos y sesiones de entrenamiento imágenes JPG/JPEG, PNG, GIF y BMP (máximo 10 MiB), audio MP3, WAV, AIFF y M4A (25 MiB), y video MP4/M4V (45 MiB). Cada elemento admite hasta 10 archivos. La app comprueba extensión, tamaño y firma binaria; los videos recomendados usan MP4 con H.264 y AAC. H.265, WebM, MKV y AVI no se reproducen en el reproductor integrado; guarda una copia para abrirlos en otra aplicación.

Ejecuta supabase/10_adjuntos.sql en el SQL Editor del proyecto. El script crea el bucket privado ttachments con límite de 50 MiB y políticas de lectura, subida y borrado para la carpeta del usuario autenticado. El cliente nunca usa service_role. Valida el aislamiento con dos cuentas: A sube un adjunto; B no debe ver su fila y, con su propio token, un GET a /storage/v1/object/authenticated/attachments/<ruta-de-A> debe ser rechazado. Las subidas mayores de 6 MB usan transferencia estándar; Supabase recomienda TUS para archivos grandes.

Los archivos descargados se guardan en un directorio temporal privado de la sesión y se borran al cerrar sesión o la aplicación. Si se elimina una fila o un objeto directamente desde el panel de Supabase, pueden quedar archivos huérfanos. PDF y otros documentos no están admitidos. El límite del plan gratuito es 50 MB por archivo, aunque el bucket restringe a 50 MiB y la aplicación aplica límites menores por formato.




## Tema visual

La interfaz usa fondo `#141416`, superficies `#1c1c1f`, tinte cálido `#2a1619`, texto blanco y carmesí `#d62839` (variantes `#e63946` y `#7a0f1a`). Errores: coral `#ff6b6b`; éxito `#3ecf8e`; advertencia `#f5a524`.

Las clases se definen en `src/main/resources/com/example/proyectolp/css/theme.css`. En una pantalla nueva, agrega `stylesheets="@../css/theme.css"` al nodo raíz del FXML dentro de `ui/` y reutiliza sus clases. SceneBuilder resuelve la ruta relativa al FXML; abre ese archivo para previsualizarlo. Montserrat TTF es opcional: descarga los pesos estáticos Regular, Medium, SemiBold y Bold desde [Google Fonts](https://github.com/google/fonts/tree/main/ofl/montserrat) y guárdalos en `src/main/resources/com/example/proyectolp/fonts/` como `Montserrat-Regular.ttf`, `Montserrat-Medium.ttf`, `Montserrat-SemiBold.ttf` y `Montserrat-Bold.ttf`. Segoe UI y Arial son los respaldos.

## Recuperar contraseña

La recuperación usa un OTP de seis dígitos que Supabase envía por correo. La contraseña actual nunca se solicita ni se envía: Supabase conserva un hash bcrypt irreversible. Tras validar el código, la app cambia la clave con una sesión temporal, la revoca con alcance local y vuelve al login sin iniciar sesión automáticamente.

### Configuración de Supabase

1. En **Authentication → SMTP Settings**, configura SMTP personalizado para entregar correo a usuarios fuera del equipo del proyecto. El proveedor integrado solo envía a miembros del equipo y limita actualmente el proyecto completo a 2 correos por hora. Para permitir hasta 5 correos por hora, configura SMTP personalizado y luego fija **Authentication → Rate Limits → Emails sent** en `5` por hora. El proveedor integrado no permite elevar su cuota a 5; cambiar el cliente no elimina ese límite de Supabase.
2. En **Authentication → Email Templates → Reset Password**, establece:

   **Asunto:** `Código para restablecer tu contraseña`
   **Cuerpo:**

   ```html
   <h2>Restablece tu contraseña</h2><p>Tu código de verificación es:</p><h1>{{ .Token }}</h1><p>Si no lo solicitaste, ignora este correo.</p>
   ```

   `{{ .Token }}` contiene el OTP de seis dígitos; no incluyas `{{ .ConfirmationURL }}` en esta plantilla. La app llama a `/auth/v1/recover` sin `redirect_to`; si el correo abre una página `localhost`, la plantilla de Supabase todavía está usando el enlace de confirmación. Sustituye el cuerpo anterior, guarda los cambios y solicita un correo nuevo: el código aparecerá dentro del mensaje y se escribe en la pantalla de recuperación de la app.
3. Revisa **Authentication → Rate Limits**. Configura el límite agregado de correos a `5` por hora (requiere SMTP personalizado). Supabase aplica por defecto 60 segundos entre solicitudes de recuperación por usuario. La app aplica esa espera y además limita localmente cada cuenta a 5 correos por hora.
4. Ejecuta `supabase/11_recuperacion.sql` después de los scripts existentes. Crea `public.get_account_status`, que devuelve solo el estado de cuenta a PostgREST.
5. Mantén **Confirm email** activado si quieres exigir confirmación. Desactivado, Supabase establece `email_confirmed_at` para las cuentas nuevas y se consideran verificadas.

### Riesgo de enumeración

`RecoveryPolicy.REVEAL_ACCOUNT_EXISTENCE` está en `true` por defecto y diferencia cuenta inexistente, no verificada y verificada; revela si el correo está registrado. La app limita a 5 consultas de existencia por ventana de 10 minutos y no devuelve datos del usuario. Para ocultar el estado, cambia la constante a `false` y recompila: se omite `get_account_status`, se llama siempre a `/auth/v1/recover` y se muestra un mensaje genérico. Los límites del cliente se pueden eludir con instalaciones distintas o clientes modificados; conserva los límites de Supabase y SMTP.

### Prueba manual

1. Con SMTP configurado y una cuenta verificada, abre **¿Olvidó su contraseña?** desde el login.
2. Solicita el código recibido por correo e ingrésalo con una contraseña nueva.
3. Comprueba que la app vuelve al login, la clave nueva permite iniciar sesión y la anterior no.
4. Prueba correo inexistente, cuenta no verificada, código erróneo/caducado, bloqueo al quinto intento y reenvío tras 60 segundos.
5. Para probar el modo privado, cambia `REVEAL_ACCOUNT_EXISTENCE` a `false` temporalmente y valida el mensaje genérico.

Rutas y configuración contrastadas con la documentación oficial de [verify OTP](https://supabase.com/docs/reference/javascript/auth-verifyotp), [plantillas de correo](https://supabase.com/docs/guides/auth/auth-email-templates), [límites de Auth](https://supabase.com/docs/guides/auth/rate-limits) y [SMTP personalizado](https://supabase.com/docs/guides/auth/auth-smtp). No se hicieron peticiones reales: no hay credenciales de proyecto en este entorno.

## Canje de créditos

El canje es una simulación académica: no existe alianza ni canje real con Frisby, Burger King, Little Caesars, Popsy, El Corral o McDonald's. Los valores del catálogo son costos demostrativos en créditos (1 crédito equivale a $2.000 COP en créditos), no precios comerciales. Los cupones solo sirven como comprobante visual dentro del proyecto y no tienen valor real.

Aplica `supabase/12_canje.sql` después de los scripts de autenticación. Crea `merchants`, `merchant_products` y `redemptions`, activa RLS y expone `redeem_product` solo mediante RPC para usuarios autenticados. El saldo del servidor se obtiene sumando `credit_ledger.credits` del usuario. La función bloquea por usuario, valida el costo almacenado en la base de datos y hace idempotente la solicitud por `request_id`.

Para editar el catálogo, modifica las filas del `INSERT` en `12_canje.sql` y vuelve a ejecutarlo. La clave natural `(merchant_id, name)` actualiza descripción, tipo, costo, orden y estado. Verifica productos y enlaces en los sitios oficiales antes de publicar; deja `website_url` en `NULL` mientras no se haya verificado. La app abre enlaces solo con esquema HTTPS, host válido y sin credenciales.

Limitación conocida: los créditos aún pueden insertarse desde el cliente por las políticas actuales de `credit_ledger`; el canje valida el saldo calculado en el servidor, pero no convierte todo el sistema de créditos en un libro mayor protegido contra manipulación desde el cliente.

### Pruebas SQL

Con una sesión autenticada en SQL Editor, verifica el saldo y canjea un producto activo reemplazando los parámetros con valores de prueba:

```sql
select coalesce(sum(credits),0) as balance from public.credit_ledger where user_id=auth.uid();
select public.redeem_product(1,'11111111-1111-4111-8111-111111111111'::uuid);
select public.redeem_product(1,'11111111-1111-4111-8111-111111111111'::uuid); -- mismo cupón, un solo descuento
select * from public.redemptions where user_id=auth.uid() order by created_at desc;
```

Comprueba el aislamiento con las cuentas A y B: inicia la consulta de `redemptions` con el token de B y confirma que no aparecen cupones creados por A. Las pruebas automatizadas del proyecto no usan red: ejecuta `mvnw.cmd test` en Windows o `./mvnw test` en Unix.

### Errores comunes del canje

- **RPC 404:** ejecuta `12_canje.sql` y espera a que PostgREST recargue el esquema.
- **Permission denied:** verifica que el script otorgó `EXECUTE` sobre `redeem_product` a `authenticated`.
- **CHECK de `entry_type`:** revisa que `credit_ledger` acepte `redencion`.
- **Saldo negativo inesperado:** no edites el libro desde varios clientes; los canjes se serializan por usuario en la función.
- **FXML no carga por `&`:** escribe `&amp;` en los atributos XML.
- **Tarjetas antiguas tras cerrar sesión:** cierra sesión por el flujo de la app, que limpia la caché del catálogo.
