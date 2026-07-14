FROM maven:3.9.12-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY lostfound-backend/pom.xml lostfound-backend/pom.xml
RUN mvn -pl lostfound-backend -am dependency:go-offline -B

COPY lostfound-backend/src lostfound-backend/src
RUN mvn -pl lostfound-backend -am -DskipTests package -B

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV LOSTFOUND_UPLOAD_DIR=/app/uploads

RUN mkdir -p /app/uploads
COPY --from=build /workspace/lostfound-backend/target/lostfound-backend-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
