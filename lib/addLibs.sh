#!/usr/bin/env bash
mvn install:install-file -Dfile=Spex.jar -DgroupId=org.deckfour.spex -DartifactId=spex -Dversion=1.0-RC2 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=XESLite.jar -DgroupId=org.processmining.xesLite -DartifactId=xeslite -Dversion=6.5.155 -Dpackaging=jar -DgeneratePom=true
