# Auth/User Migration Notes

## Assigned scope

I am assigned to migrate the auth and user area of the app to the new layered
architecture. This includes:

- `AuthController`
- login
- registration, if currently handled there
- `/profile`
- user-related models, services, and DAOs
- email-related logic
- auth-related tokens, such as verification or reset tokens if they exist

## Target data flow

```text
Controller
 -> receives HTTP requests
 -> receives/binds Forms
 -> checks form validation results
 -> sends valid Forms to the Presentation layer

Presentation layer
 -> receives Forms from Controllers
 -> turns Forms into domain Models
 -> calls Services with Models

Services
 -> receive Models as input
 -> contain business logic
 -> call DAO interfaces
 -> returns models to presentation layer

DAO
 -> receives Models or model identifiers as needed
 -> uses Hibernate
 -> contacts the database

Presentation layer
 -> receives Models back from Services
 -> turns returned Models into JSP-ready data
 -> gives the JSP response/model back to the Controller

Controller
 -> returns the JSP response prepared by the Presentation layer

JSP
 -> renders the data
 -> submits Forms back to the Controller
```

Important rule: controllers must be skinny. They should not create domain
models, call services directly, or contain business logic. Controllers receive
forms, check `BindingResult`, pass valid forms to the presentation layer, and
return the JSP response prepared by presentation. Form validation should live in
the `*Form` classes through Bean Validation annotations or form-local methods,
as in `MarketplaceSearchForm.isSearchTimeRangeValid()`.

Services receive domain models as input, not web forms. The presentation layer
is responsible for turning forms into models before calling services, and for
turning returned models into JSP-ready data.

Current staged auth domain inputs use `*Model` names under
`models/src/main/java/ar/edu/itba/paw/models/nuevo/`. This keeps them visually
separate from the old `models` classes while the migration is staged.

Current staged auth mapping:

```text
RegisterForm -> UserModel + rawPassword
PasswordRecoveryRequestForm -> UserModel(email)
PasswordResetForm -> UserModel(passwordRecoveryToken) + rawPassword
```

`UserModel` contains domain/user state such as names, email, phone, payment
alias, preferred language, password hash, and recovery token state. It does not
contain raw password, confirm password, request objects, or query/form flags.
`PreferredLanguageModel` owns the typed `"es"`/`"en"` language value.

## Work to do

1. Use `/marketplace GET` as the reference example for the new structure.
2. Review all auth, login, logout, profile, user, email, and token routes.
3. Update `AuthController` so it handles request parsing, form binding,
   `BindingResult` checks, presentation delegation, and `ModelAndView` responses
   only.
4. Move business rules out of controllers and into services.
5. Create or update form classes for login, registration, profile update,
   password reset, and email verification where applicable.
6. Move form-to-model mapping into the presentation layer.
7. Make the presentation layer call services with domain models.
8. Make service methods receive domain models and use DAO interfaces.
9. Migrate user/auth-related DAOs to Hibernate.
10. Update JSPs to use the expected model attributes and Spring form tags.
11. Add or update validation and UI messages in `messages.properties`.
12. Add or update tests for auth, user, profile, email, and token behavior.

## Deprecated area

Old owner/delete/token actions in model-related code appear to be deprecated.
Do not migrate them unless someone confirms they are still needed.

## Current code reference

The closest existing example is `/marketplace GET`:

```text
webapp/src/main/java/ar/edu/itba/paw/webapp/controller/MarketplaceController.java
 -> receives request and binds `MarketplaceSearchForm`
 -> checks `BindingResult`
 -> delegates to `MarketplaceMvcSupport`

webapp/src/main/java/ar/edu/itba/paw/webapp/controller/support/MarketplaceMvcSupport.java
 -> maps `MarketplaceSearchForm` to `ItemSearchCriteria`
 -> calls `ItemService.searchMarketplace(...)`
 -> receives `Page<Item>`
 -> creates `ModelAndView("marketplace")`
 -> adds JSP-ready model attributes
```

This is the pattern to copy for auth/profile, except the new target name should
be presentation layer instead of `controller/support`.

## Current auth/profile files

### Controllers

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/AuthController.java`
  - Handles `/login`, `/register`, `/password-recovery`, `/password-recovery/{token}`, and `/403`.
  - Currently too fat: it calls `UserService` directly, does password-confirmation checks, prepares views, and triggers post-registration authentication.
  - Needs to become a skinny delegating controller.

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/ProfileController.java`
  - Handles `/profile` and `/profile/password-recovery`.
  - Already delegates to `ProfileMvcSupport`.
  - Needs to delegate to the new presentation layer instead.

### Current presentation-like support

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/support/ProfileMvcSupport.java`
  - Already acts like presentation for profile.
  - Finds the current user, populates `ProfileForm`, calls `UserService`, refreshes the Spring Security principal, and builds `ModelAndView("profile")`.
  - Should be migrated/renamed into the real presentation layer.

- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/support/MarketplaceMvcSupport.java`
  - Keep as the example for how presentation should work.

- `webapp/src/main/java/ar/edu/itba/paw/webapp/presentation/TODO-FACADE`
  - Placeholder for the new presentation package.
  - This is probably where the real facade/support classes should eventually live.

### Forms

- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/RegisterForm.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/ProfileForm.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/PasswordRecoveryRequestForm.java`
- `webapp/src/main/java/ar/edu/itba/paw/webapp/form/PasswordResetForm.java`

These already contain Bean Validation annotations. Password-confirmation
matching is currently manual inside `AuthController`; it should move into form
validation, for example an `@AssertTrue` method like
`MarketplaceSearchForm.isSearchTimeRangeValid()`.

### Validation

- Existing generic validation examples:
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/form/validation/FileSize.java`
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/form/validation/FileSizeValidator.java`
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/form/validation/ImageGalleryUpload.java`
  - `webapp/src/main/java/ar/edu/itba/paw/webapp/form/validation/ImageGalleryUploadValidator.java`

New auth/profile validation should stay in the relevant `*Form` classes when it
only depends on submitted fields.

### Auth/security helpers

- `webapp/src/main/java/ar/edu/itba/paw/webapp/auth/PostRegistrationAuthenticator.java`
  - Used after successful registration.
  - Current controller calls it directly.
  - In the new flow, presentation should own this after the registration service call succeeds, or it should be wrapped by a small auth presentation/helper component.

- `webapp/src/main/java/ar/edu/itba/paw/webapp/auth/UserAccountDetailsService.java`
  - Spring Security user lookup.
  - Depends on `UserService`.
  - Probably keep as-is unless the service contract changes.

- `webapp/src/main/java/ar/edu/itba/paw/webapp/config/WebAuthConfig.java`
  - Owns login/logout/security route rules.
  - Needs review only if routes or login parameter names change.

### Services

- `service-contracts/src/main/java/ar/edu/itba/paw/services/UserService.java`
  - Currently receives primitive strings for register/update/reset flows.
  - Needs new methods that receive domain models as input.

- `services/src/main/java/ar/edu/itba/paw/services/UserServiceImpl.java`
  - Contains user business rules, password encoding, duplicate email logic, password recovery token generation, and mail triggering.
  - Should remain the business layer.
  - Needs to accept model-based inputs instead of form/primitive-style inputs.

- `service-contracts/src/main/java/ar/edu/itba/paw/services/MailService.java`
- `services/src/main/java/ar/edu/itba/paw/services/MailServiceImpl.java`
  - Password recovery email is called from `UserServiceImpl`.
  - Mail service currently also queries DAOs to resolve locale/recipients.
  - Needs review because services should receive models where possible and avoid hidden persistence lookups unless kept as deliberate service logic.

### Persistence

- `persistence-contracts/src/main/java/ar/edu/itba/paw/persistence/UserDao.java`
  - Current contract is primitive-heavy.
  - Needs model-based methods where possible, especially create/update/claim.

- `persistence/src/main/java/ar/edu/itba/paw/persistence/UserJdbcDao.java`
  - Current JDBC implementation.
  - Needs a new Hibernate implementation.

- `models/src/main/java/ar/edu/itba/paw/models/User.java`
  - Main domain model for this work.
  - Needs Hibernate mapping review if the migration requires annotations/entities.

- Relevant migrations:
  - `persistence/src/main/resources/db/migration/V6__users_language_and_item_owner_delete_token.sql`
  - `persistence/src/main/resources/db/migration/V7__split_user_name.sql`
  - `persistence/src/main/resources/db/migration/V12__users_password_hash.sql`
  - `persistence/src/main/resources/db/migration/V15__users_payment_alias.sql`
  - `persistence/src/main/resources/db/migration/V17__users_password_recovery.sql`

## Staging rule for new files

Anything added for this migration should be staged under a `nuevo/` folder first,
then later moved/unpacked after the old implementation is removed.

Suggested staging folders:

```text
webapp/src/main/java/ar/edu/itba/paw/webapp/<area>/nuevo/
service-contracts/src/main/java/ar/edu/itba/paw/services/nuevo/
services/src/main/java/ar/edu/itba/paw/services/nuevo/
persistence-contracts/src/main/java/ar/edu/itba/paw/persistence/nuevo/
persistence/src/main/java/ar/edu/itba/paw/persistence/nuevo/
models/src/main/java/ar/edu/itba/paw/models/nuevo/
```

## Proposed new files and plug-in points

### Webapp presentation layer

Add staged files under:

```text
webapp/src/main/java/ar/edu/itba/paw/webapp/presentation/nuevo/
```

Planned files:

- `AuthPresentation`
  - Replaces most of the current body of `AuthController`.
  - Receives valid `RegisterForm`, `PasswordRecoveryRequestForm`, and `PasswordResetForm`.
  - Maps forms to `User` or auth-specific domain inputs.
  - Calls `UserService`.
  - Builds `ModelAndView` for:
    - `login`
    - `register`
    - `password-recovery-request`
    - `password-recovery-reset`
    - redirects after successful register/reset.
  - Initial staged version exists, with active controller/form/view files under
    `controller/nuevo`, `form/nuevo`, and `views/nuevo`.

- `ProfilePresentation`
  - Replacement for `ProfileMvcSupport`.
  - Receives `ProfileForm`.
  - Loads current authenticated user.
  - Maps `ProfileForm` to `User`.
  - Calls `UserService.updateProfile(...)`.
  - Builds `ModelAndView("profile")`.
  - Refreshes authenticated principal after email changes, unless this becomes an auth helper.

- `AuthModelMapper`
  - Converts:
    - `RegisterForm -> UserModel`
    - `PasswordRecoveryRequestForm -> UserModel(email)`
    - `PasswordResetForm + token -> UserModel(passwordRecoveryToken)`
  - Next auth step before service migration: add this mapping so
    `AuthPresentation` no longer passes primitive user fields to `UserService`.

- `ProfileModelMapper`
  - Converts:
    - `User -> ProfileForm`
    - `ProfileForm + currentUserId -> User`

Controller plug-in points:

- `AuthController` should inject `AuthPresentation`, not `UserService`.
- `ProfileController` should inject `ProfilePresentation`, not `ProfileMvcSupport`.
- While staged, active controller files can live under `controller/nuevo` so the
  existing `ar.edu.itba.paw.webapp.controller` scan picks them up without
  changing `WebConfig`.

### Service contracts

Add staged files or staged changes under:

```text
service-contracts/src/main/java/ar/edu/itba/paw/services/nuevo/
```

Needed contract changes:

- Replace primitive-heavy user methods with model-based inputs where possible.
- Do this as staged `services/nuevo` contracts first; keep old `UserService`
  untouched until callers are ready to switch.
- Possible target shape:

```text
RegistrationResult register(UserModel user, String rawPassword)
Optional<UserModel> updateProfile(UserModel user)
Optional<UserModel> requestPasswordRecovery(UserModel user)
Optional<UserModel> findByPasswordRecoveryToken(String token)
PasswordRecoveryResult resetPassword(UserModel user, String rawPassword)
```

Keep `findByEmail(String)` and `findById(int)` because lookup by identifier is normal and useful.

Plug-in point:

- `AuthPresentation` and `ProfilePresentation` call the updated `UserService`.
- `UserAccountDetailsService`, `UserLocaleResolver`, and other existing code may still need `findByEmail`/`findById`.
- Current staged auth now calls `services.nuevo.UserService`, while old callers
  keep using the old `UserService`.

### Services implementation

Add staged files or staged changes under:

```text
services/src/main/java/ar/edu/itba/paw/services/nuevo/
```

Needed implementation changes:

- `UserServiceImpl`
  - Receive `UserModel` models for registration/profile update.
  - Keep password encoding here.
  - Keep duplicate email checks here.
  - Keep password recovery token generation here.
  - Current staged bridge still calls the old primitive `UserDao` internally
    until the staged DAO contract exists.

- `MailServiceImpl`
  - Review whether methods should accept richer models instead of loose strings.
  - Password recovery should ideally receive enough user data from `UserServiceImpl` so mail does not need extra user lookup for this flow.

Plug-in point:

- Existing Spring `@Service` scanning already picks up `ar.edu.itba.paw.services`.

### Persistence contracts

Add staged files or staged changes under:

```text
persistence-contracts/src/main/java/ar/edu/itba/paw/persistence/nuevo/
```

Needed contract changes:

- Add model-based user methods:

```text
User createUser(User user)
Optional<User> claimUser(User user)
Optional<User> updateProfile(User user)
Optional<User> updatePasswordRecoveryToken(User user)
```

- Keep lookup methods:

```text
Optional<User> findById(int id)
Optional<User> findByEmail(String email)
Optional<User> findByPasswordRecoveryToken(String token)
List<User> findUsersByIds(Collection<Integer> userIds)
```

Plug-in point:

- `UserServiceImpl` should depend only on `UserDao`, not a concrete Hibernate DAO.

### Hibernate persistence implementation

Add staged files under:

```text
persistence/src/main/java/ar/edu/itba/paw/persistence/nuevo/
```

Planned files:

- `UserHibernateDao`
  - Replacement for `UserJdbcDao`.
  - Implements `UserDao`.
  - Uses Hibernate/JPA session/entity manager according to the team standard.

- `HibernateConfig` or persistence config additions, if no shared Hibernate config exists yet.
  - Current `WebConfig` only defines `DataSource`, `JdbcTemplate` users, and Flyway.
  - Hibernate needs session/entity manager configuration and transaction management.

Needed Maven/config work:

- `persistence/pom.xml`
  - Add Hibernate dependencies once the team confirms the exact Hibernate version/API.

- root `pom.xml`
  - Add dependency management for Hibernate if needed.

- `WebConfig`
  - Import/scan Hibernate persistence config if it is not picked up automatically.

### Models

Add staged files or staged changes under:

```text
models/src/main/java/ar/edu/itba/paw/models/nuevo/
```

Current staged auth model files:

- `UserModel`
  - Standalone staged user/auth domain model.
  - Includes `id`, `createdAt`, `givenName`, `lastName`, `email`, `phone`,
    `paymentAlias`, `preferredLanguage`, `passwordHash`,
    `passwordRecoveryToken`, `passwordRecoveryUsedAt`, and `getName()`.
  - Does not include form-only values like raw password, confirm password,
    `HttpServletRequest`, `sent`, or `invalid`.
- `PreferredLanguageModel`
  - Staged enum for `ES("es")` and `EN("en")`.
  - Provides `fromInput`, `fromPersistence`, `toLocale`, and
    `getPersistenceCode`.

Likely later Hibernate changes:

- `UserModel`
  - Add Hibernate/JPA mapping annotations if the project chooses annotated entities.
  - Map table `users`.
  - Map columns:
    - `id`
    - `created_at`
    - `first_name`
    - `last_name`
    - `email`
    - `phone`
    - `alias`
    - `language`
    - `password_hash`
    - `mail_token`
    - `mail_token_emitted_at`

Watch out:

- `passwordRecoveryUsedAt` currently maps from `mail_token_emitted_at`, which looks semantically odd. Check the migration/table meaning before renaming anything.
- Deprecated owner/delete token fields from old migrations should not be brought into the new user model unless confirmed.

## Tests to update

- `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/AuthControllerTest.java`
  - Update to assert skinny controller delegation.
  - Move service-result tests to presentation tests.

- `webapp/src/test/java/ar/edu/itba/paw/webapp/controller/ProfileControllerTest.java`
  - Update to use `ProfilePresentation`.
  - Keep/relocate tests for profile form population and principal refresh.

- New tests to add later:
  - `webapp/src/test/java/ar/edu/itba/paw/webapp/presentation/AuthPresentationTest.java`
  - `webapp/src/test/java/ar/edu/itba/paw/webapp/presentation/ProfilePresentationTest.java`

- `services/src/test/java/ar/edu/itba/paw/services/UserServiceImplTest.java`
  - Update for model-based service inputs.

- `services/src/test/java/ar/edu/itba/paw/services/MailServiceImplTest.java`
  - Update if mail contracts become model-based.

- `persistence/src/test/java/ar/edu/itba/paw/persistence/UserJdbcDaoTest.java`
  - Replace or complement with Hibernate DAO tests.

## Concrete migration order

1. Finish staged auth presentation and form-to-model mapping.
2. Create staged service contracts with `UserModel`-based user inputs.
3. Adapt staged `UserServiceImpl` to `UserModel`-based inputs, bridging to old
   `UserDao` internally for now.
4. Migrate profile to staged presentation using the same pattern.
5. Add staged `UserHibernateDao`.
6. Add Hibernate configuration/dependencies.
7. Update tests around the new structure.
8. Once the staged version is accepted, remove old files/classes and unpack from `nuevo/` into the final packages.
