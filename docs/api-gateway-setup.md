# Configuración AWS API Gateway — ms-administracion-archivos

Guía paso a paso para registrar y securitizar los endpoints en AWS API Gateway (AWS Academy).  
Use este documento como base para el entregable Word con capturas de pantalla.

## Prerrequisitos

- Microservicio desplegado en EC2 (Docker en puerto 8080).
- IP pública o DNS del EC2 anotada.
- Security Group del EC2 permite tráfico entrante en puerto 8080 (desde API Gateway o 0.0.0.0/0 según restricciones de AWS Academy).
- Tenant Azure AD B2C configurado (ver [azure-ad-b2c-setup.md](azure-ad-b2c-setup.md)).
- Valores de `AZURE_B2C_ISSUER_URI` y `AZURE_B2C_CLIENT_ID`.



## 1. Crear REST API

1. AWS Console → **API Gateway** → **Create API**.
2. Elegir **REST API** (Build) → **New API**.
3. Nombre: `ms-administracion-archivos-api`.
4. Endpoint Type: **Regional**.
5. **Create API**.



## 2. Crear recursos y métodos



### Opción A: Proxy completo

1. Seleccionar el recurso `/` → **Actions** → **Create Resource**.
2. Resource Name: `proxy`, Resource Path: `{proxy+}`, Enable API Gateway CORS: ✓.
3. Seleccionar `{proxy+}` → **Actions** → **Create Method** → **ANY**.
4. Integration type: **HTTP Proxy**.
5. Endpoint URL: `http://{EC2_IP}:8080/{proxy}`.
6. Marcar **Use HTTP Proxy integration**.

Repetir para el recurso raíz `/` con método **ANY** apuntando a `http://{EC2_IP}:8080/`.

### Opción B: Rutas explícitas


| Método | Ruta API Gateway        | Integración HTTP Proxy                             |
| ------ | ----------------------- | -------------------------------------------------- |
| POST   | `/guias`                | `http://`16.59.211.157`:8080/guias`                |
| POST   | `/guias/{id}/subir`     | `http://`16.59.211.157`:8080/guias/{id}/subir`     |
| GET    | `/guias/{id}/descargar` | `http://`16.59.211.157`:8080/guias/{id}/descargar` |
| PUT    | `/guias/{id}`           | `http://`16.59.211.157`:8080/guias/{id}`           |
| DELETE | `/guias/{id}`           | `http://`16.59.211.157`:8080/guias/{id}`           |
| GET    | `/guias`               | `http://`16.59.211.157`:8080/guias`                |
| GET    | `/s3/{bucket}/objects`  | `http://`16.59.211.157`:8080/s3/{bucket}/objects`  |
| *      | `/s3/{proxy+}`          | `http://`16.59.211.157`:8080/s3/{proxy}`           |




## 3. Configurar JWT Authorizer (Azure AD B2C)

1. **Authorizers** → **Create New Authorizer**.
2. Nombre: `AzureB2CAuthorizer`.
3. Type: **JWT**.
4. Identity Source: `$request.header.Authorization`.
5. Issuer: valor de `AZURE_B2C_ISSUER_URI`
  Ejemplo: `https://transportistaguias.b2clogin.com/{TENANT-ID}/B2C_1_signupsignin/v2.0/`
6. Audience: valor de `AZURE_B2C_CLIENT_ID` (app API).
7. **Create**.



## 4. Asociar authorizer a métodos

1. Seleccionar cada método (POST, GET, PUT, DELETE, ANY).
2. **Method Request** → **Authorization**: seleccionar `AzureB2CAuthorizer`.
3. Repetir para todos los métodos expuestos.



## 5. Habilitar CORS (si Postman lo requiere)

1. Seleccionar recurso → **Actions** → **Enable CORS**.
2. Access-Control-Allow-Headers: `Content-Type,Authorization`.
3. Access-Control-Allow-Methods: `GET,POST,PUT,DELETE,OPTIONS`.
4. **Enable CORS and replace existing CORS headers**.



## 6. Deploy API

1. **Actions** → **Deploy API**.
2. Deployment stage: `[New Stage]` → nombre `prod`.
3. Anotar la **Invoke URL**:
  `https://{api-id}.execute-api.{region}.amazonaws.com/prod`

Esta URL es la `api_base_url` para Postman en producción.

## 7. Pruebas con Postman



### Escenarios a documentar (capturas para Word)


| Escenario         | Token            | Endpoint                                           | Resultado esperado |
| ----------------- | ---------------- | -------------------------------------------------- | ------------------ |
| Sin autenticación | Ninguno          | `GET /prod/guias/{id}/descargar`                   | 401 Unauthorized   |
| Rol incorrecto    | `GUIA_DESCARGAR` | `POST /prod/guias`                                 | 403 Forbidden      |
| Rol descarga      | `GUIA_DESCARGAR` | `GET /prod/guias/{id}/descargar`                   | 200 OK             |
| Rol gestión       | `GUIA_GESTION`   | `POST /prod/guias`                                 | 201 Created        |
| Rol gestión       | `GUIA_GESTION`   | `GET /prod/guias?transportista=X&fecha=2026-06-03` | 200 OK             |




### Header requerido

```
Authorization: Bearer {access_token}
```



## 8. Cambio de URLs al desplegar en EC2


| Entorno                         | URL base                                                   |
| ------------------------------- | ---------------------------------------------------------- |
| Local (Docker)                  | `http://localhost:8080`                                    |
| EC2 directo (solo debug)        | `http://{EC2_IP}:8080`                                     |
| Producción (Postman/evaluación) | `https://{api-id}.execute-api.{region}.amazonaws.com/prod` |


> **Importante:** Para la evaluación, las pruebas deben ejecutarse contra la URL del **API Gateway**, no la IP directa del EC2.



## 9. Checklist de verificación post-despliegue

- [ ] Contenedor Docker corriendo en EC2 (`docker ps`).
- [ ] Health check: `curl http://{EC2_IP}:8080/actuator/health` (si está habilitado).
- [ ] API Gateway desplegada en stage `prod`.
- [ ] JWT Authorizer validando tokens de B2C.
- [ ] Postman obtiene token OAuth2 desde B2C.
- [ ] Endpoints responden 401 sin token.
- [ ] Endpoints responden 403 con rol incorrecto.
- [ ] Endpoints responden 200/201 con rol correcto.



## 10. Troubleshooting


| Problema           | Solución                                                                  |
| ------------------ | ------------------------------------------------------------------------- |
| 401 en API Gateway | Verificar Issuer y Audience del authorizer                                |
| 401 en Spring Boot | Verificar `AZURE_B2C_ISSUER_URI` y `AZURE_B2C_CLIENT_ID` en el contenedor |
| 403 Forbidden      | Verificar claim `roles` en el JWT (jwt.ms)                                |
| 502 Bad Gateway    | EC2 no accesible; revisar Security Group y contenedor Docker              |
| Token sin roles    | Reasignar app roles en Azure AD B2C                                       |


