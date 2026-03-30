FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# copy only what is needed to cache dependencies
COPY pom.xml .
RUN mvn -B -f pom.xml -DskipTests dependency:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B -f pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*-all.jar ./app.jar
# copy static files so the embedded HttpServer can serve them from /app/static
COPY static ./static
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
