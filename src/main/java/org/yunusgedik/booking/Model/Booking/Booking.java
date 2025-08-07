package org.yunusgedik.booking.Model.Booking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // comes from Auth/User service

    private Long eventId; // refers to Event service

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime bookingTime;

    private Instant createdAt;
}