
FROM maven:3.9.6-eclipse-temurin-17 as build

WORKDIR /app

# 更换 Maven 镜像源
RUN echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\
    <mirrors>\
    <mirror>\
    <id>alimaven</id>\
    <name>aliyun maven</name>\
    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>\
    <mirrorOf>central</mirrorOf>\
    </mirror>\
    </mirrors>\
    </settings>' > /usr/share/maven/conf/settings.xml

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
