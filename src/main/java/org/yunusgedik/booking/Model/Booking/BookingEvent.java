package org.yunusgedik.booking.Model.Booking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {
    private Long bookingId;
    private Long userId;
    private Long eventId;
    private BookingStatus status;
    private Double price;
    private LocalDateTime timestamp;
}
