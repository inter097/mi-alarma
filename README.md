# Mi Alarma

Aplicación Android nativa (Kotlin + Jetpack Compose, Material 3, diseño oscuro)
para gestionar alarmas personales con disparo exacto y exento de Doze.

## Características

- **Lista de alarmas**: hora en grande, etiqueta, días de repetición (L-D) y
  switch para activar/desactivar cada alarma.
- **Crear / editar / eliminar alarmas**: selector de hora (TimePicker de
  Material 3), selección de días de la semana, etiqueta, vibración on/off y
  selección de tono (selector de tonos del sistema para alarmas).
- **Alarmas exactas**: programadas con `AlarmManager.setAlarmClock()`, exentas
  de las restricciones de Doze/App Standby y que muestran el icono de alarma
  del sistema en la barra de estado.
- **Sonido y pantalla de alarma**: al dispararse, un `BroadcastReceiver`
  arranca un `Service` en primer plano que reproduce el tono en bucle, vibra
  (si está activado) y muestra una `Activity` a pantalla completa sobre la
  pantalla de bloqueo, con botones **Posponer** y **Descartar**.
- **Persistencia**: Room. Las alarmas activas se reprograman automáticamente
  tras reiniciar el dispositivo (`BOOT_COMPLETED`).
- **Permisos en runtime**: alarmas exactas (`SCHEDULE_EXACT_ALARM` /
  `USE_EXACT_ALARM`), notificaciones (`POST_NOTIFICATIONS`, Android 13+) y
  mostrar la alarma sobre la pantalla de bloqueo (`USE_FULL_SCREEN_INTENT`).
- **Pantalla de ajustes de batería**: explica cómo desactivar la optimización
  de batería y activar "Inicio automático" (Autostart), especialmente en
  dispositivos Xiaomi/Redmi/POCO con HyperOS o MIUI, además de Huawei, Oppo,
  Vivo y otros fabricantes.

## Estructura del proyecto

```
app/src/main/java/com/mialarma/app/
├── MiAlarmaApplication.kt        # Inicializa el canal de notificaciones
├── data/                         # Entidad Room, DAO, base de datos, repositorio
├── alarm/                        # AlarmScheduler, AlarmReceiver, AlarmService, BootReceiver
├── ui/
│   ├── MainActivity.kt           # Navegación y solicitud de permisos
│   ├── AlarmRingingActivity.kt   # Pantalla completa cuando suena la alarma
│   ├── screens/                  # Lista, edición y permisos
│   └── theme/                    # Tema Material 3 oscuro
└── viewmodel/                    # AlarmViewModel
```

## Compilar

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

El `release` se firma con el keystore de depuración (`debug.keystore`), por lo
que se puede compilar sin necesidad de configurar secretos.

## Integración continua

El workflow [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml)
compila el APK de `release` con `./gradlew assembleRelease`, lo publica como
**artifact** del workflow y, en cada push a `main` o tag `v*`, crea una
**GitHub Release** adjuntando el APK.
