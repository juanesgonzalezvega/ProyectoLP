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
