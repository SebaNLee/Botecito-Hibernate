# AGENTS.md

This repository is a multi-module Spring MVC application. Treat the conventions
below as the project baseline when reading, generating, or editing code. All new
code should follow these rules directly.

## Stack

- Java 21
- Spring MVC
- JSP + JSTL + Spring form tags
- Tailwind CSS
- Minimal JavaScript
- JPA (Hibernate)
- PostgreSQL in runtime
- Flyway migrations
- JUnit 5, Mockito, Spring Test, HSQLDB in tests

## Repository structure

```text
webapp/                presentation layer
services/              service implementations
service-contracts/     service interfaces
persistence/           JPA implementations, migrations, persistence tests
persistence-contracts/ DAO interfaces
models/                domain model
```

There is no generic `interfaces/` module. Contracts live in `*-contracts`
modules.

## Layering rules

- `webapp` depends on `service-contracts` and `services`.
- `services` depends on `service-contracts` and `persistence-contracts`.
- `persistence` depends on `persistence-contracts`.
- `service-contracts` and `persistence-contracts` depend on `models`.
- Do not import concrete implementations across layers when a contract exists.
- Do not skip layers.

## Web layer

- Controllers live in `webapp`.
- Use `ModelAndView`.
- Prefer constructor injection.
- Inject service interfaces from `service-contracts`.
- Do not inject DAOs into controllers.
- Do not place SQL or business logic in controllers.
- Controllers handle request parsing, form binding, validation, and model
  preparation.

## Routing and views

- `WebConfig` owns MVC configuration.
- `ViewResolver` uses `prefix="/WEB-INF/views/"` and `suffix=".jsp"`.
- Controllers should return logical view names such as `"index"`,
  `"marketplace"`, or `"helloworld/index"`.
- Do not return full JSP paths unless there is a deliberate exception already
  present in the code.
- JSP views live under `webapp/src/main/webapp/WEB-INF/views/`.
- Reusable UI lives under `webapp/src/main/webapp/WEB-INF/tags/`.

## JSP, taglibs, and component rules

- In views, use:
  - JSTL core: `http://java.sun.com/jsp/jstl/core`
  - custom tags from `tagdir="/WEB-INF/tags"`
- In form and i18n views, also use Spring taglibs:
  - `http://www.springframework.org/tags/form`
  - `http://www.springframework.org/tags`
- Do not use `core_rt`.
- Do not use JSP scriptlets.
- Use JSTL and EL expressions.
- Use `<c:url>` for links and resources.

## Forms, validation, and i18n

- The web layer uses `*Form` classes for form binding and validation.
- Put form classes in `webapp`, for example under
  `ar.edu.itba.paw.webapp.form`.
- Use `@ModelAttribute` for binding.
- Use `@Valid` plus `BindingResult` for validation.
- Helper `@ModelAttribute` methods are valid for shared model state.
- Keep validation annotations on form objects, not on persistence classes.
- Map form objects to service or domain inputs inside controllers.
- Use Spring form tags in JSPs when working with forms.
- Use `MessageSource` plus `messages.properties` for validation and UI messages.
- Use locale-specific message variants when needed.
- Treat i18n as part of the standard web architecture.
- Use PRG where it makes sense after successful POST requests.

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
  - `--radius-*`
  - `--shadow-*`
  - spacing and surface tokens when needed
- Prefer semantic tokens and shared component classes over scattered hardcoded
  colors.
- Theme overrides such as dark mode should be implemented through token
  overrides, not one-off ad hoc styles.
- Keep repeated styling in reusable classes when that is clearer than repeating
  long utility lists.

## Reusable frontend components

- Extract repeated UI into reusable tag files.
- Minimize duplication across JSPs, CSS, and JavaScript.
- If a piece of UI appears more than once, strongly consider abstracting it.
- Use reusable primitives for recurring UI patterns such as form fields, inputs,
  selects, textareas, checkboxes, alerts, badges, dropdowns, modals, drawers,
  empty states, page headers, sections, stacks, and grids.
- Repeated interactions should use shared JavaScript instead of inline duplicated
  scripts.

## JavaScript

- JavaScript is minimal and enhancement-oriented.
- Use it for component interactivity, accessibility improvements, and
  `localStorage`-backed UI preferences.
- Good examples: dropdowns, modals, tabs, dismissible alerts, copy buttons,
  keyboard behavior, theme persistence, filter persistence, and layout
  preference persistence.
- Keep server-rendered navigation and data flow as the default.
- Do not turn the app into a client-side SPA.

## Service layer

- Service interfaces live in `service-contracts`.
- Service implementations live in `services`.
- Inject DAO interfaces from `persistence-contracts`.
- Do not use `JdbcTemplate` or SQL in services.
- Do not return web-layer objects from services.
- Do not access request or session state from services.
- Keep business logic in services, not in controllers or DAOs.

## Persistence layer

- DAO interfaces live in `persistence-contracts`.
- JPA implementations live in `persistence`.
- Interface naming uses `*Dao`.
- JPA implementation naming uses `*JpaDao`.
- Use `EntityManager`, JPQL, and typed queries.
- Return `Optional<T>` when a row may not exist.
- Do not place business logic in DAOs.
- Do not access web-layer classes from persistence code.
- Persistence tests should seed data via `EntityManager` and assert final table
  state independently of the DAO read path under test.

## Database and migrations

- Runtime database: PostgreSQL.
- Flyway migrations live in `persistence/src/main/resources/db/migration/`.
- Do not treat HSQLDB as the main database.
- Do not describe `schema.sql` as the primary runtime mechanism.

## Testing

- Service tests use JUnit 5 + Mockito.
- Persistence tests use Spring Test + in-memory HSQLDB.
- Persistence tests should follow the Spring Test style used in the repository,
  including test configuration, transactional test behavior, and database
  initialization for the test datasource.
- Keep test style aligned with the layer under test.

## General guidance

- Follow existing module boundaries.
- Reuse abstractions before duplicating code.
- Do not invent repository structure that does not exist.
- Prefer small, consistent, maintainable additions over one-off patterns.
