./gradlew clean build
set -a
source .env
set +a
./gradlew bootRun --debug-jvm
