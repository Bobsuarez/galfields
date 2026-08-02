---
name: java-seeded-admin-credentials
description: V9__employee_auth.sql siembra admin/admin123. Riesgo conocido y documentado — reportar solo bajo condiciones específicas, no en cada corrida.
metadata:
  type: risk
  scope: java
  created: 2026-08-02
---

La migración `V9__employee_auth.sql` siembra un empleado administrador de arranque: usuario `admin`, contraseña `admin123` (hash bcrypt embebido en el SQL).

**Por qué:** es un problema de huevo y gallina, no un descuido. `/api/employees` exige un JWT con autoridad `ADMIN` para crear empleados, pero conseguir ese JWT exige iniciar sesión como administrador, y no hay otro endpoint capaz de crear el primero. La credencial está documentada explícitamente en `CLAUDE.md` y en el comentario de la propia migración, junto con la instrucción de rotarla tras el primer login real.

**Cómo aplicar:** **no** reportarlo como hallazgo nuevo en cada auditoría — es conocido, deliberado y ya documentado. Reportar únicamente si se cumple alguna de estas condiciones:

- Se detecta que la credencial sigue activa/sin rotar en una configuración que llega a producción.
- Alguien añade otra credencial sembrada en una migración nueva (ahí sí es hallazgo nuevo, y esta excepción no lo cubre).
- El endpoint de creación de empleados deja de exigir `ADMIN`, lo que convertiría la cuenta sembrada en una vía de escalada real.

Si nada de eso aplica, guardar silencio. Esta memoria existe justamente para que el hallazgo no se repita cada corrida hasta volverse ruido.
