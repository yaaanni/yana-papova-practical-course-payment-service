FROM gradle:jdk25-ubi AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle clean build -x test

FROM eclipse-temurin:25-jdk
WORKDIR /app

EXPOSE 8080

RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar paymentService.jar

ENTRYPOINT ["java", "-jar", "paymentService.jar"]
