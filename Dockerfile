FROM openjdk:8-alpine

COPY ./ /root/

WORKDIR /root/

EXPOSE 3000

CMD ["java", "-Dconf=./config.edn", "-Dlogback.configurationFile=./logback.xml", "-jar", "mtool-web.jar"]
