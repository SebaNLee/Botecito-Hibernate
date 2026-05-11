package ar.edu.itba.paw.services;

public interface BookingDashboardService {
    GuestTripsDashboardData loadGuestTripsDashboard(int guestId, int page, int pageSize);
}
