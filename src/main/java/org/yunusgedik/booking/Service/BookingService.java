package org.yunusgedik.booking.Service;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Model.Booking.BookingDTO;
import org.yunusgedik.booking.Repository.BookingRepository;

import java.util.List;

@Service
public class BookingService {

    BookingRepository bookingRepository;
    ModelMapper modelMapper;

    public BookingService(BookingRepository bookingRepository, ModelMapper modelMapper) {
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public Booking create(BookingDTO bookingDTO) {
        Booking booking = new Booking();
        modelMapper.map(bookingDTO, booking);
        return bookingRepository.save(booking);
    }

    public Booking update(Long id, BookingDTO bookingDTO) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        );
        modelMapper.map(bookingDTO, booking);
        return bookingRepository.save(booking);
    }

    public Booking delete(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        );
        bookingRepository.delete(booking);
        return booking;
    }
}
