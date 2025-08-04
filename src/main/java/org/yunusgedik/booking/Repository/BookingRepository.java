package org.yunusgedik.booking.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Model.Booking.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    int countByEventIdAndStatus(Long eventId, BookingStatus status);
}
