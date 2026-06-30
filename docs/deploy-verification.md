# Checklist de verificación post-despliegue

Use esta lista después de desplegar en EC2 y configurar API Gateway.

## 1. Azure AD B2C

- [x] Tenant B2C creado
- [x] App API registrada con roles `GUIA_DESCARGAR` y `GUIA_GESTION`
- [x] App cliente Postman registrada con redirect URI `https://oauth.pstmn.io/v1/callback`
- [x] User flow `B2C_1_signupsignin` creado y asociado a las apps
- [x] Usuario `descargador@test.com` con rol `GUIA_DESCARGAR`
- [x] Usuario `gestor@test.com` con rol `GUIA_GESTION`
- [x] Token JWT contiene claim `roles` (verificar en jwt.ms)

## 2. EC2 y Docker

- [x] Push a `main` ejecutó GitHub Actions sin errores
- [x] Contenedor corriendo: `docker ps` muestra `ms-administracion-archivos`
- [x] Variables OAuth presentes en el contenedor:
  ```bash
  docker inspect ms-administracion-archivos | grep AZURE
  ```
- [x] EFS montado en `/mnt/efs` y mapeado al contenedor
- [x] Security Group permite puerto 8080



## 3. API Gateway

- [ ] REST API creada y desplegada en stage `prod`
- [ ] Integración HTTP Proxy apunta a `http://{EC2_IP}:8080`
- [ ] JWT Authorizer configurado con Issuer y Audience de B2C
- [ ] Authorizer asociado a todos los métodos
- [ ] Invoke URL anotada y configurada en Postman environment `prod`



## 4. Pruebas Postman

Ejecutar la colección `postman/ms-administracion-archivos.postman_collection.json`:


| Prueba                         | Usuario/token | Resultado |
| ------------------------------ | ------------- | --------- |
| Sin token                      | Ninguno       | 401       |
| Descargar con `GUIA_GESTION`   | gestor        | 403       |
| Crear con `GUIA_DESCARGAR`     | descargador   | 403       |
| Descargar con `GUIA_DESCARGAR` | descargador   | 200       |
| Crear guía con `GUIA_GESTION`  | gestor        | 201       |
| Subir a S3 con `GUIA_GESTION`  | gestor        | 200       |
| Consultar con `GUIA_GESTION`   | gestor        | 200       |
| Listar S3 con `GUIA_GESTION`   | gestor        | 200       |
| Eliminar con `GUIA_GESTION`    | gestor        | 204       |




## 5. Entregables

- [ ] Repositorio GitHub actualizado con link compartido en AVA
- [ ] Documento Word con capturas de Azure B2C, API Gateway y Postman
- [ ] Archivo ZIP/RAR subido al AVA
- [ ] Grabación Teams (5–10 min) mostrando flujo completo