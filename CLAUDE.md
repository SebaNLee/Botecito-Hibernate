# CLAUDE.md

When working in this repository, follow these conventions as the default project
architecture. Treat them as established project rules.

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
- Controllers are responsible for request parsing, form binding, validation, and
  preparing the model for the view.

## Views

- `WebConfig` defines the MVC setup.
- The view resolver uses `"/WEB-INF/views/"` and `".jsp"`.
- Return logical view names, not full JSP paths, unless there is a deliberate
  exception already present.
- JSP views live in `WEB-INF/views`.
- Reusable UI components live in `WEB-INF/tags`.
- Use `<c:url>` for links and static resources.

## JSP and taglib conventions

- Use JSTL core from `http://java.sun.com/jsp/jstl/core`.
- Use custom tags from `tagdir="/WEB-INF/tags"`.
- Use Spring form tags and Spring message tags for forms and i18n.
- Do not use `core_rt`.
- Do not use JSP scriptlets.
- Use JSTL and EL expressions instead.

## Forms and validation

- Use `*Form` classes in the web layer for form binding.
- Place form classes in `webapp`, for example under
  `ar.edu.itba.paw.webapp.form`.
- Use `@ModelAttribute` for form binding.
- Use `@Valid` and `BindingResult` for validation.
- Helper `@ModelAttribute` methods are valid for shared model state.
- Keep validation annotations on form objects.
- Map forms to service or domain inputs in controllers.
- Use PRG where appropriate after successful POST requests.

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
  - radius
  - shadow
  - spacing and surface tokens as needed
- Prefer semantic design tokens over hardcoded color choices.
- Implement theme variants through token overrides instead of one-off styles.

## Reuse and abstraction

- Extract repeated markup into reusable tag files.
- Minimize duplication across JSPs, CSS, and JavaScript.
- If UI repeats, abstract it.
- If interaction logic repeats, move it to shared JavaScript.
- Use reusable primitives for recurring UI patterns such as form fields, inputs,
  selects, textareas, alerts, badges, dropdowns, modals, drawers, and layout
  sections.

## JavaScript

- Keep JavaScript minimal and enhancement-oriented.
- Use it for interactive components and `localStorage` UI persistence.
- Acceptable uses include dropdowns, modals, tabs, dismissible alerts, copy
  buttons, theme persistence, filter persistence, and layout preferences.
- Keep server-rendered navigation and data flow as the default.
- Do not turn the application into a SPA.

## Service layer

- Service interfaces live in `service-contracts`.
- Service implementations live in `services`.
- Inject DAO interfaces from `persistence-contracts`.
- Do not use `JdbcTemplate` or SQL in services.
- Do not access request or session state in services.
- Do not return web-layer objects from services.
- Keep business logic in services.

## Persistence layer

- DAO interfaces live in `persistence-contracts`.
- JDBC implementations live in `persistence`.
- Use `*Dao` for interfaces and `*JdbcDao` for implementations.
- Use `JdbcTemplate` and `SimpleJdbcInsert`.
- Keep `RowMapper` as `static final` fields.
- Return `Optional<T>` when data may not exist.
- Keep business logic out of DAOs.
- Do not access web-layer classes from persistence code.

## Database and tests

- Runtime uses PostgreSQL.
- Flyway migrations live under `persistence/.../db/migration/`.
- Do not treat HSQLDB as the runtime database.
- Service tests use Mockito.
- Persistence tests use Spring Test with in-memory HSQLDB.
- Persistence tests should follow the existing repository style for test
  configuration, transactional behavior, and datasource initialization.
- HSQLDB is for tests only.

## Working style

- Respect existing module boundaries.
- Prefer reuse over duplication.
- Do not invent structure or conventions that are outside this architecture.
