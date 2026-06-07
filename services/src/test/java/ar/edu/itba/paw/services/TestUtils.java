package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;

public final class TestUtils {

    private TestUtils() {}

    public static Users user(final int id) {
        final Users u = new Users();
        u.setId(id);
        return u;
    }

    public static Users user(final int id, final String email, final String firstName, final String lastName) {
        final Users u = user(id);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        return u;
    }

    public static Item item(final int id) {
        return Item.builder().id(id).build();
    }

    public static Item item(final int id, final Users host) {
        return Item.builder().id(id).host(host).build();
    }

    public static Item item(final int id, final Users host, final ItemStatusEnum status) {
        return Item.builder().id(id).host(host).status(status).build();
    }

    public static Item item(final int id, final Users host, final ItemStatusEnum status, final Version latestVersion) {
        return Item.builder()
                .id(id)
                .host(host)
                .status(status)
                .latestVersion(latestVersion)
                .build();
    }

    public static Version version(final int id) {
        return Version.builder().id(id).build();
    }

    public static Version version(final int id, final Item item) {
        return Version.builder().id(id).item(item).build();
    }

    public static Booking booking(final Version version, final Users guest, final BookingStatusEnum status) {
        return Booking.builder()
                .version(version)
                .guest(guest)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
