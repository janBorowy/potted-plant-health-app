./gradlew clean build -x check
set -a
source .env
set +a
./gradlew bootRun
