FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package

FROM tomcat:9.0-jdk17-temurin
ENV CATALINA_HOME=/usr/local/tomcat
WORKDIR /app
COPY --from=build /build/target/fakrudeenmart.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/fakrudeenmart-entrypoint.sh
RUN chmod +x /usr/local/bin/fakrudeenmart-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/fakrudeenmart-entrypoint.sh"]
CMD ["catalina.sh", "run"]
