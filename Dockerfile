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

RUN apt update \
 && apt -y install \
      build-essential \
      curl \
      gdb \
      jq \
      libbz2-dev \
      libffi-dev \
      libgdbm-dev \
      libncursesw5-dev \
      libreadline-dev \
      libsqlite3-dev \
      libssl-dev \
      libssl1.1 \
      lsb-release \
      maven \
      odbc-postgresql \
      odbcinst \
      postgresql-client \
      python3-dev \
      python3-pip \
      sqlite3 \
      tk-dev \
      unixodbc \
      vim \
      wget \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*
 
# Install OpenJDK-11
RUN apt-get update  \
 && apt-get install -y openjdk-11-jre-headless  \
 && apt-get clean

 
ENV PATH = ${PATH}:/apache-maven-${MAVEN_VERSION}/bin

COPY g2redoer/ /app/

#USER 1001

WORKDIR /app
RUN mvn clean install
CMD ["java", "-classpath", "target/g2redoer-1.0.0-SNAPSHOT.jar", "com.senzing.g2.redoer.sz_simple_redoer"]
#ENTRYPOINT ["/app/sz_simple_redoer.py"]
