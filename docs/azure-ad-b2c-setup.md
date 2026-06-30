# Configuración de identidad Azure

Guía paso a paso para configurar autenticación JWT y roles requeridos por la actividad Semana 6.

## Importante: Azure AD B2C ya no está disponible para cuentas nuevas

Desde el **1 de mayo de 2025**, Microsoft dejó de permitir la creación de **nuevos tenants Azure AD B2C** para clientes que nunca tuvieron el servicio. Si al intentar crear un tenant aparece:

> *Azure AD B2C is no longer available for new sales; hence, new tenants cannot be created. Please explore Microsoft Entra External ID...*

---

## Microsoft Entra External ID

### 1. Crear tenant externo

1. Ingresar a [Microsoft Entra admin center](https://entra.microsoft.com).
2. **Entra ID** → **Overview** → **Manage tenants** → **Create**.
3. Seleccionar **External** → **Continue**.
4. Elegir una opción:
  - **Trial de 30 días** — no requiere suscripción Azure (útil si no tiene Azure for Students).
  - **Use Azure Subscription** — requiere suscripción Azure activa.
5. Completar:
  - **Tenant Name:** `Transportista Guias` (ejemplo)
  - **Domain Name:** `transportistaguias` → dominio: `transportistaguias.onmicrosoft.com`
  - **Country/Region:** según corresponda (no se puede cambiar después)
6. **Review + Create**. La creación puede tardar hasta 30 minutos.

### 2. Obtener datos del tenant

1. Cambiar al tenant externo: icono de directorio (esquina superior derecha) → **Switch**.
2. **Entra ID** → **Overview** → anotar:
  - **Tenant ID** (GUID) → `{TENANT-ID}`→`828e6edd-0dbe-4ab4-a526-7cd4f4eb69d5`
  - **Primary domain** → `transportistaguias.onmicrosoft.com`
  - **Tenant name** → usado en URLs `ciamlogin.com`

### 3. Registrar aplicación API (Spring Boot)

1. **Entra ID** → **App registrations** → **New registration**.
2. Nombre: `ms-administracion-archivos-api`.
3. Supported account types: **Accounts in this organizational directory only**.
4. Redirect URI: dejar vacío (es una API).
5. **Register** y anotar el **Application (client) ID** → valor de `AZURE_B2C_CLIENT_ID`.

#### 3.1 Exponer scope de API

1. En la app API → **Expose an API**.
2. **Set** Application ID URI (si se solicita): `api://{API-CLIENT-ID}` o `https://transportistaguias.onmicrosoft.com/ms-guias`.     a
3. **Add a scope**:
  - Scope name: `access`
  - Who can consent: **Admins and users**
  - Display name: `Acceso API Guías`
  - Description: `Acceso a ms-administracion-archivos`
4. Anotar el scope completo, por ejemplo: `api://{API-CLIENT-ID}/access`. -> [https://transportistaguias.onmicrosoft.com/ms-guias/access](https://transportistaguias.onmicrosoft.com/ms-guias/access)

#### 3.2 Crear App Roles

1. **App roles** → **Create app role**:


| Display name     | Allowed member types | Value            | Description                                        |
| ---------------- | -------------------- | ---------------- | -------------------------------------------------- |
| Descargar Guías  | Users/Groups         | `GUIA_DESCARGAR` | Solo endpoint de descarga                          |
| Gestión de Guías | Users/Groups         | `GUIA_GESTION`   | Crear, subir, actualizar, eliminar, consultar y S3 |


### 4. Registrar aplicación cliente (Postman)

1. **New registration** → Nombre: `postman-client`.
2. Tipo: **Single-page application (SPA)** o **Public client/native**.
3. Redirect URI: `https://oauth.pstmn.io/v1/callback`.
4. **Register** y anotar el **Client ID** del cliente Postman. -> 0eb7029d-1ef3-4257-b1ac-3448c4c66c18

#### 4.1 Permisos API

1. **API permissions** → **Add a permission** → **My APIs**.
2. Seleccionar `ms-administracion-archivos-api`.
3. Marcar el scope `access` → **Add permissions**.
4. **Grant admin consent** (si aplica).-----------------------------------------------------

### 5. User flow (flujo de registro e inicio de sesión)

1. **External Identities** → **User flows** → **New user flow**.
2. Tipo: **Sign up and sign in**.
3. Nombre: `SignUpSignIn`
4. Identidad local: **Email with password**.
5. Atributos: Display Name, Email Address.
6. **Create**.

#### 5.1 Asociar aplicaciones al user flow

1. Abrir el user flow → **Applications**.
2. Agregar `postman-client` y `ms-administracion-archivos-api`.

### 6. Crear usuarios de prueba

1. **Entra ID** → **Users** → **New user** → **Create new user**.
2. Crear `descargador@test.com` (cuenta local con contraseña). Puqa692761
3. Crear `gestor@test.com` (cuenta local con contraseña).

#### 6.1 Asignar roles

1. **Entra ID** → **Enterprise applications** → buscar `ms-administracion-archivos-api`.
2. **Users and groups** → **Add user/group**.
3. Asignar `descargador@test.com` → rol `GUIA_DESCARGAR`.
4. Asignar `gestor@test.com` → rol `GUIA_GESTION`.

> **Nota:** Verificar que el claim `roles` aparezca en el token JWT decodificado en [jwt.ms](https://jwt.ms). Si no aparece, revise la asignación en Enterprise applications y el manifest de la app API.

### 7. Variables de entorno para Spring Boot

El backend usa OAuth2 Resource Server estándar; **no requiere cambios de código** al migrar de B2C a External ID, solo actualizar valores:


| Variable               | Valor (Entra External ID)                          |
| ---------------------- | -------------------------------------------------- |
| `AZURE_B2C_CLIENT_ID`  | Client ID de la app API                            |
| `AZURE_B2C_ISSUER_URI` | `https://{tenant}.ciamlogin.com/{TENANT-ID}/v2.0/` |


Ejemplo:

```
AZURE_B2C_ISSUER_URI=https://transportistaguias.ciamlogin.com/12345678-abcd-efgh-ijkl-1234567890ab/v2.0/
AZURE_B2C_CLIENT_ID=87654321-dcba-hgfe-lkji-0987654321ba
```

 Configurar en GitHub Secrets: `AZURE_B2C_ISSUER_URI`, `AZURE_B2C_CLIENT_ID`.

### 8. Obtener token en Postman (OAuth 2.0)

1. Authorization → Type: **OAuth 2.0**.
2. Grant Type: **Authorization Code (With PKCE)**.
3. **Auth URL:**
  ```
   https://transportistaguias.ciamlogin.com/{TENANT-ID}/oauth2/v2.0/authorize
  ```
4. **Access Token URL:**
  ```
   https://transportistaguias.ciamlogin.com/{TENANT-ID}/oauth2/v2.0/token
  ```
5. **Client ID:** ID de `postman-client`.
6. **Scope:** `openid api://{API-CLIENT-ID}/access offline_access`
  (ajuste según el scope definido al exponer la API).
7. **Callback URL:** `https://oauth.pstmn.io/v1/callback`.
8. Obtener token e iniciar sesión con el usuario del rol que desea probar.

### 9. Verificación

Decodifique el token en [jwt.ms](https://jwt.ms) y confirme:


| Claim   | Valor esperado                                     |
| ------- | -------------------------------------------------- |
| `aud`   | Client ID de la app API (`AZURE_B2C_CLIENT_ID`)    |
| `roles` | `GUIA_DESCARGAR` o `GUIA_GESTION` según el usuario |
| `iss`   | Coincide con `AZURE_B2C_ISSUER_URI`                |


Pruebas de autorización:


| Escenario                                       | Resultado esperado |
| ----------------------------------------------- | ------------------ |
| Sin token                                       | 401 Unauthorized   |
| `GUIA_DESCARGAR` en `POST /guias`               | 403 Forbidden      |
| `GUIA_DESCARGAR` en `GET /guias/{id}/descargar` | 200 OK             |
| `GUIA_GESTION` en `POST /guias`                 | 201 Created        |


### 10. API Gateway (AWS)

El JWT Authorizer funciona igual que con B2C; solo cambia el **Issuer**:

- **Issuer:** valor de `AZURE_B2C_ISSUER_URI` (`ciamlogin.com`)
- **Audience:** valor de `AZURE_B2C_CLIENT_ID`

Ver [api-gateway-setup.md](api-gateway-setup.md) para el resto de la configuración.

---

## Referencias

- [Microsoft Entra External ID — Overview](https://learn.microsoft.com/en-us/entra/external-id/external-identities-overview)
- [Create an external tenant](https://learn.microsoft.com/en-us/entra/external-id/customers/how-to-create-external-tenant-portal)
- [Azure AD B2C end of sale FAQ](https://learn.microsoft.com/en-us/azure/active-directory-b2c/faq)

