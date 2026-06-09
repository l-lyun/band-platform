# band-platform

Spring Boot 기반 밴드 플랫폼 API 서버입니다.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- Gradle

## Getting Started

`.env.example`을 참고해 `.env`를 생성합니다.

```bash
cp .env.example .env
```

MySQL 컨테이너를 실행합니다.

```bash
docker compose up -d mysql
```

애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```
