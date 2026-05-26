
todo:

## As a user, I want to subscribe to a user so that I get notified about
  their activities

  - Add a table in the DB for subscriptions, aka one user is to another user. This would be a follow table.
  
  - Notification email to subscribed users when someone publishes a new item. Aka when I publish a boat, send an email to everyone follows me.

  - Item details: from here, a user can subscribe/unsubscribe to a specific user. In the card where the owner’s contact info is shown, add a conditional button that says subscribe or unsubscribe as appropriate.

  - In /profile, add the user’s subscription list so they can cancel subscriptions from there too. In /profile, add a dropdown/list of all the people I follow with very simple cards, with a button like the one in item details.


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


## What we implemented

- Added user subscriptions as a follow relationship between users.
- Users can subscribe/unsubscribe from the item detail owner card.
- The profile page now shows the people the user follows, with pagination and
  unsubscribe actions.
- Publishing now notifies verified followers by email after the publish
  transaction commits.
- The publisher confirmation email still sends, but it now also runs after the
  transaction commits.
- Subscription listing code was kept paginated only; the old unpaged service
  path was removed.
- The item detail page was adjusted so everything scrolls normally with the
  document. No sticky right panel and no internal sidebar scrolling.
- The host/contact card was moved under the right-side item panel.
- The old direct "send email" mailto button was removed from the host card.
