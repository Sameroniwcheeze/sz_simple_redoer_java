# docker build -t brian/sz_simple_redoer .
# docker run --user $UID -it -v $PWD:/data -e SENZING_ENGINE_CONFIGURATION_JSON brian/sz_simple_redoer

ARG BASE_IMAGE=senzing/senzingapi-runtime:latest
FROM ${BASE_IMAGE}

ENV REFRESHED_AT=2023-06-21

LABEL Name="sam/sz_simple_redoer_java" \
      Maintainer="brianmacy@gmail.com" \
      Version="DEV"

# Install OpenJDK-11
RUN apt-get update  \
 && apt-get install -y openjdk-11-jre-headless  \
 && apt-get -y autoremove \
 && apt-get clean

COPY sz_simple_redoer.py /app/

RUN curl -X GET \
      --output /app/senzing_governor.py \
      https://raw.githubusercontent.com/Senzing/governor-postgresql-transaction-id/main/senzing_governor.py

ENV PYTHONPATH=/opt/senzing/g2/sdk/python:/app

USER 1001

WORKDIR /app
ENTRYPOINT ["/app/sz_simple_redoer.py"]

