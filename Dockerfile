FROM tomcat:10.1.20-jre17-temurin-jammy

# 删除webapps目录下的所有文件
RUN rm -rf /usr/local/tomcat/webapps/*

# 将你的.war文件添加到Tomcat的webapps目录
COPY /target/fivedayTieba.war /usr/local/tomcat/webapps/ROOT.war

# 暴露8080端口
EXPOSE 8080

# 启动Tomcat
CMD ["catalina.sh", "run"]
