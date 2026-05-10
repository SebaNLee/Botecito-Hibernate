# Auth/User Migration Notes

## Assigned scope

Migrate the auth and user area to the staged layered architecture while keeping
old code preserved until the `nuevo` implementation is accepted.

Scope:

- `AuthController`
- login
- registration
- `/profile`
- user-related models, services, and DAOs
- email-related logic used by auth/user flows
- auth-related tokens, such as password recovery tokens

Do not migrate deprecated owner/delete/token behavior unless someone confirms it
is still needed.

## Target data flow

```text
Controller
 -> receives HTTP requests
 -> receives/binds Forms
 -> checks BindingResult
 -> delegates valid Forms to Presentation

Presentation
 -> maps Forms to domain Models
 -> calls Services with Models
 -> maps service results to JSP-ready ModelAndView

Services
 -> receive Models as input
 -> contain business rules
 -> call DAO interfaces

DAO
 -> receives Models or identifiers
 -> uses Hibernate
 -> contacts the database

JSP
 -> renders ModelAndView data
 -> submits Forms back to Controllers
```

Rules:

- Controllers stay skinny: request parsing, form binding, validation result
  checks, presentation delegation, and `ModelAndView` returns only.
- Presentation owns form-to-model mapping and JSP-ready model preparation.
- Services receive domain models, not web forms.
- New staged files go under `nuevo` packages/folders first.
- Old files stay in place until the staged implementation is accepted.

Current staged auth/user model inputs:

```text
RegisterForm -> UserModel + rawPassword
ProfileForm + currentUserId -> UserModel
PasswordRecoveryRequestForm -> UserModel(email)
PasswordResetForm -> UserModel(passwordRecoveryToken) + rawPassword
```

Current staged auth/profile/user flow:

```text
webapp/controller/nuevo
 -> webapp/presentation/nuevo
 -> service-contracts/services/nuevo.UserService
 -> services/nuevo.UserServiceImpl
 -> persistence-contracts/persistence/nuevo.UserDao
 -> persistence/orm/daos.UserHibernateDao
 -> UsersOrm/database
```

## Work done

- Added staged auth controller/presentation/mapper/forms under `nuevo`.
- Added staged profile controller/presentation/mapper/form under `nuevo`.
- Disabled old active auth/profile controller annotations where needed so staged
  routes do not conflict with preserved old controllers.
- Added staged `services.nuevo.UserService` and `services.nuevo.UserServiceImpl`.
- Added staged `persistence.nuevo.UserDao`.
- Added `persistence.orm.daos.UserHibernateDao`.
- Switched staged `UserServiceImpl` to use `persistence.nuevo.UserDao`.
- Added tests for staged auth/profile presentation, staged profile controller,
  staged user service, and staged user Hibernate DAO.

Preserved old files include:

- `webapp/controller/AuthController.java`
- `webapp/controller/ProfileController.java`
- `webapp/controller/support/ProfileMvcSupport.java`
- `service-contracts/services/UserService.java`
- `services/UserServiceImpl.java`
- `persistence-contracts/persistence/UserDao.java`
- `persistence/UserJdbcDao.java`
- `service-contracts/services/MailService.java`
- `services/MailServiceImpl.java`

## Work to do

Next: migrate auth/user mail in the same staged style.

Implementation details:

1. Add `service-contracts/src/main/java/ar/edu/itba/paw/services/nuevo/MailService.java`.
2. Add `services/src/main/java/ar/edu/itba/paw/services/nuevo/MailServiceImpl.java`.
3. Keep old `MailService` and `MailServiceImpl` untouched.
4. For this pass, migrate only auth/user mail:
   - password recovery email
   - locale resolution needed by auth/user mail
5. Do not migrate booking/payment/publication mail yet; those still belong to
   old service flows until those areas are staged.
6. Make staged `services.nuevo.UserServiceImpl` depend on
   `services.nuevo.MailService` for password recovery mail.
7. Prefer model-based mail inputs:

```text
void sendPasswordRecoveryEmail(UserModel user)
Locale resolveLocale(UserModel user)
Locale resolveLocale(String recipientIdentifier) // temporary bridge only
```

8. `services.nuevo.MailServiceImpl` should use the `UserModel` it receives for
   email, display name, preferred language, and recovery token. It should not
   re-query the user for password recovery mail.
9. Keep the staged mail implementation dependencies limited to:
   - `JavaMailSender`
   - `TemplateEngine`
   - `MessageSource`
   - `credentialsProperties`
   - a temporary user lookup dependency only if needed for
     `resolveLocale(String recipientIdentifier)`
10. Add staged mail tests under
    `services/src/test/java/ar/edu/itba/paw/services/nuevo/`.
11. Run:

```text
mvn -pl service-contracts,services,webapp,persistence -am test
```

Later:

- Migrate booking/payment/publication mail only when those service flows are
  moved to staged models/contracts.
- After staged auth/user/mail is accepted, remove old files and unpack `nuevo`
  classes into final packages.
