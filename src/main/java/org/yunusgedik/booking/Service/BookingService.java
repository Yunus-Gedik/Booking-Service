package org.yunusgedik.booking.Service;

import org.springframework.stereotype.Service;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Repository.BookingRepository;

import java.util.List;

@Service
public class BookingService {

    BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }
}
