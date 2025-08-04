package org.yunusgedik.booking.Model.Event;

import java.time.LocalDateTime;

public record EventDTO(
    Long id,
    String title,
    LocalDateTime eventDate,
    int capacity,
    boolean active
) { }