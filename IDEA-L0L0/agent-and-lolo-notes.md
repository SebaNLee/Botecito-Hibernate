
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

## Favourites implementation notes

- `persistence/src/main/resources/db/migration/V28__favourites.sql`: adds the `favourite` join table for saved user-item relationships.
- `models/src/main/java/ar/edu/itba/paw/models/entity/Favourite.java` and `FavouriteId.java`: add the JPA entity and composite key for favourite rows.
- `persistence-contracts/src/main/java/ar/edu/itba/paw/persistence/FavouriteDao.java`: defines persistence operations for creating, deleting, checking, and listing favourites.
- `persistence/src/main/java/ar/edu/itba/paw/persistence/FavouriteJpaDao.java`: implements favourite persistence and loads favourite items with their latest versions for listing cards.
- `service-contracts/src/main/java/ar/edu/itba/paw/services/FavouriteService.java`: exposes the service contract used by web/presentation code.
- `services/src/main/java/ar/edu/itba/paw/services/FavouriteServiceImpl.java`: enforces favourite rules such as no own-item favourites and no deleted-item favourites.
- `services/src/test/java/ar/edu/itba/paw/services/FavouriteServiceImplTest.java`: covers the main service rules and idempotent add/remove behavior.
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/FavouriteController.java`: adds skinny routes for the favourites page and favourite/unfavourite POST actions.
- `webapp/src/main/java/ar/edu/itba/paw/webapp/presentation/FavouritePresentation.java`: prepares the favourites page model, handles toasts, redirects, and PRG flow.
- `webapp/src/main/java/ar/edu/itba/paw/webapp/controller/MarketplaceController.java` and `MarketplacePresentation.java`: pass the logged-in user and add per-card favourite state to marketplace results.
- `webapp/src/main/java/ar/edu/itba/paw/webapp/presentation/DetailPresentation.java`: adds favourite state for the item detail page.
- `webapp/src/main/webapp/WEB-INF/views/favourites.jsp`: renders the authenticated favourites list with existing listing-card UI.
- `webapp/src/main/webapp/WEB-INF/tags/listingCard.tag`: adds optional favourite/unfavourite controls while preserving the existing card link behavior.
- `webapp/src/main/webapp/WEB-INF/views/marketplace.jsp` and `item-detail.jsp`: render favourite actions in marketplace cards and item detail.
- `webapp/src/main/webapp/WEB-INF/tags/siteHeader.tag`: adds the authenticated header link to `/favourites`.
- `webapp/src/main/resources/i18n/messages.properties` and `messages_es.properties`: add English and Spanish labels, titles, empty states, and toast messages for favourites.
