# MS Administración de Archivos - Guías de Despacho

Microservicio Spring Boot para gestión de guías de despacho con almacenamiento temporal en EFS y persistencia en AWS S3, alineado con la actividad sumativa CDY2204 Semana 3.

## Funcionalidades

- Crear guías de despacho y guardarlas temporalmente en EFS
- Subir guías a S3 organizadas por fecha y transportista: `yyyy-MM-dd/{transportista}/guia-{id}.pdf`
- Descargar guías con validación de permisos por transportista
- Actualizar y eliminar guías
- Consultar historial por transportista y fecha

## Endpoints REST

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/guias` | Crear guía de despacho |
| POST | `/guias/{id}/subir` | Subir guía a S3 |
| GET | `/guias/{id}/descargar?transportista=...` | Descargar guía (valida permisos) |
| PUT | `/guias/{id}` | Actualizar guía |
| DELETE | `/guias/{id}` | Eliminar guía |
| GET | `/guias?transportista=...&fecha=yyyy-MM-dd` | Consultar guías por transportista y fecha |
| GET | `/guias?transportista=...&fecha=...&incluirS3=true` | Consultar historial directamente en S3 |

## Ejemplos curl

```bash
# Crear guía
curl -X POST http://localhost:8080/guias \
  -H "Content-Type: application/json" \
  -d '{"transportista":"transportistaX","pedidoId":"PED-001","descripcion":"Envio Santiago"}'

# Subir a S3
curl -X POST http://localhost:8080/guias/{id}/subir

# Descargar (solo transportista propietario o admin)
curl -OJ "http://localhost:8080/guias/{id}/descargar?transportista=transportistaX"

# Actualizar
curl -X PUT http://localhost:8080/guias/{id} \
  -H "Content-Type: application/json" \
  -d '{"descripcion":"Envio actualizado"}'

# Consultar por transportista y fecha
curl "http://localhost:8080/guias?transportista=transportistaX&fecha=2026-06-03"

# Eliminar
curl -X DELETE http://localhost:8080/guias/{id}
```

## Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `EFS_PATH` | Ruta montaje EFS | `/app/efs` |
| `S3_BUCKET` | Bucket S3 | `ms-guias-bucket` |
| `AWS_REGION` | Región AWS | `us-east-1` |
| `AWS_ACCESS_KEY_ID` | Credencial AWS | - |
| `AWS_SECRET_ACCESS_KEY` | Credencial AWS | - |
| `ADMIN_TRANSPORTISTA` | Transportista con permiso admin | `admin` |

## Ejecución local

```bash
mvn spring-boot:run
```

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

## Checklist video demostrativo (pauta evaluación)

1. **EFS (15 pts):** Mostrar creación de guía y archivo temporal en EFS
2. **Subida S3 (10 pts):** Subir guía y verificar ruta `fecha/transportista/guia-id.pdf`
3. **Modificar S3 (15 pts):** Actualizar guía y verificar reemplazo en S3
4. **Descargar S3 (10 pts):** Descargar con transportista autorizado y rechazo sin permisos
5. **Historial (10 pts):** Consultar por transportista y fecha
6. **Pipeline (20 pts):** Push a main, build Docker Hub y despliegue EC2
7. **Video (20 pts):** Explicar cada requerimiento y su cumplimiento

## Entregables

- Link repositorio Git
- Zip/RAR con documentación
- Video explicativo en AVA
