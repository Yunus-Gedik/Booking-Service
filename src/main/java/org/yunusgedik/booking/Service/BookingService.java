package org.yunusgedik.booking.Service;

import org.springframework.data.redis.core.StringRedisTemplate;

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
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${event-service.base-url}")
    private String eventServiceBaseUrl;

    @Value("${redis.timeout}")
    private Integer redisTimeout;

    public BookingService(
        BookingRepository bookingRepository,
        ModelMapper modelMapper,
        RestTemplate restTemplate,
        StringRedisTemplate redisTemplate
    ) {
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public Booking create(BookingDTO bookingDTO) {
        Long eventId = bookingDTO.eventId();

        Long fencingToken = acquireLock(eventId);
        String lockKey = "lock:event:" + eventId;

        try {
            EventDTO event = fetchEventDetails(eventId);
            validateEvent(event);
            checkCapacity(event.id(), event.capacity());
            Booking booking = prepareBooking(bookingDTO);

            // Check fencing token right before saving
            validateFencingToken(lockKey, fencingToken);

            return bookingRepository.save(booking);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private Long acquireLock(Long eventId) {
        String counterKey = "lock:counter:event:" + eventId;
        String lockKey = "lock:event:" + eventId;

        Long fencingToken = redisTemplate.opsForValue().increment(counterKey);
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(
                lockKey,
                Objects.requireNonNull(fencingToken).toString(),
                redisTimeout,
                java.util.concurrent.TimeUnit.SECONDS
            );

        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new IllegalStateException("Could not acquire lock for event booking");
        }
        return fencingToken;
    }


    private EventDTO fetchEventDetails(Long eventId) {
        String url = eventServiceBaseUrl + "/event/" + eventId;
        ResponseEntity<EventDTO> response = restTemplate.getForEntity(url, EventDTO.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Event not found");
        }
        return response.getBody();
    }

    private void validateFencingToken(String lockKey, Long expectedToken) {
        String currentToken = redisTemplate.opsForValue().get(lockKey);
        if (!expectedToken.toString().equals(currentToken)) {
            throw new IllegalStateException("Lock lost before save, aborting booking");
        }
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

    public Booking confirm(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        );
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            checkCapacity(booking.getEventId(), fetchEventDetails(booking.getEventId()).capacity());
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        return bookingRepository.save(booking);
    }

    public Booking cancel(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        );
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}
