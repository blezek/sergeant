
HOST := ril-notebook

install:
	./gradlew jar
	${MAKE} sync
	${MAKE} restart

sync:
	ssh ril@${HOST} mkdir -p /research/projects/ril/sergeant
	ssh ril@${HOST} touch /research/projects/ril/sergeant/sergeant-env.sh
	rsync -z init.d/sergeantw ril@${HOST}:/research/projects/ril/sergeant/
	# rsync -z qin.sergeant.yml ril@${HOST}:/research/projects/ril/sergeant/sergeant.yml
	rsync -rz build/libs/* ril@${HOST}:/research/projects/ril/sergeant/

restart:
	ssh root@${HOST} "service sergeant restart"

setup:
	rsync -z init.d/sergeant root@${HOST}:/etc/rc.d/init.d/sergeant
	ssh root@${HOST} "yum install daemonize -y"
	ssh root@${HOST} "chmod 755 /etc/rc.d/init.d/sergeant"
	ssh root@${HOST} "chkconfig sergeant on"
