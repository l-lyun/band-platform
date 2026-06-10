# Spring Backend Reference

Use this when changing package structure, dependencies, services, entities, repositories, or migrations.

## Package Shape

Prefer feature-first packages:

```text
band.platform
  common
  auth
  member
  profile
```

Inside features, use layers only when useful:

```text
presentation
application
domain
infrastructure
```

## Rules

- Controllers handle HTTP mapping and DTO validation only.
- Application services own transactions and use-case orchestration.
- Domain objects own state changes and invariants.
- Infrastructure wraps persistence, external providers, mail, token libraries, and adapters.
- API DTOs are not JPA entities.
- Prefer Java records for immutable request/response DTOs.
- Do not introduce a shared abstraction until there is a real second use case.

## Persistence

- Use Spring Data JPA repositories for aggregate access.
- Prefer lazy relationships.
- Avoid bidirectional mappings unless needed.
- Add DB uniqueness constraints for login ID and email.
- Avoid relying on `ddl-auto: update` after schema stabilizes.
- Introduce Flyway when the first real table set is implemented.

## Dependency Rule

Before adding a dependency:

1. Check if Spring Boot already provides it.
2. Check whether the project already has an equivalent.
3. Explain why the dependency is needed.
4. Keep paid/cloud/provider-specific dependencies out unless the user confirms.
