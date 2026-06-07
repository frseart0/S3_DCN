## Funcionalidades

- Crear guías de despacho y guardarlas temporalmente en EFS
- Subir guías a S3 organizadas por fecha y transportista: `yyyy-MM-dd/{transportista}/guia-{id}.pdf`
- Descargar guías desde EFS o S3
- Actualizar y eliminar guías
- Consultar historial por transportista y fecha

## Endpoints REST


| Método | Ruta                                                | Descripción                               |
| ------ | --------------------------------------------------- | ----------------------------------------- |
| POST   | `/guias`                                            | Crear guía de despacho                    |
| POST   | `/guias/{id}/subir`                                 | Subir guía a S3                           |
| GET    | `/guias/{id}/descargar`                             | Descargar guía                            |
| PUT    | `/guias/{id}`                                       | Actualizar guía                           |
| DELETE | `/guias/{id}`                                       | Eliminar guía                             |
| GET    | `/guias?transportista=...&fecha=yyyy-MM-dd`         | Consultar guías por transportista y fecha |
| GET    | `/guias?transportista=...&fecha=...&incluirS3=true` | Consultar historial directamente en S3    |


## Variables de entorno


| Variable                | Descripción      | Default           |
| ----------------------- | ---------------- | ----------------- |
| `EFS_PATH`              | Ruta montaje EFS | `/app/efs`        |
| `S3_BUCKET`             | Bucket S3        | `ms-guias-bucket` |
| `AWS_REGION`            | Región AWS       | `us-east-1`       |
| `AWS_ACCESS_KEY_ID`     | Credencial AWS   | -                 |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS   | -                 |


## Docker

```bash
docker build -t ms-administracion-archivos .
docker run -p 8080:8080 \
  -v ./efs:/app/efs \
  -e S3_BUCKET=tu-bucket \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  ms-administracion-archivos
```

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

## Configuración AWS

1. Crear bucket S3 para guías
2. Montar EFS en EC2 en `/mnt/efs` y mapearlo al contenedor
3. Configurar IAM con permisos `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket`



prueba aaa github actions