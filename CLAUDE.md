# CLAUDE.md

When working in this repository, follow these conventions as the default project
architecture.

## Architecture

- This is a multi-module Spring MVC application.
- Modules:
  - `webapp`
  - `services`
  - `service-contracts`
  - `persistence`
  - `persistence-contracts`
  - `models`
- There is no generic `interfaces/` module. Contracts live in `*-contracts`
  modules.

## Layer boundaries

- Controllers belong in `webapp`.
- Services belong in `services`.
- JDBC DAOs belong in `persistence`.
- Domain entities belong in `models`.
- Inject contracts, not concrete implementations.
- Do not skip layers.

## Web conventions

- Use `ModelAndView` in controllers.
- Prefer constructor injection.
- Inject service interfaces from `service-contracts`.
- Do not inject DAOs into controllers.
- Do not place business logic or SQL in controllers.

## Views

- `WebConfig` defines the MVC setup.
- The view resolver uses `"/WEB-INF/views/"` and `".jsp"`.
- Return logical view names, not full JSP paths, unless there is a deliberate
  exception.
- JSP views live in `WEB-INF/views`.
- Reusable UI components live in `WEB-INF/tags`.

## Forms and validation

- Use `*Form` classes in the web layer for form binding.
- Place form classes in `webapp`, for example under
  `ar.edu.itba.paw.webapp.form`.
- Use `@ModelAttribute` for form binding.
- Use `@Valid` and `BindingResult` for validation.
- Keep validation annotations on form objects.
- Map forms to service or domain inputs in controllers.

## i18n

- Use `MessageSource` in `WebConfig`.
- Use `messages.properties` and locale-specific variants for validation and UI
  strings.
- Treat i18n as a standard part of the web layer.

## Frontend

- The frontend is server-rendered.
- Use JSP, JSTL, Spring form tags, Tailwind, and minimal JavaScript.
- Tailwind is compiled through Maven.
- `input.css` handles Tailwind loading and scanning.
- `main.css` is the design-system stylesheet.
- Define semantic tokens in `main.css`, including:
  - primary
  - secondary
  - background
  - foreground
  - accent
  - muted
  - border
  - success
  - danger
- Prefer semantic design tokens over hardcoded color choices.

## Reuse and abstraction

- Extract repeated markup into reusable tag files.
- Minimize duplication across JSPs, CSS, and JavaScript.
- If UI repeats, abstract it.
- If interaction logic repeats, move it to shared JavaScript.
- Use JSTL and EL, not JSP scriptlets.

## JavaScript

- Keep JavaScript minimal and enhancement-oriented.
- Use it for interactive components and `localStorage` UI persistence.
- Acceptable uses include dropdowns, modals, tabs, dismissible alerts, theme
  persistence, and filter persistence.
- Do not turn the application into a SPA.

## Service layer

- Service interfaces live in `service-contracts`.
- Service implementations live in `services`.
- Inject DAO interfaces from `persistence-contracts`.
- Do not use `JdbcTemplate` or SQL in services.
- Do not access request or session state in services.
- Do not return web-layer objects from services.

## Persistence layer

- DAO interfaces live in `persistence-contracts`.
- JDBC implementations live in `persistence`.
- Use `*Dao` for interfaces and `*JdbcDao` for implementations.
- Use `JdbcTemplate` and `SimpleJdbcInsert`.
- Keep `RowMapper` as `static final` fields.
- Return `Optional<T>` when data may not exist.
- Keep business logic out of DAOs.

## Database and tests

- Runtime uses PostgreSQL.
- Flyway migrations live under `persistence/.../db/migration/`.
- Service tests use Mockito.
- Persistence tests use Spring Test with in-memory HSQLDB.
- HSQLDB is for tests only.

## Working style

- Respect existing module boundaries.
- Prefer reuse over duplication.
- Do not invent structure or conventions that are outside this architecture.
