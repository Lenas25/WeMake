# Implementación de Date & Time Pickers en WeMake

## Resumen
Se han implementado Date & Time pickers de Material Design en la sección de tiempo y fecha de `CreateTaskActivity`.

## Características Implementadas

### 1. DatePickerDialog
- **Ubicación**: Chip con ID `chip_date`
- **Funcionalidad**: Permite seleccionar una fecha futura
- **Restricción**: Fecha mínima establecida como hoy
- **Formato**: dd/MM/yyyy (ej: 19/09/2025)

### 2. TimePickerDialog
- **Ubicación**: Chip con ID `chip_time`
- **Funcionalidad**: Permite seleccionar hora y minutos
- **Formato**: 24 horas (ej: 14:30)

## Código Implementado

### Variables Agregadas
```java
private Calendar selectedDate;
private Calendar selectedTime;
private SimpleDateFormat dateFormat;
private SimpleDateFormat timeFormat;
```

### Métodos Principales

#### `showDatePicker()`
- Crea y muestra un DatePickerDialog
- Actualiza el texto del chip con la fecha seleccionada
- Establece fecha mínima como hoy

#### `showTimePicker()`
- Crea y muestra un TimePickerDialog
- Actualiza el texto del chip con la hora seleccionada
- Usa formato de 24 horas

### Listeners Actualizados
- `chipDate.setOnClickListener()` → llama a `showDatePicker()`
- `chipTime.setOnClickListener()` → llama a `showTimePicker()`

## Flujo de Usuario

1. Usuario toca el chip de fecha → se abre DatePickerDialog
2. Usuario selecciona fecha → chip se actualiza con la fecha
3. Usuario toca el chip de tiempo → se abre TimePickerDialog
4. Usuario selecciona hora → chip se actualiza con la hora
5. Al guardar la tarea → se incluyen fecha y hora en la información

## Beneficios

- **UX Mejorada**: Pickers nativos de Android con Material Design
- **Validación**: Fecha mínima establecida para evitar fechas pasadas
- **Consistencia**: Formato estándar para fecha y hora
- **Accesibilidad**: Pickers nativos son más accesibles que inputs manuales

## Próximos Pasos Sugeridos

1. Integrar con base de datos para persistir fecha y hora
2. Agregar validación de fecha límite máxima
3. Implementar notificaciones basadas en fecha/hora seleccionada
4. Agregar opción para seleccionar "Sin fecha límite"
