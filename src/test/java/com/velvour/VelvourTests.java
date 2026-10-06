package com.velvour;

import com.velvour.Models.*;
import com.velvour.Services.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VELVOUR business logic: billing, availability, bookings,
 * check-in/out workflow, and payment handling.
 */
public class VelvourTests {

    private Hotel hotel;
    private Billing billing;
    private DataStore store;

    @BeforeEach
    void setUp() {
        store = DataStore.get();
        hotel = new Hotel();
        billing = new Billing();
    }

    // ================= BILLING =================

    @Test
    void roomChargeIsNightsTimesBasePrice() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("101"); // STANDARD = 120.0
        LocalDate ci = LocalDate.now().plusDays(10);
        LocalDate co = ci.plusDays(3);
        Booking b = new Booking("BT1", g, r, ci, co, BookingStatus.RESERVED, null);

        assertEquals(3, b.getNights());
        assertEquals(360.0, billing.roomCharge(b), 0.001);
    }

    @Test
    void perNightServicesAreMultipliedByNights() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("101");
        LocalDate ci = LocalDate.now().plusDays(10);
        Booking b = new Booking("BT2", g, r, ci, ci.plusDays(2), BookingStatus.RESERVED, null);

        HotelService breakfast = new HotelService("BRK", "Breakfast", 25.0, true);
        b.addService(breakfast, 2); // 25 * 2 qty * 2 nights = 100

        assertEquals(100.0, billing.servicesCharge(b), 0.001);
    }

    @Test
    void oneTimeServicesAreNotMultipliedByNights() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("101");
        LocalDate ci = LocalDate.now().plusDays(10);
        Booking b = new Booking("BT3", g, r, ci, ci.plusDays(5), BookingStatus.RESERVED, null);

        HotelService spa = new HotelService("SPA", "Spa", 120.0, false);
        b.addService(spa, 1); // flat 120 regardless of nights

        assertEquals(120.0, billing.servicesCharge(b), 0.001);
    }

    @Test
    void discountIsAppliedToSubtotal() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("101");
        LocalDate ci = LocalDate.now().plusDays(10);
        Booking b = new Booking("BT4", g, r, ci, ci.plusDays(2), BookingStatus.RESERVED, null);
        b.setDiscountPercent(10.0);

        double subtotal = billing.subtotal(b); // 2 * 120 = 240
        assertEquals(240.0, subtotal, 0.001);
        assertEquals(24.0, billing.discount(b), 0.001);
        assertEquals(216.0, billing.taxableAmount(b), 0.001);
    }

    @Test
    void taxIsTwelvePercentOfTaxableAmount() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("101");
        LocalDate ci = LocalDate.now().plusDays(10);
        Booking b = new Booking("BT5", g, r, ci, ci.plusDays(1), BookingStatus.RESERVED, null);

        // subtotal 120, no discount, tax 12% = 14.40
        assertEquals(14.40, billing.tax(b), 0.001);
        assertEquals(134.40, billing.total(b), 0.001);
    }

    @Test
    void totalCombinesRoomServicesDiscountAndTax() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("201"); // DELUXE 220
        LocalDate ci = LocalDate.now().plusDays(20);
        Booking b = new Booking("BT6", g, r, ci, ci.plusDays(3), BookingStatus.RESERVED, null);
        b.setDiscountPercent(10.0);
        b.addService(new HotelService("SPA", "Spa", 120.0, false), 1);
        b.addService(new HotelService("BRK", "Breakfast", 25.0, true), 2);

        // room 3 * 220 = 660
        // spa 120
        // breakfast 25 * 2 * 3 = 150
        // subtotal 930, discount 93, taxable 837, tax 100.44, total 937.44
        assertEquals(930.0, billing.subtotal(b), 0.001);
        assertEquals(93.0, billing.discount(b), 0.001);
        assertEquals(100.44, billing.tax(b), 0.001);
        assertEquals(937.44, billing.total(b), 0.001);
    }

    // ================= AVAILABILITY =================

    @Test
    void roomIsAvailableWhenNoOverlap() {
        Room r = store.findRoom("102"); // has a booking BK5004 in 2..5 days
        LocalDate ci = LocalDate.now().plusDays(10);
        assertTrue(hotel.isAvailable(r, ci, ci.plusDays(2), null));
    }

    @Test
    void roomIsNotAvailableWhenDatesOverlap() {
        Room r = store.findRoom("102");
        LocalDate ci = LocalDate.now().plusDays(3);
        assertFalse(hotel.isAvailable(r, ci, ci.plusDays(2), null));
    }

    @Test
    void checkOutOnSameDayAsAnotherCheckInIsAllowed() {
        Room r = store.findRoom("102"); // booked 2..5
        LocalDate ci = LocalDate.now().plusDays(5);
        assertTrue(hotel.isAvailable(r, ci, ci.plusDays(2), null));
    }

    @Test
    void searchRoomsFiltersByType() {
        LocalDate ci = LocalDate.now().plusDays(30);
        LocalDate co = ci.plusDays(1);
        List<Room> deluxe = hotel.searchRooms(RoomType.DELUXE, ci, co, null, null);
        assertFalse(deluxe.isEmpty());
        assertTrue(deluxe.stream().allMatch(r -> r.getType() == RoomType.DELUXE));
    }

    // ================= BOOKING =================

    @Test
    void createBookingRejectsPastCheckIn() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> hotel.createBooking(g, r, ci, ci.plusDays(2), 0));
    }

    @Test
    void createBookingRejectsCheckOutBeforeCheckIn() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(5);
        assertThrows(IllegalArgumentException.class,
                () -> hotel.createBooking(g, r, ci, ci.minusDays(1), 0));
    }

    @Test
    void createBookingRejectsDiscountAboveFiftyPercent() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(5);
        assertThrows(IllegalArgumentException.class,
                () -> hotel.createBooking(g, r, ci, ci.plusDays(2), 60));
    }

    @Test
    void createBookingFailsWhenRoomAlreadyBooked() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("102"); // occupied 2..5
        LocalDate ci = LocalDate.now().plusDays(3);
        assertThrows(IllegalStateException.class,
                () -> hotel.createBooking(g, r, ci, ci.plusDays(2), 0));
    }

    @Test
    void createBookingSucceedsForAvailableWindow() {
        Guest g = store.findGuest("G1005");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(40);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(2), 5);
        assertNotNull(b.getId());
        assertEquals(BookingStatus.RESERVED, b.getStatus());
        assertTrue(store.getBookings().contains(b));
    }

    // ================= CHECK-IN / CHECK-OUT =================

    @Test
    void checkInTransitionsReservedToCheckedInAndOccupiesRoom() {
        Guest g = store.findGuest("G1002");
        Room r = store.findRoom("302");
        LocalDate ci = LocalDate.now();
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(2), 0);

        hotel.checkIn(b.getId());
        assertEquals(BookingStatus.CHECKED_IN, b.getStatus());
        assertEquals(RoomStatus.OCCUPIED, r.getStatus());
        assertNotNull(b.getActualCheckIn());
    }

    @Test
    void checkOutRequiresCheckedInStatus() {
        Guest g = store.findGuest("G1002");
        Room r = store.findRoom("302");
        LocalDate ci = LocalDate.now().plusDays(120); // different dates than the check-in test (shared singleton store)
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(2), 0);

        assertThrows(IllegalStateException.class, () -> hotel.checkOut(b.getId()));
    }

    @Test
    void checkOutMovesRoomToCleaning() {
        Guest g = store.findGuest("G1004");
        Room r = store.findRoom("203");
        LocalDate ci = LocalDate.now();
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);

        hotel.checkIn(b.getId());
        hotel.checkOut(b.getId());
        assertEquals(BookingStatus.CHECKED_OUT, b.getStatus());
        assertEquals(RoomStatus.CLEANING, r.getStatus());
    }

    @Test
    void cancelMakesRoomBookableAgain() {
        Guest g = store.findGuest("G1004");
        Room r = store.findRoom("203");
        LocalDate ci = LocalDate.now().plusDays(15);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(2), 0);

        hotel.cancel(b.getId());
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertTrue(hotel.isAvailable(r, ci, ci.plusDays(2), null));
    }

    // ================= PAYMENTS =================

    @Test
    void partialPaymentResultsInPartialStatus() {
        Guest g = store.findGuest("G1005");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(50);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(2), 0);

        double total = billing.total(b);
        billing.recordPayment(b, total / 2, PaymentMethod.CARD);
        assertEquals(PaymentStatus.PARTIAL, billing.statusFor(b));
        assertTrue(billing.balance(b) > 0);
    }

    @Test
    void fullPaymentResultsInPaidStatusAndZeroBalance() {
        Guest g = store.findGuest("G1005");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(60);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);

        billing.recordPayment(b, billing.total(b), PaymentMethod.UPI);
        assertEquals(PaymentStatus.PAID, billing.statusFor(b));
        assertEquals(0.0, billing.balance(b), 0.001);
    }

    @Test
    void paymentCannotExceedBalance() {
        Guest g = store.findGuest("G1005");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(70);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);

        assertThrows(IllegalArgumentException.class,
                () -> billing.recordPayment(b, billing.total(b) + 100, PaymentMethod.CASH));
    }

    @Test
    void paymentMustBePositive() {
        Guest g = store.findGuest("G1005");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(80);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);

        assertThrows(IllegalArgumentException.class,
                () -> billing.recordPayment(b, -50, PaymentMethod.CASH));
    }

    // ================= SERVICES =================

    @Test
    void addingSameServiceAccumulatesQuantity() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(90);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);

        HotelService spa = new HotelService("SPA", "Spa", 120.0, false);
        hotel.addService(b, spa, 1);
        hotel.addService(b, spa, 2);
        assertEquals(1, b.getServices().size());
        assertEquals(3, b.getServices().get(0).getQuantity());
    }

    @Test
    void cannotAddServiceToCheckedOutBooking() {
        Guest g = store.findGuest("G1001");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(100);
        Booking b = hotel.createBooking(g, r, ci, ci.plusDays(1), 0);
        hotel.checkIn(b.getId());
        hotel.checkOut(b.getId());

        assertThrows(IllegalStateException.class,
                () -> hotel.addService(b, new HotelService("SPA", "Spa", 120.0, false), 1));
    }

    // ================= AUTH =================

    @Test
    void loginSucceedsWithSeededCredentials() {
        Auth auth = new Auth();
        assertTrue(auth.login("admin", "admin123"));
        assertNotNull(auth.current());
        assertEquals("Alexander Vance", auth.current().getFullName());
    }

    @Test
    void loginFailsWithWrongPassword() {
        Auth auth = new Auth();
        assertFalse(auth.login("admin", "wrong"));
        assertNull(auth.current());
    }

    @Test
    void logoutClearsCurrentUser() {
        Auth auth = new Auth();
        auth.login("admin", "admin123");
        auth.logout();
        assertNull(auth.current());
    }

    // ================= ID GENERATION =================

    @Test
    void generatedIdsNeverCollideWithSeededData() {
        String bookingId = IdGen.booking();
        String guestId = IdGen.guest();
        String txnId = IdGen.txn();
        assertNull(store.findBooking(bookingId));
        assertNull(store.findGuest(guestId));
        assertFalse(store.getPayments().stream().anyMatch(p -> p.getTransactionId().equals(txnId)));
    }

    @Test
    void creatingBookingsDoesNotOverwriteSeededBookings() {
        Booking seeded = store.findBooking("BK5004");
        Guest g = store.findGuest("G1003");
        Room r = store.findRoom("103");
        LocalDate ci = LocalDate.now().plusDays(130);
        hotel.createBooking(g, r, ci, ci.plusDays(1), 0);
        assertTrue(store.findBooking("BK5004") == seeded);
    }
}
