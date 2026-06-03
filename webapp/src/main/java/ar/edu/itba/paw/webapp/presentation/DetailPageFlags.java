package ar.edu.itba.paw.webapp.presentation;

/**
 * Viewer-specific flags for the item detail page, computed by the controller
 * from domain services before building the view.
 */
public record DetailPageFlags(boolean favouriteItem, boolean alreadyReported, boolean subscribedToOwner) {}
