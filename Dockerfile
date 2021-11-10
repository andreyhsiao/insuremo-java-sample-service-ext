FROM adoptopenjdk/openjdk11 as BUILD
WORKDIR /application
ADD target/*-release.zip /app.zip
RUN jar xvf /app.zip

FROM adoptopenjdk/openjdk11

WORKDIR /application
COPY --from=BUILD /application /application
RUN chmod 775 /application/bin/app

ENV JAVA_OPTS="$JAVA_OPTS "
CMD ["sh", "-c", "bin/app"]
