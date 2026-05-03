# Controller Refactor — Restore Layer Boundaries

## Context

Webapp controllers have drifted into doing service-layer work: parsing/validating availability, computing slot conflicts, assembling multi-source view models, validating payment-proof magic bytes, and orchestrating multi-step booking/edit flows. Per `CLAUDE.md`, controllers must only handle HTTP, form binding, validation, and view selection. This refactor moves business logic into existing service interfaces (extending them, not creating many new ones), introduces DTOs in `service-contracts` for view assemblies, and leaves the controllers as thin HTTP adapters.

## Target Architecture (per CLAUDE.md)

- Controllers (`webapp`): request parsing, `@Valid` form binding, calling **one** service entry-point per concern, packaging the result into the `ModelAndView`.
- Services (`services` impl, `service-contracts` ifaces): business rules, multi-step orchestration, transactional boundaries, notifications.
- DTOs returned to the web layer live in `service-contracts` (e.g. `ar.edu.itba.paw.services.dto`).
- Helpers in `webapp` keep ONLY web concerns: `ItemImageUtils` (URL resolution), `ToastSupport` (flash messages). `AvailabilityPickerSupport`'s logic moves into `ItemService` / new view-DTO methods; only a thin web-layer adapter remains if needed.
- Spring Security auto-login moves to a new `webapp/auth` helper, not into services (Spring Security coupling).

## New DTOs (in `service-contracts`, package `ar.edu.itba.paw.services.dto`)

- `OwnerDashboardView` — aggregates: items page, received bookings, deletion flags, image map, pending-review-by-booking map (replaces `addMyBoatsData` work).
- `GuestTripsView` — sent bookings with snapshots, image URLs, pending and authored reviews (replaces `addMyTripsData`).
- `ReceivedBookingRow`, `SentBookingRow` — per-row DTO replacing ad-hoc `Map<String,Object>` lists.
- `MarketplaceItemView` — item + snapshots + reviews + rating summary + booking state + availability picker payload + visibility flags.
- `MarketplaceSearchCriteria` — validated filters (difficulty 1–5, rating 1–5, capacity, weight, time range).
- `AvailabilityPickerData` — calendar grid currently built by `AvailabilityPickerSupport`.
- `PublicationEditView` / `PublicationDraft` — current publication state + change-detection payload for `PublishActionController`.
- `PaymentProofUpload` — bytes + content-type; service validates magic bytes and returns a domain result.
- `BookingDecisionBatch` — token + accept/decline pairs for edit-conflict resolution.

## Service Interface Extensions (in `service-contracts`)

`ItemService`
- `OwnerDashboardView buildOwnerDashboard(long ownerId, Pageable …)`
- `MarketplaceItemView buildMarketplaceItemView(long itemId, Long viewerUserId, MarketplaceSearchCriteria filters)`
- `Page<ItemSummary> searchMarketplace(MarketplaceSearchCriteria criteria, Pageable …)`  — pulls all the difficulty/rating/capacity/availability filtering currently in `MarketplaceController`.
- `MarketplaceSearchCriteria parseAndValidateCriteria(Map<String,String> rawParams)` — replaces `parseDifficultyLevel`, `parseMinAverageRating`, `buildItemSearchCriteria`.
- `AvailabilityPickerData buildAvailabilityPicker(long itemId, LocalDate weekStart)` — replaces `AvailabilityPickerSupport`.
- `void validatePublicationDraft(PublicationDraft draft)` — encapsulates 2-hr min, 30-min gap, no-overlap rules from `PublishController.validateAvailabilityStep` / `validateDaySlots`.
- `Item createPublication(PublicationDraft draft, List<GalleryImageUpload> images)` — already partially exists; absorb image MIME / 10-image cap checks (currently in `PublishController` and `GalleryController`).
- `boolean hasPublicationChanges(long itemId, PublicationDraft draft)` — from `PublishActionController`.
- `void resolveEditConflict(long itemId, BookingDecisionBatch decisions)` — wraps batch accept/decline.

`BookingRequestService`
- `GuestTripsView buildGuestTrips(long guestUserId, Pageable …)`
- `void submitPaymentProof(long bookingId, long actorUserId, PaymentProofUpload upload)` — moves magic-byte / size / MIME validation in.
- `boolean canAccessPaymentProof(long bookingId, long viewerUserId)` — domain access rule.
- `BookingRequest createRequest(long itemId, long guestUserId, ItemBookingForm form)` — absorbs self-booking + availability conflict checks.
- All state-transition methods (`accept`, `decline`, `confirmPaymentReceived`, `refusePaymentProof`, block/unblock slot) own their `MailService` calls.

`UserService`
- `Optional<UserAccount> register(RegisterCommand cmd)` — already exists; ensure it's a single entry-point (no duplicate-email check in controller).
- `UserProfileView loadProfile(long userId)` if the controller currently builds one.

`ReviewService` — already adequate; no new methods.

`MailService` — invoked **only from services**. Remove direct injections from controllers where they exist.

## Webapp-layer Helpers

- New `webapp/auth/PostRegistrationAuthenticator` — wraps `UsernamePasswordAuthenticationToken` + `SecurityContextHolder` + session creation. Called by `AuthController.register` after `userService.register` succeeds. Stays in `webapp` because of Spring Security coupling.
- Keep `ItemImageUtils` (pure URL builder) and `ToastSupport` (flash messages).
- Delete `AvailabilityPickerSupport` from controller package after its logic is in `ItemService` / `AvailabilityPickerData`.

## Per-Controller Changes

**AuthController** (1475 → target ~400 lines)
- `register` POST: call `userService.register`, then `postRegistrationAuthenticator.authenticate(req, user)`. No SecurityContext code in controller.
- `myBoats` GET: replace `addMyBoatsData` + `buildReceivedBookings` (lines ~358–822) with `itemService.buildOwnerDashboard(ownerId, pageable)`; iterate `OwnerDashboardView` into the model.
- `bookings` GET: replace `addMyTripsData` with `bookingRequestService.buildGuestTrips(...)`.
- `profile` POST: `userService.updateProfile` already does the work; keep the principal-refresh in controller (Spring Security concern).
- Password recovery endpoints stay (already thin).

**PublishController** (712 → ~250)
- POST publish: bind `PublishBoatForm` → map to `PublicationDraft` → `itemService.validatePublicationDraft` (BindingResult on failure) → `itemService.createPublication(draft, uploads)`. Drop `syncAvailabilityFromRequest`, `validateAvailabilityStep`, `validateDaySlots` (move into service via `parseAvailability(Map<String,String> raw)` helper on the form / service).
- Image MIME + 10-cap validation moves into service.

**PublishActionController** (508 → ~200)
- Edit endpoint: `itemService.hasPublicationChanges` + `itemService.resolveEditConflict` + `itemService.updatePublication`.
- Booking display formatting (`formatBookingForDisplay` etc.) moves to either `BookingRequestService.buildEditConflictView` or a JSP-layer formatter tag — prefer service DTO with already-formatted fields.

**MarketplaceController** (553 → ~180)
- GET list: `itemService.parseAndValidateCriteria(params)` → `itemService.searchMarketplace(criteria, page)`.
- GET item detail: `itemService.buildMarketplaceItemView(id, viewerId, criteria)`.
- POST booking request: `bookingRequestService.createRequest(itemId, guestId, form)` — service does self-booking, availability, conflict checks.

**PublicationAvailabilityController** (524 → ~150)
- Replace all slot-state computation with `itemService.buildAvailabilityPicker(itemId, weekStart)` returning `AvailabilityPickerData`.
- `blockSlot` / `unblockSlot`: `bookingRequestService.blockSlot(itemId, ownerId, BlockSlotForm)` / `unblockSlot(...)` — service parses dates, validates, transitions.

**BookingRequestActionController** (327 → ~130)
- `submitPaymentProof`: `bookingRequestService.submitPaymentProof(...)`. Service runs `isValidPaymentProof` + `matchesMagicBytes` (move from controller).
- `viewPaymentProof`: `bookingRequestService.canAccessPaymentProof(bookingId, viewerId)` guard, then stream bytes.
- `accept` / `decline` / `confirmPaymentReceived` / `refusePaymentProof`: already thin; ensure each owns its own mail-sending in the service impl.
- Drop `findBookingById` private helper — use service.

**GalleryController** (179 → ~80)
- File validation (`upload`) → `itemService.uploadGalleryImage(itemId, ownerId, GalleryImageUpload)`.
- Reorder → `itemService.reorderGallery(itemId, ownerId, List<Long> idsInOrder)`.

**ReviewController** — already thin, leave as-is.
**ItemController / HomeController / ImageController** — thin, no changes beyond removing `AvailabilityPickerSupport` static call from `HomeController` (replace with `itemService.buildAvailabilityPicker` if it actually uses it; otherwise leave).
**ItemImageUtils / ToastSupport** — keep as web helpers.

## Critical Files

To modify:
- `webapp/.../controller/*.java` — every controller listed above.
- `webapp/.../auth/PostRegistrationAuthenticator.java` — NEW.
- `service-contracts/.../services/ItemService.java`
- `service-contracts/.../services/BookingRequestService.java`
- `service-contracts/.../services/UserService.java`
- `service-contracts/.../services/dto/*.java` — NEW DTOs listed above.
- `services/.../services/ItemServiceImpl.java`
- `services/.../services/BookingRequestServiceImpl.java`
- `services/.../services/UserServiceImpl.java`

To delete:
- `webapp/.../controller/AvailabilityPickerSupport.java` (after logic migration).

## Tests (per user direction: contract-style, no `Mockito.verify`)

- Service tests under `services/src/test`: drive each new service method through its contract — given inputs, assert returned DTO state and observable side-effects via re-querying mocked DAOs (stubbed returns). No `verify(...)`. Use plain JUnit + Mockito stubs only for DAO returns.
- Controller tests under `webapp/src/test` (existing `AuthControllerTest`, `HomeControllerTest`, `ImageControllerTest`, `ErrorControllerTest`): update mocks to the new service signatures; assert HTTP/view outcomes only. They should shrink — controller logic is now mostly delegation.
- Add controller tests for the previously-untested big controllers (Marketplace, Publish, PublishAction, PublicationAvailability, BookingRequestAction, Gallery, Review) covering: happy path, validation failures, authorization rejection. Web-layer `MockMvc`-style if already used; otherwise plain unit tests against the controller method returning `ModelAndView`.
- Persistence tests untouched (no DAO changes).

## Execution Order (single PR, but ordered to keep compile green)

1. Add new DTOs to `service-contracts`.
2. Add new methods to service interfaces (default-implementation-free; will fail to compile in `services` until step 3).
3. Implement new methods in `services`, moving logic from controllers/helpers verbatim, then refactor for clarity. Run service tests.
4. Add `PostRegistrationAuthenticator` in `webapp/auth`.
5. Refactor controllers one by one, deleting now-dead private methods. Compile after each.
6. Delete `AvailabilityPickerSupport` once nothing references it.
7. Update existing controller tests; add new ones.
8. Run full Maven build.

## Verification

- `mvn -pl services test` — new service methods covered (contract assertions on returned DTO + state, no `verify`).
- `mvn -pl webapp test` — controller tests pass; new controller tests added.
- `mvn -pl persistence test` — unchanged, must still pass.
- `mvn clean install` — full build green.
- Manual smoke: start the app (`mvn -pl webapp tomcat7:run` or the project's run target), hit:
  - `/register` (auto-login should still work),
  - `/my-boats` and `/bookings` (dashboards render with bookings, images, pending reviews),
  - `/marketplace` with filters (difficulty, rating, time range),
  - publication detail page → request booking,
  - `/publish` create flow with availability + images,
  - `/publish/edit` with pending bookings (decision batch),
  - `/publication/{id}/availability` block/unblock slot,
  - payment proof upload + view + refuse + confirm.
- Sanity check controller line counts: AuthController target ≤ 450, PublishController ≤ 250, MarketplaceController ≤ 180, PublicationAvailabilityController ≤ 150, BookingRequestActionController ≤ 130.
- Grep for forbidden patterns afterward: `JdbcTemplate` in `webapp/`, direct DAO injections in controllers, `MailService` injected into controllers — should yield zero matches.
