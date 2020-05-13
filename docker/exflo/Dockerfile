FROM gradle:6.3.0-jdk11 AS build

WORKDIR /build

COPY ./ /build

RUN ./gradlew :plugin:build

FROM hyperledger/besu:1.4.4

RUN mkdir -p /opt/besu/plugins

COPY --from=build /build/plugin/build/libs/*.jar /opt/besu/plugins/