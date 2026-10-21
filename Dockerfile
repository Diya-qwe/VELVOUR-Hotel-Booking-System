

FROM maven:3.9-eclipse-temurin-21

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends xvfb libgtk-3-0 libasound2t64 libgl1 libx11-6 libxext6 libxrender1 libxtst6 libxi6 libfontconfig1 && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package

CMD ["xvfb-run", "-a", "mvn", "-B", "javafx:run"]
