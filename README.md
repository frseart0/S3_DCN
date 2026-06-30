## Funcionalidades

- Crear guías de despacho y guardarlas temporalmente en EFS
- Subir guías a S3 organizadas por fecha y transportista: `yyyy-MM-dd/{transportista}/guia-{id}.pdf`
- Descargar guías desde EFS o S3
- Actualizar y eliminar guías
- Consultar historial por transportista y fecha
- Autenticación JWT con Azure AD B2C y control de acceso por roles

## Endpoints REST


| Método | Ruta                                                | Descripción                               | Rol requerido    |
| ------ | --------------------------------------------------- | ----------------------------------------- | ---------------- |
| POST   | `/guias`                                            | Crear guía de despacho                    | `GUIA_GESTION`   |
| POST   | `/guias/{id}/subir`                                 | Subir guía a S3                           | `GUIA_GESTION`   |
| GET    | `/guias/{id}/descargar`                             | Descargar guía                            | `GUIA_DESCARGAR` |
| PUT    | `/guias/{id}`                                       | Actualizar guía                           | `GUIA_GESTION`   |
| DELETE | `/guias/{id}`                                       | Eliminar guía                             | `GUIA_GESTION`   |
| GET    | `/guias?transportista=...&fecha=yyyy-MM-dd`         | Consultar guías por transportista y fecha | `GUIA_GESTION`   |
| GET    | `/guias?transportista=...&fecha=...&incluirS3=true` | Consultar historial directamente en S3    | `GUIA_GESTION`   |
| *      | `/s3/`**                                            | Operaciones directas S3                   | `GUIA_GESTION`   |


Todos los endpoints requieren header `Authorization: Bearer {token}` excepto en perfil `local`.

## Seguridad — Azure AD B2C



### Roles


| Rol              | Permisos                                              |
| ---------------- | ----------------------------------------------------- |
| `GUIA_DESCARGAR` | Solo `GET /guias/{id}/descargar`                      |
| `GUIA_GESTION`   | Crear, subir, actualizar, eliminar, consultar y `/s3` |




### Variables de entorno


| Variable               | Descripción                    |
| ---------------------- | ------------------------------ |
| `AZURE_B2C_ISSUER_URI` | Issuer del user flow B2C       |
| `AZURE_B2C_CLIENT_ID`  | Client ID de la aplicación API |


Ejemplo de issuer: `https://{tenant}.b2clogin.com/{tenant-id}/B2C_1_signupsignin/v2.0/`

Guía completa de configuración: [docs/azure-ad-b2c-setup.md](docs/azure-ad-b2c-setup.md)

### Desarrollo local sin B2C

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

El perfil `local` desactiva la validación JWT para pruebas sin Azure.

## Variables de entorno


| Variable                | Descripción       | Default           |
| ----------------------- | ----------------- | ----------------- |
| `EFS_PATH`              | Ruta montaje EFS  | `/app/efs`        |
| `S3_BUCKET`             | Bucket S3         | `ms-guias-bucket` |
| `AWS_REGION`            | Región AWS        | `us-east-1`       |
| `AWS_ACCESS_KEY_ID`     | Credencial AWS    | -                 |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS    | -                 |
| `AZURE_B2C_ISSUER_URI`  | Issuer Azure B2C  | -                 |
| `AZURE_B2C_CLIENT_ID`   | Client ID app API | -                 |




## Docker

```bash
docker build -t ms-administracion-archivos .
docker run -p 8080:8080 \
  -v ./efs:/app/efs \
  -e S3_BUCKET=tu-bucket \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  -e AZURE_B2C_ISSUER_URI=https://... \
  -e AZURE_B2C_CLIENT_ID=... \
  ms-administracion-archivos
```



## API Gateway (producción)

En evaluación, las pruebas deben ejecutarse contra la URL del API Gateway, no la IP directa del EC2.


| Entorno    | URL base                                                   |
| ---------- | ---------------------------------------------------------- |
| Local      | `http://localhost:8080`                                    |
| Producción | `https://{api-id}.execute-api.{region}.amazonaws.com/prod` |


Guía de configuración: [docs/api-gateway-setup.md](docs/api-gateway-setup.md)

## Postman

Importar desde la carpeta `postman/`:

- `ms-administracion-archivos.postman_collection.json`
- `ms-administracion-archivos-local.postman_environment.json` (desarrollo)
- `ms-administracion-archivos-prod.postman_environment.json` (API Gateway)

Configurar OAuth 2.0 en la colección con las credenciales de Azure AD B2C. Iniciar sesión con el usuario correspondiente al rol que desea probar.

## CI/CD (GitHub Actions)

Al hacer push a `main`, el workflow:

1. Ejecuta tests Maven
2. Construye imagen Docker
3. Publica en Docker Hub
4. Despliega en EC2 vía SSH



### Secrets requeridos en GitHub

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`
- `AWS_REGION`
- `S3_BUCKET`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AZURE_B2C_ISSUER_URI`
- `AZURE_B2C_CLIENT_ID`



## Configuración AWS

1. Crear bucket S3 para guías
2. Montar EFS en EC2 en `/mnt/efs` y mapearlo al contenedor
3. Configurar IAM con permisos `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket`
4. Registrar endpoints en API Gateway con JWT Authorizer de Azure B2C



## Documentación


| Documento                 | Ubicación                                                  |
| ------------------------- | ---------------------------------------------------------- |
| Azure AD B2C paso a paso  | [docs/azure-ad-b2c-setup.md](docs/azure-ad-b2c-setup.md)   |
| API Gateway paso a paso   | [docs/api-gateway-setup.md](docs/api-gateway-setup.md)     |
| Checklist post-despliegue | [docs/deploy-verification.md](docs/deploy-verification.md) |


Convertir los documentos Markdown a Word (.docx) con capturas de pantalla para el entregable AVA