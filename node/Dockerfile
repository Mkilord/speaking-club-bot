FROM maven:3.9-eclipse-temurin-22

ENV HOME=/home/app
RUN mkdir -p $HOME

## Установка Maven
#RUN apt-get update && \
#    apt-get install -y maven && \
#    apt-get clean

ADD ./settings.xml /usr/share/maven/ref/
ENV MVN_SETTINGS=/usr/share/maven/ref/settings.xml

RUN mkdir -p $HOME/node
ADD ./node/pom.xml $HOME/node
ADD ./node/src $HOME/node/src
RUN mvn -f $HOME/node/pom.xml -s $MVN_SETTINGS clean package -DskipTests --debug

CMD java -jar $HOME/node/target/node-0.0.1.jar
