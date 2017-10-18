SETUP

See Confluence:
* https://confluence.ihtsdotools.org/display/MT/Deploy+Instructions
* https://confluence.ihtsdotools.org/display/MT/Redeploy+Instructions

Tomcat Settings
#* config file = /etc/tomcat7/tomcat7.conf (add -Drun.config= parameter here)
* webapps dir = /opt/tomcat8/webapps

MYSQL Settings
* my.cnf - /etc/mysql
** innodb_file_per_table = 1
* files - /var/lib/mysql

Special Notes
* "ihtsdo" user runs tomcat currently, so that index files can be properly managed
  ALTERNATIVE: make sure index files are always writeable by admin user and tomcat user

CLEANUP INDEXES INSTRUCTIONS

su root
# enter password

# To clean up the indexes do this (as root)
/bin/rm -rf /opt/tomcat8/indexes
mkdir /opt/tomcat8/indexes
mkdir /opt/tomcat8/indexes/lucene
mkdir /opt/tomcat8/indexes/lucene/indexes
chmod -R ga+rwx /opt/tomcat8/indexes
chown -R tomcat /opt/tomcat8/indexes
chgrp -R tomcat /opt/tomcat8/indexes

# Ensure the config.properties file used to run the application has this setting
hibernate.search.default.indexBase=/opt/tomcat8/indexes/lucene/indexes

UPDATE DATABASE - after build

cd ~/code/admin/updatedb
mvn -Drun.config=$OTF_MAPPING_CONFIG -PUpdatedb -Dhibernate.hbm2ddl.auto=update clean install

REDEPLOY SCRIPTS - after build
cd
echo "A" | unzip ~/code/config/nlm/target/map*zip "bin/*"

DEPLOY INSTRUCTIONS

# (one time)
git clone https://github.com/IHTSDO/OTF-Mapping-Service.git
# for UAT do this:
git checkout develop

cd ~/code
git pull
mvn -Dconfig.artifactId=mapping-config-nlm clean install

sudo service tomcat stop
/bin/rm -rf /opt/tomcat8/work/Catalina/localhost/mapping-rest
/bin/rm -rf /opt/tomcat8/webapps/mapping-rest
/bin/rm -rf /opt/tomcat8/webapps/mapping-rest.war

/bin/cp -f ~/code/rest/target/mapping-rest*war /opt/tomcat8/webapps/mapping-rest.war

sudo service tomcat start

sleep 40

cd /opt/tomcat8/webapps/mapping-rest
ln -s ~ihtsdo/data/doc
chmod -R ga+rwx ~ihtsdo/data/doc

# access to the tomcat log
/var/log/upstart/tomcat.log
# access to historical logs
/opt/tomcat8/logs

