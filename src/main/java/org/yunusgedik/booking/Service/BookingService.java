package org.yunusgedik.booking.Service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Model.Booking.BookingDTO;
import org.yunusgedik.booking.Model.Booking.BookingStatus;
import org.yunusgedik.booking.Model.Event.EventDTO;
import org.yunusgedik.booking.Repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;

    @Value("${event-service.base-url}")
    private String eventServiceBaseUrl;


    public BookingService(
        BookingRepository bookingRepository,
        ModelMapper modelMapper,
        RestTemplate restTemplate
    ) {
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
        this.restTemplate = restTemplate;
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

//    public Booking create(BookingDTO bookingDTO) {
//        Booking booking = new Booking();
//        modelMapper.map(bookingDTO, booking);
//        return bookingRepository.save(booking);
//    }

    public Booking create(BookingDTO bookingDTO) {
        EventDTO event = fetchEventDetails(bookingDTO.eventId());
        validateEvent(event);
        checkCapacity(event.id(), event.capacity());
        Booking booking = prepareBooking(bookingDTO);
        return bookingRepository.save(booking);
    }

    private EventDTO fetchEventDetails(Long eventId) {
        String url = eventServiceBaseUrl + "/event/" + eventId;
        ResponseEntity<EventDTO> response = restTemplate.getForEntity(url, EventDTO.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Event not found");
        }
        return response.getBody();
    }

    private void validateEvent(EventDTO event) {
        if (!event.active()) {
            throw new IllegalStateException("Event is not active");
        }
    }

    private void checkCapacity(Long eventId, int capacity) {
        int currentBookings = bookingRepository.countByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
        if (currentBookings >= capacity) {
            throw new IllegalStateException("Event is full");
        }
    }

    private Booking prepareBooking(BookingDTO bookingDTO) {
        Booking booking = modelMapper.map(bookingDTO, Booking.class);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
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
