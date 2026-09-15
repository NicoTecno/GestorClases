# GestorClases

GestorClases es una aplicación móvil desarrollada en Android (Kotlin) utilizando Jetpack Compose y arquitectura MVVM (Model-View-ViewModel). Está diseñada para profesores que necesitan llevar un control organizado de sus alumnos, clases programadas, horarios y estados de pago.

## Funcionalidades Principales

*   **Gestión de Alumnos:** Creación, edición y visualización de perfiles de alumnos (nombre, estado académico, teléfono).
*   **Gestión de Clases:** Programación de clases con estado dinámico (Reservada, Dada, Cancelada, Pagada).
*   **Calendario Integrado:** Vista mensual y diaria para consultar rápidamente la agenda.
*   **Dashboard Principal:** Resumen de las clases correspondientes al día actual.
*   **Cierre Mensual:** Reporte que calcula el saldo pendiente de clases dadas y no pagadas del mes actual.
*   **Diseño Dinámico:** Interfaz moderna y adaptable con soporte nativo para Modo Claro y Modo Oscuro usando tokens de Material Design 3.

## Arquitectura y Tecnologías

*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Arquitectura:** MVVM (Model-View-ViewModel) con StateFlow.
*   **Base de Datos Local:** Room Database
*   **Navegación:** Compose Navigation

## Cómo ejecutar el proyecto

1. Clonar el repositorio.
2. Abrir el proyecto con Android Studio.
3. Sincronizar los archivos Gradle (hacer clic en "Sync Project with Gradle Files").
4. Asegurarse de tener seleccionado un emulador y presionar `Run (Shift + F10)`.

## Estado del Proyecto

*   **En desarrollo.**
*   *Próximas funcionalidades sugeridas:* Exportar cierre mensual a PDF/Excel e integración de autenticación en la nube.
