# Libraries that should be installed in your maven local repository:
# grep "mvn install" ../pom.xml

mvn install:install-file -Dfile=lib/JMMC-MAN-2220-0001-doc.jar -DgroupId=fr.jmmc.smprun-doc -DartifactId=smprun-doc -Dversion=1.0.0 -Dpackaging=jar

