# docker build -t brian/sz_simple_redoer .
# docker run --user $UID -it -v $PWD:/data -e SENZING_ENGINE_CONFIGURATION_JSON brian/sz_simple_redoer

ARG BASE_IMAGE=senzing/senzingapi-runtime:latest

FROM ${BASE_IMAGE}

ENV REFRESHED_AT=2023-06-21

LABEL Name="sameroni/sz_simple_redoer_java" \
      Maintainer="brianmacy@gmail.com" \
      Version="DEV"


# Run as "root" for system installation.

USER root

ENV PATH = ${PATH}:/apache-maven-${MAVEN_VERSION}/bin

COPY g2redoer /build
WORKDIR /build

RUN apt-get update \
 && apt-get -y install postgresql-client \
 && apt-get -y install openjdk-11-jre-headless maven \
 && apt-get -y clean \
 && ls \
 && mvn clean install \
 && mkdir /app \
 && cp target/g2redoer-1.0.0-SNAPSHOT.jar /app/ \
 && cd / \
 && rm -rf /build \
 && apt-get -y remove maven \
 && apt-get -y autoremove \
 && apt-get -y clean

WORKDIR /app
CMD ["java", "-classpath", "g2redoer-1.0.0-SNAPSHOT.jar", "com.senzing.g2.redoer.sz_simple_redoer"]

