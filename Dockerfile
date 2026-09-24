FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM tomcat:9.0-jdk17-temurin
ENV CATALINA_HOME=/usr/local/tomcat
WORKDIR ${CATALINA_HOME}
COPY --from=build /app/target/fakrudeenmart.war ${CATALINA_HOME}/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
