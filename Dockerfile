FROM eclipse-temurin:21-jdk AS buildstage

RUN apt-get update && apt-get install -y maven

WORKDIR /app

COPY pom.xml .
COPY src /app/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=buildstage /app/target/ms-administracion-archivos-1.0.0.jar /app/app.jar

EXPOSE 8080

RUN mkdir -p /app/efs

ENV EFS_PATH=/app/efs
ENV AWS_REGION=us-east-1

CMD ["java", "-jar", "/app/app.jar"]
