package org.yunusgedik.booking.Security;

import org.springframework.stereotype.Component;
import org.yunusgedik.booking.Service.BookingService;

@Component("belongCheck")
public class BelongCheck {
    private final BookingService bookingService;

    BelongCheck(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public boolean isOwner(Long bookingId, Long userId) {
        return bookingService.get(bookingId).getUserId().equals(userId);
    }
}
