# Étape 1 : Compilation de l'application avec Maven et Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (accélère les prochains builds)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source et compiler le fichier .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Image finale légère pour exécuter l'application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copier uniquement le fichier .jar compilé depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Exposer le port utilisé par Spring Boot
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
