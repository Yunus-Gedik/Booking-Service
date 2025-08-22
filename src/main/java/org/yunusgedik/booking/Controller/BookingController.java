package org.yunusgedik.booking.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.yunusgedik.booking.Model.Booking.Booking;
import org.yunusgedik.booking.Model.Booking.BookingDTO;
import org.yunusgedik.booking.Service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {

    BookingService bookingService;

    BookingController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking getByParam(@RequestParam(name = "id") Long id) {
        return bookingService.get(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking get(@PathVariable Long id) {
        return bookingService.get(id);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Booking> getAll() {
        return bookingService.getAll();
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Booking create(@RequestBody BookingDTO bookingDTO) {
        return this.bookingService.create(bookingDTO);
    }

    @PatchMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking update(@RequestBody BookingDTO bookingDTO, @PathVariable Long id) {
        return this.bookingService.update(id, bookingDTO);
    }

    @DeleteMapping()
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking delete(@RequestParam(name = "id") Long id) {
        return bookingService.delete(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @belongCheck.isOwner(#id, principal)")
    public Booking cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

}
