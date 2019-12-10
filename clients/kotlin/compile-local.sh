set -ex

mvn package --batch-mode
cp target/aicup2019-jar-with-dependencies.jar output/