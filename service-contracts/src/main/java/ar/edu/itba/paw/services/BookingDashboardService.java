package ar.edu.itba.paw.services;

public interface BookingDashboardService {

    OwnerBoatsDashboardData loadOwnerBoatsDashboard(int ownerId, int page, int pageSize);

    GuestTripsDashboardData loadGuestTripsDashboard(int guestId, int page, int pageSize);
}
