package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.persistence.EntityManager;

public final class TestUtils {

    private TestUtils() {}

    public static Users insertUnverifiedUser(
            final EntityManager em, final String firstName, final String lastName, final String email) {
        final Users user = new Users();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setLanguage("en");
        user.setVerified(false);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        em.persist(user);
        return user;
    }

    public static Users insertUser(
            final EntityManager em, final String firstName, final String lastName, final String email) {
        final Users user = new Users();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setLanguage("en");
        user.setVerified(true);
        user.setAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        em.persist(user);
        return user;
    }

    public static Location insertLocation(final EntityManager em, final String name, final String slug) {
        final Location loc = new Location();
        loc.setName(name);
        loc.setSlug(slug);
        em.persist(loc);
        return loc;
    }

    public static ItemType insertItemType(final EntityManager em, final String name, final String slug) {
        final ItemType type = new ItemType();
        type.setName(name);
        type.setSlug(slug);
        em.persist(type);
        return type;
    }

    public static Item insertItem(final EntityManager em, final Users host, final ItemStatusEnum status) {
        final Item item = new Item();
        item.setHost(host);
        item.setStatus(status);
        item.setCreatedAt(LocalDateTime.now());
        em.persist(item);
        return item;
    }

    public static Version insertVersion(
            final EntityManager em,
            final Item item,
            final ItemType type,
            final Location location,
            final String title,
            final BigDecimal price,
            final int capacity,
            final int weight,
            final int difficulty,
            final LocalDateTime createdAt) {
        final Version v = new Version();
        v.setItem(item);
        v.setType(type);
        v.setLocation(location);
        v.setTitle(title);
        v.setDescription("Description for " + title);
        v.setPrice(price);
        v.setCapacity(capacity);
        v.setWeight(weight);
        v.setDifficulty(difficulty);
        v.setTimezone("America/Argentina/Buenos_Aires");
        v.setCreatedAt(createdAt);
        em.persist(v);
        return v;
    }

    public static Version insertVersion(
            final EntityManager em, final Item item, final ItemType type, final Location location, final String title) {
        return insertVersion(em, item, type, location, title, BigDecimal.valueOf(100), 4, 200, 2, LocalDateTime.now());
    }

    public static Booking insertBooking(
            final EntityManager em, final Version version, final Users guest, final BookingStatusEnum status) {
        final Booking b = new Booking();
        b.setVersion(version);
        b.setGuest(guest);
        b.setStart(LocalDateTime.now().plusDays(1));
        b.setEnd(LocalDateTime.now().plusDays(2));
        b.setStatus(status);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        em.persist(b);
        return b;
    }

    public static Review insertReview(
            final EntityManager em,
            final Booking booking,
            final Users sender,
            final TargetEnum targetType,
            final double rating,
            final String comment) {
        final Review r = new Review();
        r.setBooking(booking);
        r.setSender(sender);
        r.setTargetType(targetType);
        r.setRating(BigDecimal.valueOf(rating));
        r.setComment(comment);
        r.setCreatedAt(LocalDateTime.now());
        em.persist(r);
        return r;
    }

    public static Availability insertAvailability(
            final EntityManager em,
            final Version version,
            final WeekdayEnum weekday,
            final LocalTime startTime,
            final LocalTime endTime) {
        final Availability a = new Availability();
        a.setVersion(version);
        a.setWeekday(weekday);
        a.setStartTime(startTime);
        a.setEndTime(endTime);
        em.persist(a);
        return a;
    }

    public static Image insertImage(final EntityManager em) {
        final Image img = new Image();
        img.setData(new byte[] {1, 2, 3});
        em.persist(img);
        return img;
    }

    public static Media insertMedia(final EntityManager em, final Version version, final Image image, final int index) {
        final Media media = new Media();
        media.setId(new MediaId(version.getId(), index));
        media.setVersion(version);
        media.setImage(image);
        em.persist(media);
        return media;
    }

    public static Report insertReport(
            final EntityManager em,
            final Users sender,
            final Item item,
            final ReportEnum reason,
            final String description) {
        final Report report = Report.builder()
                .sender(sender)
                .item(item)
                .reason(reason)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        em.persist(report);
        return report;
    }

    public static Subscription insertSubscription(
            final EntityManager em, final Users subscriber, final Users subscribedTo) {
        final Subscription sub = new Subscription();
        sub.setId(new SubscriptionId(subscriber.getId(), subscribedTo.getId()));
        sub.setSubscriber(subscriber);
        sub.setSubscribedTo(subscribedTo);
        sub.setCreatedAt(LocalDateTime.now());
        em.persist(sub);
        return sub;
    }

    public static Booking createBooking(
            final Version version,
            final Users guest,
            final LocalDateTime start,
            final LocalDateTime end,
            final BookingStatusEnum status) {
        final Booking b = new Booking();
        b.setVersion(version);
        b.setGuest(guest);
        b.setStart(start);
        b.setEnd(end);
        b.setStatus(status);
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        return b;
    }
}
