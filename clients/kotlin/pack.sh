rm -f strategy.zip
zip -r strategy.zip src Dockerfile pom.xml *.sh -x "*.DS_Store"