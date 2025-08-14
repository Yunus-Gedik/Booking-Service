package org.yunusgedik.booking.Model.Event;

import java.time.LocalDateTime;

public record Event(
    Long id,
    String title,
    LocalDateTime eventDate,
    int capacity,
    boolean active,
    double price
) { }