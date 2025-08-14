package org.yunusgedik.booking.Service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.yunusgedik.booking.Model.Booking.BookingEvent;

@Service
public class BookingEventProducer {
    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    public BookingEventProducer(KafkaTemplate<String, BookingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBookingEvent(BookingEvent event) {
        kafkaTemplate.send("booking-events", String.valueOf(event.getBookingId()), event);
    }
}