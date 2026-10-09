package edu.cmu.cs214.scheduling.workflow;

import edu.cmu.cs214.scheduling.domain.Booking;
import edu.cmu.cs214.scheduling.domain.BookingStore;
import edu.cmu.cs214.scheduling.domain.Member;
import edu.cmu.cs214.scheduling.domain.Room;
import edu.cmu.cs214.scheduling.domain.TimeSlot;
import edu.cmu.cs214.scheduling.notify.NotificationHub;
import edu.cmu.cs214.scheduling.pricing.PriceCalculator;

abstract class AbstractBookingTypeWorkflow implements BookingTypeWorkflow {

    protected static final String FACILITIES_CONTACT = "facilities@rooms.example.edu";

    protected final BookingStore store;
    protected final PriceCalculator calculator;
    protected final NotificationHub hub;

    AbstractBookingTypeWorkflow(BookingStore store, PriceCalculator calculator,
                                NotificationHub hub) {
        this.store = store;
        this.calculator = calculator;
        this.hub = hub;
    }

    protected String roomNameFor(Booking booking) {
        Room room = store.findRoom(booking.getRoomId());
        return room == null ? booking.getRoomId() : room.getName();
    }

    protected boolean overlaps(TimeSlot slot, Booking existing) {
        return existing.getStart().compareTo(slot.end()) < 0
                && slot.start().compareTo(existing.getEnd()) < 0;
    }

    protected String recipientFor(Member member) {
        return member == null ? FACILITIES_CONTACT : member.getEmail();
    }
}
