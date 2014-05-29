
HOST := ril-notebook

install:
	./gradlew jar
	${MAKE} sync
	${MAKE} restart

sync:
	ssh ril@${HOST} mkdir -p /research/projects/ril/sargent
	rsync -z qin.sargent.yml ril@${HOST}:/research/projects/ril/sargent/sargent.yml
	rsync -rz build/libs/* ril@${HOST}:/research/projects/ril/sargent/

restart:
	ssh root@${HOST} "service sargent restart"

setup:
	rsync -z init.d/sargent root@${HOST}:/etc/rc.d/init.d/sargent
	ssh root@${HOST} "yum install daemonize -y"
	ssh root@${HOST} "chmod 755 /etc/rc.d/init.d/sargent"
	ssh root@${HOST} "chkconfig sargent on"
