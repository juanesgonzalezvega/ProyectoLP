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
