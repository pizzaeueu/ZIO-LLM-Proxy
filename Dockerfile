FROM sbtscala/scala-sbt:eclipse-temurin-21.0.6_7_1.10.11_3.6.4 as builder

WORKDIR /app

COPY build.sbt .
COPY project ./project
COPY src ./src
COPY llm ./llm
COPY core ./core
COPY mcp ./mcp
COPY pii ./pii


RUN sbt assembly && find /app/target

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y docker.io

COPY --from=builder /app/target/scala-*/zio-llm-proxy-*.jar /app/app.jar

ENTRYPOINT ["java", "-cp", "/app/app.jar", "com.github.pizzaeueu.Main"]
