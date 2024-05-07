
FROM maven:3.9.6-eclipse-temurin-17 as build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY /src /app/src

RUN mvn package -DskipTests


FROM tomcat:10.1.20-jre17-temurin-jammy as deploy

# 删除webapps目录下的所有文件
RUN rm -rf /usr/local/tomcat/webapps/*

# 将你的.war文件添加到Tomcat的webapps目录
COPY --from=build  /target/dangpay.war /usr/local/tomcat/webapps/ROOT.war

# 暴露8080端口
EXPOSE 8080

# 启动Tomcat
CMD ["catalina.sh", "run"]
