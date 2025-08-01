package org.yunusgedik.booking.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Service.BookingService;

import java.util.List;

@RestController("/booking")
public class BookingController {

    BookingService bookingService;

    BookingController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @GetMapping()
    public Booking getByParam(@RequestParam(name = "id") Long id) {
        return bookingService.get(id);
    }

    @GetMapping("/{id}")
    public Booking get(@PathVariable Long id) {
        return bookingService.get(id);
    }

    @GetMapping("/all")
    public List<Booking> getAll() {
        return bookingService.getAll();
    }

}
