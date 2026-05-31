
todo:

## Considerations:
# Target data flow

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


Rules:

- Controllers stay skinny: request parsing, form binding, validation result
  checks, presentation delegation, and `ModelAndView` returns only.
- Presentation owns form-to-model mapping and JSP-ready model preparation.
- Services receive domain models, not web forms.


