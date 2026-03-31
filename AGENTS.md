# AGENTS.md

This repository is a multi-module Spring MVC application. Treat the conventions
below as the project baseline when reading, generating, or editing code.

## Stack

- Java 21
- Spring MVC
- JSP + JSTL + Spring form tags
- Tailwind CSS
- Minimal JavaScript
- JDBC
- PostgreSQL in runtime
- Flyway migrations
- JUnit 5, Mockito, Spring Test, HSQLDB in tests

## Repository structure

```text
webapp/                presentation layer
services/              service implementations
service-contracts/     service interfaces
persistence/           JDBC implementations, migrations, persistence tests
persistence-contracts/ DAO interfaces
models/                domain model
```

There is no generic `interfaces/` module. Contracts live in `*-contracts`
modules.

## Layering rules

- `webapp` depends on service contracts and service implementations.
- `services` depends on service contracts and persistence contracts.
- `persistence` depends on persistence contracts.
- `service-contracts` and `persistence-contracts` depend on `models`.
- Do not import concrete implementations across layers when a contract exists.

## Web layer

- Controllers live in `webapp`.
- Use `ModelAndView`.
- Prefer constructor injection.
- Inject service interfaces from `service-contracts`.
- Do not inject DAOs into controllers.
- Do not place SQL or business logic in controllers.

## Routing and views

- `WebConfig` owns MVC configuration.
- `ViewResolver` uses `prefix="/WEB-INF/views/"` and `suffix=".jsp"`.
- Controllers should return logical view names such as `"index"` or
  `"helloworld/index"`.
- JSP views live under `webapp/src/main/webapp/WEB-INF/views/`.
- Reusable UI lives under `webapp/src/main/webapp/WEB-INF/tags/`.

## Forms, validation, and i18n

- The web layer uses `*Form` classes for form binding and validation.
- Put form classes in `webapp`, for example under
  `ar.edu.itba.paw.webapp.form`.
- Use `@ModelAttribute` for binding and `@Valid` plus `BindingResult` for
  validation.
- Keep validation annotations on form objects, not on persistence classes.
- Map form objects to service/domain inputs inside controllers.
- Use Spring form tags in JSPs when working with forms.
- Use `MessageSource` plus `messages.properties` for validation and UI messages.
- Treat i18n as part of the standard web architecture.

## Frontend conventions

- The frontend is server-rendered, not a SPA.
- Tailwind is compiled through Maven.
- `input.css` loads Tailwind and source scanning.
- `main.css` is the design-system stylesheet.
- `main.css` defines semantic tokens such as:
  - `--color-primary`
  - `--color-secondary`
  - `--color-background`
  - `--color-foreground`
  - `--color-accent`
  - `--color-muted`
  - `--color-border`
  - `--color-danger`
  - `--color-success`
- Prefer semantic tokens and shared component classes over scattered hardcoded
  colors.
- Keep repeated styling in reusable classes when that is clearer than repeating
  long utility lists.

## Reusable frontend components

- Extract repeated UI into reusable tag files.
- Minimize duplication across JSPs, CSS, and JavaScript.
- If a piece of UI appears more than once, strongly consider abstracting it.
- Repeated interactions should use shared JavaScript instead of inline duplicated
  scripts.
- Avoid scriptlets in JSP and tag files. Use JSTL and EL.

## JavaScript

- JavaScript is minimal and enhancement-oriented.
- Use it for component interactivity, accessibility improvements, and
  `localStorage`-backed UI preferences.
- Good examples: dropdowns, modals, tabs, dismissible alerts, theme persistence,
  filter persistence, layout preference persistence.
- Do not turn the app into a client-side SPA.

## Service layer

- Service interfaces live in `service-contracts`.
- Service implementations live in `services`.
- Inject DAO interfaces from `persistence-contracts`.
- Do not use `JdbcTemplate` or SQL in services.
- Do not return web-layer objects from services.
- Do not access request or session state from services.

## Persistence layer

- DAO interfaces live in `persistence-contracts`.
- JDBC implementations live in `persistence`.
- Interface naming uses `*Dao`.
- JDBC implementation naming uses `*JdbcDao`.
- Use `JdbcTemplate` and `SimpleJdbcInsert`.
- Keep `RowMapper` definitions as `static final` fields.
- Return `Optional<T>` when a row may not exist.
- Do not place business logic in DAOs.
- Do not access web-layer classes from persistence code.

## Database and migrations

- Runtime database: PostgreSQL.
- Flyway migrations live in `persistence/src/main/resources/db/migration/`.
- Do not treat HSQLDB as the main database.

## Testing

- Service tests use JUnit 5 + Mockito.
- Persistence tests use Spring Test + in-memory HSQLDB.
- Keep test style aligned with the layer under test.

## General guidance

- Follow existing module boundaries.
- Reuse abstractions before duplicating code.
- Do not invent repository structure that does not exist.
- Prefer small, consistent, maintainable additions over one-off patterns.
