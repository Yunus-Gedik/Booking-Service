package org.yunusgedik.booking.Model.Event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class Event{
    Long id;
    String title;
    LocalDateTime eventDate;
    int capacity;
    boolean active;
    double price;
}