# Pruebas con Postman — ms-administracion-archivos

Guía para importar la colección, configurar OAuth 2.0 con Microsoft Entra External ID y ejecutar las pruebas de la actividad Semana 6.

## Archivos a importar

Desde la carpeta `postman/` del repositorio:

| Archivo | Uso |
| ------- | --- |
| `ms-administracion-archivos.postman_collection.json` | Colección con todos los endpoints |
| `ms-administracion-archivos-local.postman_environment.json` | Desarrollo local (`localhost:8080`) |
| `ms-administracion-archivos-prod.postman_environment.json` | Producción vía API Gateway |

### Importar en Postman

1. **Import** → arrastrar los 3 archivos JSON.
2. Seleccionar el environment según el entorno:
   - **Local** → perfil Spring `local` desactiva JWT; útil solo para debug sin Azure.
   - **Producción (API Gateway)** → entorno de evaluación.

> Para la entrega, las pruebas deben ejecutarse contra el environment **Producción**, con la Invoke URL del HTTP API Gateway.

---

## Configurar variables de entorno

Edite el environment activo (**Environments** → ojo activado) y complete:

| Variable | Descripción | Ejemplo |
| -------- | ----------- | ------- |
| `api_base_url` | URL base de la API | Local: `http://localhost:8080` · Prod: `https://{api-id}.execute-api.{region}.amazonaws.com/prod` |
| `postman_client_id` | Client ID de la app `postman-client` en Entra | `0eb7029d-1ef3-4257-b1ac-3448c4c66c18` |
| `api_scope` | Scope expuesto por la app API | `https://transportistaguias.onmicrosoft.com/ms-guias/access` |
| `s3_bucket` | Nombre del bucket S3 | `ms-guias-bucket` |
| `guia_id` | Se llena automáticamente al crear una guía | (vacío al inicio) |

Las variables `b2c_tenant` y `b2c_policy` de los JSON de environment son herencia de B2C clásico; con **Entra External ID** configure OAuth manualmente en la colección (siguiente sección).

---

## Configurar OAuth 2.0 (Entra External ID)

La colección hereda autenticación OAuth 2.0 a nivel de colección. Configúrela una vez:

1. Clic en la colección **ms-administracion-archivos** → pestaña **Authorization**.
2. **Type:** OAuth 2.0.
3. Complete los campos:

| Campo | Valor |
| ----- | ----- |
| **Grant Type** | Authorization Code (With PKCE) |
| **Callback URL** | `https://oauth.pstmn.io/v1/callback` |
| **Auth URL** | `https://transportistaguias.ciamlogin.com/828e6edd-0dbe-4ab4-a526-7cd4f4eb69d5/oauth2/v2.0/authorize` |
| **Access Token URL** | `https://transportistaguias.ciamlogin.com/828e6edd-0dbe-4ab4-a526-7cd4f4eb69d5/oauth2/v2.0/token` |
| **Client ID** | `{{postman_client_id}}` (o pegue el Client ID de `postman-client`) |
| **Scope** | `openid {{api_scope}} offline_access` |
| **Client Authentication** | Send as Basic Auth header (o Send client credentials in body, según configuración de la app) |

4. **Get New Access Token** → inicie sesión con el usuario de prueba.
5. **Use Token** para aplicar el token a las peticiones.

> Reemplace `828e6edd-0dbe-4ab4-a526-7cd4f4eb69d5` por su **Tenant ID** si es distinto. Las URLs usan `ciamlogin.com` (Entra External ID), no `b2clogin.com`.

### Usuarios de prueba

| Usuario | Rol en Entra | Uso en Postman |
| ------- | ------------ | -------------- |
| `descargador@test.com` | `GUIA_DESCARGAR` | Pruebas de descarga y escenarios 403 al crear |
| `gestor@test.com` | `GUIA_GESTION` | Flujo completo CRUD + S3 |

**Importante:** obtenga un token **distinto** para cada rol. Si cambia de usuario, pulse **Get New Access Token** e inicie sesión con la cuenta correcta.

### Verificar el token

Copie el access token y péguelo en [jwt.ms](https://jwt.ms). Confirme:

| Claim | Valor esperado |
| ----- | -------------- |
| `iss` | `https://transportistaguias.ciamlogin.com/828e6edd-0dbe-4ab4-a526-7cd4f4eb69d5/v2.0/` |
| `aud` | Client ID de la app **API** (`ms-administracion-archivos-api`) |
| `roles` | `GUIA_DESCARGAR` o `GUIA_GESTION` según el usuario |

Si `roles` no aparece, revise la asignación en **Enterprise applications** (ver [azure-ad-b2c-setup.md](azure-ad-b2c-setup.md)).

---

## Estructura de la colección

```
ms-administracion-archivos
├── Guias
│   ├── Crear guia (GUIA_GESTION)          POST   /guias
│   ├── Subir guia a S3 (GUIA_GESTION)     POST   /guias/{id}/subir
│   ├── Descargar guia (GUIA_DESCARGAR)    GET    /guias/{id}/descargar
│   ├── Actualizar guia (GUIA_GESTION)     PUT    /guias/{id}
│   ├── Consultar guias (GUIA_GESTION)     GET    /guias?transportista=&fecha=
│   └── Eliminar guia (GUIA_GESTION)       DELETE /guias/{id}
├── S3
│   └── Listar objetos bucket (GUIA_GESTION) GET /s3/{bucket}/objects
└── Seguridad - Evidencias
    ├── Sin token - debe retornar 401
    ├── Rol incorrecto - descargar con GUIA_GESTION → 403
    └── Rol incorrecto - crear con GUIA_DESCARGAR → 403
```

Todas las peticiones (excepto la de “Sin token”) heredan el OAuth 2.0 de la colección.

---

## Orden recomendado de ejecución

### 1. Pruebas de seguridad (evidencias para el informe)

Ejecute la carpeta **Seguridad - Evidencias** y tome capturas.

| Request | Token / usuario | HTTP esperado | Significado |
| ------- | --------------- | ------------- | ----------- |
| Sin token - debe retornar 401 | Sin autenticación | **401** | API rechaza acceso anónimo |
| Rol incorrecto - descargar con GUIA_GESTION | `gestor@test.com` | **403** | Gestión no puede descargar |
| Rol incorrecto - crear con GUIA_DESCARGAR | `descargador@test.com` | **403** | Descargar no puede crear |

> Para la prueba 401, el request tiene **Auth: No Auth** explícito; no hace falta quitar el token de la colección manualmente.

### 2. Flujo funcional con `gestor@test.com` (GUIA_GESTION)

1. Obtenga token con **gestor@test.com**.
2. Ejecute en orden:

| # | Request | HTTP esperado |
| - | ------- | ------------- |
| 1 | Crear guia (GUIA_GESTION) | **201** — guarda `guia_id` automáticamente |
| 2 | Subir guia a S3 (GUIA_GESTION) | **200** |
| 3 | Consultar guias (GUIA_GESTION) | **200** |
| 4 | Actualizar guia (GUIA_GESTION) | **200** |
| 5 | Listar objetos bucket (GUIA_GESTION) | **200** |
| 6 | Eliminar guia (GUIA_GESTION) | **204** |

### 3. Prueba de descarga con `descargador@test.com` (GUIA_DESCARGAR)

1. Obtenga token con **descargador@test.com**.
2. Cree una guía y súbala con **gestor** (pasos 1–2 del flujo anterior), o use un `guia_id` existente en el environment.
3. Cambie a token de **descargador**.
4. Ejecute **Descargar guia (GUIA_DESCARGAR)** → **200 OK**.

---

## Matriz completa de resultados esperados

En Postman: **Body** → **raw** → **JSON**. Los requests GET/DELETE no llevan body; use `—`.

| Escenario | Usuario / token | Endpoint | Raw body (ejemplo) | Resultado |
| --------- | --------------- | -------- | ------------------ | --------- |
| Sin autenticación | Ninguno | `GET /guias/{id}/descargar` | — | 401 |
| Rol incorrecto (crear) | `GUIA_DESCARGAR` | `POST /guias` | `{"transportista":"TransX","pedidoId":"PED-999"}` | 403 |
| Rol incorrecto (descargar) | `GUIA_GESTION` | `GET /guias/{id}/descargar` | — | 403 |
| Descargar | `GUIA_DESCARGAR` | `GET /guias/{id}/descargar` | — | 200 |
| Crear guía | `GUIA_GESTION` | `POST /guias` | `{"transportista":"Transportes ABC","pedidoId":"PED-001","descripcion":"Guia de prueba"}` | 201 |
| Subir a S3 | `GUIA_GESTION` | `POST /guias/{id}/subir` | — | 200 |
| Consultar | `GUIA_GESTION` | `GET /guias?transportista=Transportes ABC&fecha=2026-06-29` | — | 200 |
| Actualizar | `GUIA_GESTION` | `PUT /guias/{id}` | `{"descripcion":"Guia actualizada"}` | 200 |
| Listar S3 | `GUIA_GESTION` | `GET /s3/{{s3_bucket}}/objects` | — | 200 |
| Eliminar | `GUIA_GESTION` | `DELETE /guias/{id}` | — | 204 |

Ejemplos formateados para copiar en Postman:

**Crear guía** (`POST /guias`):

```json
{
  "transportista": "Transportes ABC",
  "pedidoId": "PED-001",
  "descripcion": "Guia de prueba"
}
```

**Rol incorrecto — crear** (`POST /guias`, token `descargador@test.com`):

```json
{
  "transportista": "TransX",
  "pedidoId": "PED-999"
}
```

**Actualizar guía** (`PUT /guias/{id}`):

```json
{
  "descripcion": "Guia actualizada"
}
```

> **Consultar:** no requiere body; los filtros van en la URL como query params `transportista` y `fecha` (fecha en formato `YYYY-MM-DD`, idealmente la del día en que se creó la guía).

---

## Header de autorización

Todas las peticiones autenticadas envían:

```
Authorization: Bearer {access_token}
```

Postman lo agrega automáticamente al usar OAuth 2.0 con **Use Token**.

---

## Entornos: Local vs Producción

| Aspecto | Local | Producción (evaluación) |
| ------- | ----- | ------------------------ |
| `api_base_url` | `http://localhost:8080` | `https://{api-id}.execute-api.{region}.amazonaws.com/prod` |
| Spring profile | `local` (sin JWT) o default (con JWT) | JWT activo en EC2 |
| API Gateway | No aplica | HTTP API + JWT Authorizer |
| Uso | Desarrollo | Entrega y grabación Teams |

Para evaluación use **siempre** el environment **Producción** y la URL del API Gateway.

---

## Capturas sugeridas para el informe Word

1. Configuración OAuth 2.0 en la colección (Auth URL, Client ID, Scope).
2. Token obtenido (pestaña Authorization → token visible o jwt.ms con claims `roles`, `aud`, `iss`).
3. Request **Sin token** → respuesta **401**.
4. Request **crear con descargador** → respuesta **403**.
5. Request **descargar con gestor** → respuesta **403**.
6. Request **crear con gestor** → respuesta **201** con body JSON.
7. Request **descargar con descargador** → respuesta **200**.
8. Environment **Producción** mostrando `api_base_url` del API Gateway.

---

## Troubleshooting

| Problema | Causa probable | Solución |
| -------- | -------------- | -------- |
| Error al obtener token en Postman | URLs OAuth incorrectas | Use `ciamlogin.com` y su Tenant ID, no `b2clogin.com` |
| 401 en todas las peticiones | Token expirado o no aplicado | **Get New Access Token** → **Use Token** |
| 401 solo en API Gateway | Issuer/Audience incorrectos en JWT Authorizer | Ver [api-gateway-setup.md](api-gateway-setup.md) |
| 401 en EC2 directo | Secrets Azure incorrectos en contenedor | `docker inspect ms-administracion-archivos \| grep AZURE` |
| 403 inesperado | Token de usuario con rol incorrecto | Verifique `roles` en jwt.ms; vuelva a autenticarse con el usuario correcto |
| 403 al descargar con gestor | Comportamiento esperado | Solo `GUIA_DESCARGAR` puede descargar |
| 403 al crear con descargador | Comportamiento esperado | Solo `GUIA_GESTION` puede crear |
| 502 Bad Gateway | EC2 no accesible desde API Gateway | Revise Security Group puerto 8080 y contenedor Docker |
| `guia_id` vacío en descargar | No se ejecutó Crear guía antes | Ejecute **Crear guia** con gestor primero |
| Consultar guias sin resultados | Fecha/transportista no coinciden | Ajuste query params o use la fecha del día de creación |

---

## Referencias

- Configuración Azure / Entra: [azure-ad-b2c-setup.md](azure-ad-b2c-setup.md)
- API Gateway HTTP API + JWT: [api-gateway-setup.md](api-gateway-setup.md)
- Checklist post-despliegue: [deploy-verification.md](deploy-verification.md)
