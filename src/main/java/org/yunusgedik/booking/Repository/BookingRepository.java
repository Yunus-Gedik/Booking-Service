package org.yunusgedik.booking.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yunusgedik.booking.Model.Booking.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
