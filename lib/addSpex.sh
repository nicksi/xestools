#!/usr/bin/env bash
mvn install:install-file -Dfile=Spex.jar -DgroupId=org.deckfour.spex -DartifactId=spex -Dversion=1.0-RC2 -Dpackaging=jar -DgeneratePom=true
