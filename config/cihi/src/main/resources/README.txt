DEPLOY INSTRUCTIONS

1. setup

mkdir ~/mapping
mkdir ~/mapping/{code,data}
cd ~/mapping/code
git clone https://github.com/IHTSDO/OTF-Mapping-Service.git .
mvn -Dconfig.artifactId=mapping-config-wci clean install

2. config

cp ~/mapping/code/config/wci/target/map*zip -d ~/mapping/config
# edit the "EDIT_THIS" and security entries and paths

3. data

cd ~/mapping/data
wget https://wci1.s3.amazonaws.com/TermServer/mapDemoData.zip
unzip mapDemoData.zip
/bin/rm -f mapDemoData.zip

4. Demo build

cd ~/mapping/code/integration-tests
mvn install -Preset -Drun.config=/home/ec2-tomcat/mapping/config/config.properties -DskipTests=false -Dmaven.home=/project/maven-current


REDEPLOY INSTRUCTIONS

cd ~/mapping/code
git pull
mvn -Dconfig.artifactId=mapping-config-wci clean install

/bin/rm -rf /var/lib/tomcat8/work/Catalina/localhost/mapping-rest
/bin/rm -rf /var/lib/tomcat8/webapps/mappng-rest
/bin/rm -rf /var/lib/tomcat8/webapps/mapping-rest.war
/bin/rm -rf /var/lib/tomcat8/work/Catalina/localhost/mapping-webapp
/bin/rm -rf /var/lib/tomcat8/webapps/mappng-webapp
/bin/rm -rf /var/lib/tomcat8/webapps/mapping-webapp.war.war

/bin/rm -rf /var/lib/tomcat8/indexes/map/*
cd ~/mapping/code/integration-tests
mvn install -Preset -Drun.config=/home/ec2-tomcat/mapping/config/config.properties -DskipTests=false -Dmaven.home=/project/maven-current


/bin/cp -f ~/mapping/code/rest/target/mapping-rest*war /var/lib/tomcat8/webapps/mapping-rest.war
/bin/cp -f ~/mapping/code/webapp/target/mapping-webapp*war /var/lib/tomcat8/webapps/mapping-webapp.war

cd ~/mapping/code/admin/updatedb
mvn clean install -Drun.config=/home/ec2-tomcat/mapping/config/config.properties -Dhibernate.hbm2ddl.auto=update

cd ~/mapping
echo "A" | unzip ~/mapping/code/config/wci/target/mapping*zip "bin/*"


mkdir data/doc
mkdir data/doc/archive
chmod -R ga+rwx data/doc
