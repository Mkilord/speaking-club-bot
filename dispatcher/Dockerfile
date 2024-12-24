FROM maven:3.9-eclipse-temurin-21

ENV HOME=/home/app
RUN mkdir -p $HOME

## Установка Maven
#RUN apt-get update && \
#    apt-get install -y maven && \
#    apt-get clean

ADD ./settings.xml /usr/share/maven/ref/
ENV MVN_SETTINGS=/usr/share/maven/ref/settings.xml

RUN mkdir -p $HOME/dispatcher
ADD ./dispatcher/pom.xml $HOME/dispatcher
ADD ./dispatcher/src $HOME/dispatcher/src
RUN mvn -f $HOME/dispatcher/pom.xml -s $MVN_SETTINGS clean package --debug

CMD java -jar $HOME/dispatcher/target/dispatcher-0.0.1.jar
