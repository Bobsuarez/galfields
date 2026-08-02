---
name: java-default-location-hardcoded
description: DEFAULT_LOCATION_NAME = "Bogotá - Chapinero" está hardcodeado en los usecase a propósito — limitación conocida y documentada.
metadata:
  type: convention
  scope: java
  created: 2026-08-02
---

`DEFAULT_LOCATION_NAME = "Bogotá - Chapinero"` aparece como constante en los usecase que tocan inventario (`ApplyStockAdjustmentsUseCase`, `ProductUseCase`). Toda escritura de inventario del sistema se hace contra esa ubicación.

**Por qué:** el negocio tiene una sola sede. No existe soporte multi-ubicación en ninguna parte del sistema — ni en la API, ni en el POS de escritorio, ni en la app móvil — así que no hay nada que pueda enviar una ubicación distinta. Hacerla configurable hoy sería añadir un punto de variación que ningún llamador usa (KISS). Está documentado en `backend/pos_transactions/CLAUDE.md` como gotcha indispensable: renombrar o borrar esa ubicación rompe la creación de productos.

**Cómo aplicar:** no reportarlo como constante mágica, valor hardcodeado, ni configuración faltante. Si en algún momento se agrega soporte multi-sede real, esta memoria queda obsoleta y hay que borrarla — mientras tanto, la duplicación de la constante entre dos usecase es aceptada y tampoco se reporta como DRY.
