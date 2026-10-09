package edu.cmu.cs214.scheduling.workflow;

import edu.cmu.cs214.scheduling.domain.Booking;
import edu.cmu.cs214.scheduling.domain.BookingOutcome;
import edu.cmu.cs214.scheduling.domain.BookingRequest;
import edu.cmu.cs214.scheduling.domain.Room;

interface BookingTypeWorkflow {

    BookingOutcome submit(BookingRequest request, Room room);

    boolean cancel(Booking booking, boolean adminOverride);

    double priceOf(Booking booking);

    String describe(Booking booking);
}
