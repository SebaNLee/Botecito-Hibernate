table users {
    id INT PK NN,
    first_name VARCHAR(100) NN,
    last_name VARCHAR (100) NN,
    email VARCHAR(100) UNIQUE NN,
    admin boolean NN,
    phone VARCHAR(30),
    language VARCHAR(10) NN,
    alias VARCHAR (30),
    password_hash VARCHAR(255),
    mail_token VARCHAR (100),
    mail_token_emitted_at TIMESTAMP,
    verified boolean NN;
    created_at TIMESTAMP NN
}

table item_type {
    id INT PK NN,
    name VARCHAR(100) NN,
    slug VARCHAR(100) UNIQUE NN
}

table location {
    id INT PK NN,
    name VARCHAR(100) NN,
    slug VARCHAR(100) UNIQUE NN
}

table image {
    id INT PK NN,
    data BYTEA NN
}

table item {
    id INT PK NN,
    host_id INT FK-ref-users ON-DELETE-SET-NULL,
    status item_status_enum NN,
    created_at TIMESTAMP NN
}

table version {
    id INT PK NN,
    item_id INT FK-ref-item NN ON-DELETE-CASCADE,
    type_id INT FK-ref-item_type NN,
    title VARCHAR (100) NN,
    description VARCHAR (1000),
    price NUMERIC(12,2) NN,
    capacity INT NN,
    weight INT NN,
    difficulty INT NN,
    location_id INT FK-ref-location NN,
    timezone VARCHAR(50) NN,
    created_at TIMESTAMP NN
}

table media {
    version_id INT FK-ref-version NN ON-DELETE-CASCADE,
    image_id INT FK-ref-image ON-DELETE-CASCADE,
    index INT NN,
    PK(version_id, index)
}

table availability {
    id INT PK NN,
    version_id INT FK-ref-version NN ON-DELETE-CASCADE,
    weekday weekday_enum NN,
    start_time time NN,
    end_time time NN
}

table payment_proof {
    id INT PK NN,
    booking_id INT FK-ref-booking UNIQUE NN ON-DELETE-CASCADE,
    filename VARCHAR(100) NN,
    content_type VARCHAR(100) NN,
    file_data BYTEA NN,
    created_at TIMESTAMP NN,
    host_msg VARCHAR(255),
    host_at TIMESTAMP,
    guest_msg VARCHAR(255),
    guest_at TIMESTAMP
}

table booking {
    id INT PK NN,
    version_id INT FK-ref-version NN ON-DELETE-CASCADE,
    guest_id INT FK-ref-users ON-DELETE-SET-NULL,
    start TIMESTAMP NN,
    end TIMESTAMP NN,
    status booking_status_enum NN,
    msg VARCHAR(255),
    created_at TIMESTAMP NN,
    updated_at TIMESTAMP NN
}

table review {
    id INT PK NN,
    booking_id INT FK-ref-booking NN ON-DELETE-CASCADE,
    sender_id INT FK-ref-users ON-DELETE-SET-NULL,
    target_type target_enum NN,
    rating NUMERIC(2,1) NN,
    comment VARCHAR(255),
    created_at TIMESTAMP NN
}

table reports {
    id INT PK NN,
    sender_id INT FK-ref-users ON-DELETE-SET-NULL,
    item_id INT FK-ref-item NN ON-DELETE-CASCADE,
    reason report_enum NN,
    description VARCHAR(255),
    created_at TIMESTAMP NN,
    unique(sender_id, item_id)
}

table favourite {
    user_id    INT FK-ref-users NN ON-DELETE-CASCADE,
    item_id    INT FK-ref-item NN ON-DELETE-CASCADE,
    created_at TIMESTAMP NN,
    PK(user_id, item_id)
}

table subscriptons {
    subscriber_id    INT FK-ref-users NN ON-DELETE-CASCADE,
    subscribed_to_id INT FK-ref-users NN ON-DELETE-CASCADE,
    created_at       TIMESTAMP NN,
    PK(subscriber_id, subscribed_to_id)
}

enum report_enum {
    FAKE, -- Fake, misleading, or inaccurate publication content.
    ABANDONED, -- The publication appears abandoned.
    DUPLICATE, -- The publication is the same as another one.
    SPAM, -- Promotional garbage content.
    IRRELEVANT, -- The publication is unrelated to the platform’s purpose
    INAPPROPRIATE -- The publication contains offensive, explicit, abusive, or otherwise unacceptable content.
    OTHER -- Anything else not covered by the other options.
}

enum item_status_enum {
    ACTIVE,
    INACTIVE,
    DELETED
}

enum weekday_enum {
    MONDAY = 0,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

enum booking_status_enum {
    PENDING,
    ACCEPTED,
    REJECTED,
    PAID,
    CONFIRMED,
    REFUSED,
    CANCELLED,
    FINISHED
}

enum target_enum {
    ITEM,
    USER
}