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
import org.yunusgedik.booking.Model.Booking.BookingEvent;
import org.yunusgedik.booking.Model.Booking.BookingStatus;
import org.yunusgedik.booking.Model.Event.Event;
import org.yunusgedik.booking.Repository.BookingRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;
    private final BookingEventProducer bookingEventProducer;

    @Value("${event-service.base-url}")
    private String eventServiceBaseUrl;

    @Value("${redis.timeout}")
    private Integer redisTimeout;

    public BookingService(
        BookingRepository bookingRepository,
        ModelMapper modelMapper,
        RestTemplate restTemplate,
        StringRedisTemplate redisTemplate,
        BookingEventProducer bookingEventProducer
    ) {
        this.bookingRepository = bookingRepository;
        this.modelMapper = modelMapper;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.bookingEventProducer = bookingEventProducer;
    }

    public Booking get(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public Booking create(BookingDTO bookingDTO) {
        Long eventId = bookingDTO.eventId();
        String counterKey = "lock:counter:event:" + eventId;
        String lockKey = "lock:event:" + eventId;

        Long fencingToken = acquireLock(counterKey, lockKey);

        try {
            Event event = fetchEventDetails(eventId);
            validateEvent(event);
            checkCapacity(event.getId(), event.getCapacity());
            Booking booking = prepareBooking(bookingDTO);

            // Check fencing token right before saving
            validateFencingToken(lockKey, fencingToken, "Lock lost before save, aborting booking");

            Booking bookingSaved = bookingRepository.save(booking);

            produceKafkaBookingEvent(bookingSaved, event.getPrice());

            return bookingSaved;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private Long acquireLock(String counterKey, String lockKey) {
        Long fencingToken = redisTemplate.opsForValue().increment(counterKey);
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(
                lockKey,
                Objects.requireNonNull(fencingToken).toString(),
                redisTimeout,
                java.util.concurrent.TimeUnit.SECONDS
            );

        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new IllegalStateException("Could not acquire lock");
        }
        return fencingToken;
    }

    private Event fetchEventDetails(Long eventId) {
        String url = eventServiceBaseUrl + "/event/" + eventId;
        ResponseEntity<Event> response = restTemplate.getForEntity(url, Event.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Event not found");
        }
        return response.getBody();
    }

    private void validateFencingToken(String lockKey, Long expectedToken, String throwString) {
        String currentToken = redisTemplate.opsForValue().get(lockKey);
        if (!expectedToken.toString().equals(currentToken)) {
            throw new IllegalStateException(throwString);
        }
    }

    private void validateEvent(Event event) {
        if (!event.isActive()) {
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
        booking.setCreatedAt(Instant.now());
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
        Event event = fetchEventDetails(booking.getEventId());

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            if (isCapacityFull(event)) {
                booking.setStatus(BookingStatus.WAITLISTED);
            } else {
                booking.setStatus(BookingStatus.CONFIRMED);
            }
        }

        Booking bookingSaved = bookingRepository.save(booking);

        Double price = (bookingSaved.getStatus() == BookingStatus.WAITLISTED)
            ? null
            : event.getPrice();

        produceKafkaBookingEvent(bookingSaved, price);

        return bookingSaved;
    }

    public Booking cancel(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found")
        );
        booking.setStatus(BookingStatus.CANCELLED);
        Booking bookingSaved = bookingRepository.save(booking);

        Event event = fetchEventDetails(bookingSaved.getEventId());
        produceKafkaBookingEvent(bookingSaved, event.getPrice());

        promoteWaitlisted(bookingSaved.getEventId());
        return bookingSaved;
    }

    private boolean isCapacityFull(Event event) {
        return bookingRepository.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED)
               >= event.getCapacity();
    }

    private void promoteWaitlisted(Long eventId) {
        String counterKey = "lock:counter:promote:event:" + eventId;
        String lockKey = "lock:promote:event:" + eventId;

        Long fencingToken = acquireLock(counterKey, lockKey);

        try {
            Booking waitlisted = bookingRepository
                .findFirstByEventIdAndStatusOrderByBookingTimeAsc(eventId, BookingStatus.WAITLISTED);
            if (waitlisted != null) {
                // Validate fencing token before proceeding
                validateFencingToken(lockKey, fencingToken, "Lock lost before promotion, aborting");

                waitlisted.setStatus(BookingStatus.CONFIRMED);
                Booking bookingSaved = bookingRepository.save(waitlisted);

                Event event = fetchEventDetails(bookingSaved.getEventId());
                produceKafkaBookingEvent(bookingSaved, event.getPrice());
            }
        } finally {
            redisTemplate.delete(lockKey);
        }

    }

    private void produceKafkaBookingEvent(Booking booking, Double price) {
        bookingEventProducer.sendBookingEvent(
            new BookingEvent(
                booking.getId(),
                booking.getUserId(),
                booking.getEventId(),
                booking.getStatus(),
                price,
                LocalDateTime.now()
            )
        );
    }
}
