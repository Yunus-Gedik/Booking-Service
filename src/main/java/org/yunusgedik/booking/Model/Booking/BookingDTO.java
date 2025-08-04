package org.yunusgedik.booking.Model.Booking;

import java.time.LocalDateTime;

public record BookingDTO (
    Long id,
    Long userId,
    Long eventId,
    BookingStatus status,
    LocalDateTime bookingTime
){}