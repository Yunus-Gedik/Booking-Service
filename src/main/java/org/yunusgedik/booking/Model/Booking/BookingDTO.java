package org.yunusgedik.booking.Model.Booking;

import java.time.LocalDateTime;

public class BookingDTO {
    private Long id;
    private Long userId;
    private Long eventId;
    private BookingStatus status;
    private LocalDateTime bookingTime;
}