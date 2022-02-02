FROM busybox

RUN mkdir /spi
COPY /target/*.jar /spi

