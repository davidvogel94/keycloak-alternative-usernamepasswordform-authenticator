# syntax=docker/dockerfile:1

FROM openjdk:16-alpine3.13 as build
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw dependency:go-offline clean package


FROM busybox:1.35.0
LABEL version="1.0.1"
RUN  mkdir /spi
COPY --from=build /target/*.jar /spi

