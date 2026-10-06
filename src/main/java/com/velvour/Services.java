package com.velvour;

import com.velvour.Models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** DataStore, ID generation, authentication, hotel operations, billing and invoice logic. */
public final class Services {
    private Services() {}

    // ============ ID GENERATOR ============
    public static final class IdGen {
        private static final AtomicInteger g = new AtomicInteger(1006);
        private static final AtomicInteger b = new AtomicInteger(5005);
        private static final AtomicInteger t = new AtomicInteger(90003);
        public static String guest() { return "G" + g.incrementAndGet(); }
        public static String booking() { return "BK" + b.incrementAndGet(); }
        public static String txn() { return "TXN" + t.incrementAndGet(); }
    }

    // ============ DATA STORE ============
    public static final class DataStore {
        private static DataStore instance;
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Guest> guests = new ConcurrentHashMap<>();
        private final Map<String, Room> rooms = new ConcurrentHashMap<>();
        private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
        private final Map<String, Payment> payments = new ConcurrentHashMap<>();
        private final List<HotelService> services = new ArrayList<>();

        private DataStore() { seed(); }

        public static synchronized DataStore get() {
            if (instance == null) instance = new DataStore();
            return instance;
        }

        private void seed() {
            users.put("admin", new User("admin", "admin123", "Alexander Vance", "Administrator"));
            users.put("manager", new User("manager", "manager123", "Priya Raman", "Manager"));
            users.put("reception", new User("reception", "reception123", "Daniel Cole", "Receptionist"));

            // Customer demo account
            users.put("customer", new User("customer", "customer123", "Aarav Kapoor", "Customer"));

            addRoom("101", RoomType.STANDARD, 1, 2);
            addRoom("102", RoomType.STANDARD, 1, 2);
            addRoom("103", RoomType.STANDARD, 1, 3);
            addRoom("201", RoomType.DELUXE, 2, 2);
            addRoom("202", RoomType.DELUXE, 2, 3);
            addRoom("203", RoomType.DELUXE, 2, 2);
            addRoom("301", RoomType.EXECUTIVE, 3, 2);
            addRoom("302", RoomType.EXECUTIVE, 3, 3);
            addRoom("401", RoomType.SUITE, 4, 4);
            addRoom("402", RoomType.SUITE, 4, 4);
            addRoom("501", RoomType.PRESIDENTIAL, 5, 6);

            services.add(new HotelService("BRK", "Breakfast Buffet", 25.0, true));
            services.add(new HotelService("SPA", "Spa Treatment", 120.0, false));
            services.add(new HotelService("LND", "Laundry Service", 30.0, false));
            services.add(new HotelService("APT", "Airport Pickup", 60.0, false));
            services.add(new HotelService("DRP", "Airport Drop", 55.0, false));
            services.add(new HotelService("MIN", "Minibar Package", 45.0, true));
            services.add(new HotelService("GYM", "Gym Access", 20.0, true));
            services.add(new HotelService("CAR", "Chauffeur Service", 150.0, false));
            services.add(new HotelService("DIN", "Fine Dining Experience", 180.0, false));

            Guest g1 = new Guest("G1001", "Marcus Whitfield", "marcus.w@exmail.com", "+1-202-555-0142", "44 Kensington Rd, London", "P-9811223", "British");
            Guest g2 = new Guest("G1002", "Anjali Mehta", "anjali.mehta@exmail.com", "+91-98200-11223", "Bandra West, Mumbai", "A-4412345", "Indian");
            Guest g3 = new Guest("G1003", "Hiroshi Tanaka", "h.tanaka@exmail.com", "+81-90-1234-5678", "Shibuya, Tokyo", "JP-559012", "Japanese");
            Guest g4 = new Guest("G1004", "Sofia Alvarez", "sofia.a@exmail.com", "+34-612-334-556", "Gran Via 24, Madrid", "ES-778123", "Spanish");
            Guest g5 = new Guest("G1005", "Ethan Brooks", "ethan.b@exmail.com", "+1-415-555-7788", "Market St, San Francisco", "US-221190", "American");

            // Customer demo profile linked to username: customer
            Guest customerGuest = new Guest(
                    "G1006",
                    "Aarav Kapoor",
                    "customer@velvour.com",
                    "+91-98765-43210",
                    "Bangalore, India",
                    "IND-CUST-1006",
                    "Indian"
            );

            for (Guest gg : List.of(g1, g2, g3, g4, g5, customerGuest)) {
                guests.put(gg.getId(), gg);
            }

            LocalDate today = LocalDate.now();

            Booking b1 = new Booking("BK5001", g1, rooms.get("201"), today.minusDays(2), today.plusDays(1),
                    BookingStatus.CHECKED_IN, LocalDateTime.now().minusDays(2));
            b1.addService(services.get(0), 3);
            b1.addService(services.get(1), 1);
            b1.setActualCheckIn(LocalDateTime.now().minusDays(2).withHour(14).withMinute(0));
            bookings.put(b1.getId(), b1);
            rooms.get("201").setStatus(RoomStatus.OCCUPIED);

            Booking b2 = new Booking("BK5002", g2, rooms.get("301"), today, today.plusDays(3),
                    BookingStatus.RESERVED, LocalDateTime.now().minusHours(5));
            b2.addService(services.get(3), 1);
            b2.setDiscountPercent(5.0);
            bookings.put(b2.getId(), b2);

            Booking b3 = new Booking("BK5003", g3, rooms.get("401"), today.minusDays(7), today.minusDays(4),
                    BookingStatus.CHECKED_OUT, LocalDateTime.now().minusDays(8));
            b3.addService(services.get(8), 2);
            b3.setActualCheckIn(LocalDateTime.now().minusDays(7).withHour(15).withMinute(30));
            b3.setActualCheckOut(LocalDateTime.now().minusDays(4).withHour(11).withMinute(0));
            bookings.put(b3.getId(), b3);

            Booking b4 = new Booking("BK5004", g4, rooms.get("102"), today.plusDays(2), today.plusDays(5),
                    BookingStatus.RESERVED, LocalDateTime.now().minusHours(2));
            bookings.put(b4.getId(), b4);

            Booking b5 = new Booking("BK5005", g5, rooms.get("501"), today.minusDays(1), today.plusDays(2),
                    BookingStatus.CHECKED_IN, LocalDateTime.now().minusDays(1));
            b5.addService(services.get(7), 2);
            b5.addService(services.get(5), 3);
            b5.setActualCheckIn(LocalDateTime.now().minusDays(1).withHour(13).withMinute(0));
            bookings.put(b5.getId(), b5);
            rooms.get("501").setStatus(RoomStatus.OCCUPIED);

            payments.put("TXN90001", new Payment("TXN90001", "BK5003", 2000.0, PaymentMethod.CARD, PaymentStatus.PAID, LocalDateTime.now().minusDays(4)));
            payments.put("TXN90002", new Payment("TXN90002", "BK5001", 500.0, PaymentMethod.UPI, PaymentStatus.PARTIAL, LocalDateTime.now().minusDays(2)));
            payments.put("TXN90003", new Payment("TXN90003", "BK5005", 4000.0, PaymentMethod.NET_BANKING, PaymentStatus.PAID, LocalDateTime.now().minusDays(1)));
        }

        private void addRoom(String number, RoomType type, int floor, int capacity) {
            rooms.put(number, new Room(number, type, floor, capacity, RoomStatus.AVAILABLE));
        }

        public Collection<User> getUsers() { return users.values(); }
        public User findUser(String u) { return users.get(u); }

        public Collection<Guest> getGuests() { return guests.values(); }
        public Guest findGuest(String id) { return guests.get(id); }
        public void addGuest(Guest g) { guests.put(g.getId(), g); }
        public void removeGuest(String id) { guests.remove(id); }

        public Collection<Room> getRooms() { return rooms.values(); }
        public Room findRoom(String n) { return rooms.get(n); }

        public Collection<Booking> getBookings() { return bookings.values(); }
        public Booking findBooking(String id) { return bookings.get(id); }
        public void addBooking(Booking b) { bookings.put(b.getId(), b); }

        public Collection<Payment> getPayments() { return payments.values(); }
        public void addPayment(Payment p) { payments.put(p.getTransactionId(), p); }

        public List<HotelService> getServices() { return services; }

        public List<Payment> paymentsForBooking(String bookingId) {
            return payments.values().stream().filter(p -> p.getBookingId().equals(bookingId)).collect(Collectors.toList());
        }
    }

    // ============ AUTH ============
    public static final class Auth {
        private User current;
        public boolean login(String u, String p) {
            if (u == null || p == null) return false;
            User found = DataStore.get().findUser(u.trim().toLowerCase());
            if (found != null && found.getPassword().equals(p)) { current = found; return true; }
            return false;
        }
        public void logout() { current = null; }
        public User current() { return current; }
    }

    // ============ HOTEL SERVICE ============
    public static final class Hotel {
        private final DataStore store;
        public Hotel() { store = DataStore.get(); }

        public List<Room> searchRooms(RoomType type, LocalDate ci, LocalDate co, Integer minCap, Double maxPrice) {
            return store.getRooms().stream()
                    .filter(r -> r.getStatus() != RoomStatus.MAINTENANCE)
                    .filter(r -> type == null || r.getType() == type)
                    .filter(r -> minCap == null || r.getCapacity() >= minCap)
                    .filter(r -> maxPrice == null || r.getBasePrice() <= maxPrice)
                    .filter(r -> isAvailable(r, ci, co, null))
                    .sorted(Comparator.comparing(Room::getNumber))
                    .collect(Collectors.toList());
        }

        public boolean isAvailable(Room room, LocalDate ci, LocalDate co, String ignoreId) {
            if (room == null || ci == null || co == null) return false;
            if (!co.isAfter(ci)) return false;
            return store.getBookings().stream()
                    .filter(b -> !b.getId().equals(ignoreId))
                    .filter(b -> b.getRoom().getNumber().equals(room.getNumber()))
                    .filter(b -> b.getStatus() == BookingStatus.RESERVED || b.getStatus() == BookingStatus.CHECKED_IN)
                    .noneMatch(b -> ci.isBefore(b.getCheckOut()) && b.getCheckIn().isBefore(co));
        }

        public Booking createBooking(Guest guest, Room room, LocalDate ci, LocalDate co, double discount) {
            if (guest == null) throw new IllegalArgumentException("Guest is required.");
            if (room == null) throw new IllegalArgumentException("Room is required.");
            if (ci == null || co == null) throw new IllegalArgumentException("Dates required.");
            if (ci.isBefore(LocalDate.now())) throw new IllegalArgumentException("Check-in cannot be in the past.");
            if (!co.isAfter(ci)) throw new IllegalArgumentException("Check-out must be after check-in.");
            if (discount < 0 || discount > 50) throw new IllegalArgumentException("Discount must be 0-50%.");
            if (!isAvailable(room, ci, co, null))
                throw new IllegalStateException("Room " + room.getNumber() + " is not available for these dates.");
            Booking b = new Booking(IdGen.booking(), guest, room, ci, co, BookingStatus.RESERVED, LocalDateTime.now());
            b.setDiscountPercent(discount);
            store.addBooking(b);
            return b;
        }

        public void addService(Booking b, HotelService s, int qty) {
            if (b.getStatus() == BookingStatus.CHECKED_OUT)
                throw new IllegalStateException("Cannot modify a checked-out booking.");
            if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive.");
            b.addService(s, qty);
        }

        public void checkIn(String bookingId) {
            Booking b = requireBooking(bookingId);
            if (b.getStatus() != BookingStatus.RESERVED)
                throw new IllegalStateException("Only reserved bookings can be checked in.");
            b.setStatus(BookingStatus.CHECKED_IN);
            b.setActualCheckIn(LocalDateTime.now());
            b.getRoom().setStatus(RoomStatus.OCCUPIED);
        }

        public void checkOut(String bookingId) {
            Booking b = requireBooking(bookingId);
            if (b.getStatus() != BookingStatus.CHECKED_IN)
                throw new IllegalStateException("Only checked-in bookings can be checked out.");
            b.setStatus(BookingStatus.CHECKED_OUT);
            b.setActualCheckOut(LocalDateTime.now());
            b.getRoom().setStatus(RoomStatus.CLEANING);
        }

        public void cancel(String bookingId) {
            Booking b = requireBooking(bookingId);
            if (b.getStatus() == BookingStatus.CHECKED_OUT)
                throw new IllegalStateException("Cannot cancel a completed booking.");
            b.setStatus(BookingStatus.CANCELLED);
            if (b.getRoom().getStatus() == RoomStatus.OCCUPIED)
                b.getRoom().setStatus(RoomStatus.CLEANING);
        }

        public void markAvailable(String roomNumber) {
            Room r = store.findRoom(roomNumber);
            if (r == null) throw new IllegalArgumentException("Room not found.");
            r.setStatus(RoomStatus.AVAILABLE);
        }

        public void setMaintenance(String roomNumber, boolean on) {
            Room r = store.findRoom(roomNumber);
            if (r == null) throw new IllegalArgumentException("Room not found.");
            if (r.getStatus() == RoomStatus.OCCUPIED) throw new IllegalStateException("Room is occupied.");
            r.setStatus(on ? RoomStatus.MAINTENANCE : RoomStatus.AVAILABLE);
        }

        private Booking requireBooking(String id) {
            Booking b = store.findBooking(id);
            if (b == null) throw new IllegalArgumentException("Booking not found.");
            return b;
        }

        public DataStore store() { return store; }
    }

    // ============ BILLING ============
    public static final class Billing {
        public static final double TAX_RATE = 0.12;

        public double roomCharge(Booking b) {
            int n = b.getNights();
            return n <= 0 ? 0 : n * b.getRoom().getBasePrice();
        }

        public double servicesCharge(Booking b) {
            int nights = Math.max(b.getNights(), 1);
            double total = 0;
            for (BookingService bs : b.getServices()) {
                int mult = bs.getService().isPerNight() ? nights : 1;
                total += bs.getService().getPrice() * bs.getQuantity() * mult;
            }
            return total;
        }

        public double subtotal(Booking b) { return roomCharge(b) + servicesCharge(b); }
        public double discount(Booking b) { return subtotal(b) * (b.getDiscountPercent() / 100.0); }
        public double taxableAmount(Booking b) { return subtotal(b) - discount(b); }
        public double tax(Booking b) { return taxableAmount(b) * TAX_RATE; }
        public double total(Booking b) { return taxableAmount(b) + tax(b); }

        public double paidAmount(String bookingId) {
            return DataStore.get().paymentsForBooking(bookingId).stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                    .mapToDouble(Payment::getAmount).sum();
        }

        public double balance(Booking b) {
            double bal = total(b) - paidAmount(b.getId());
            return Math.max(bal, 0);
        }

        public PaymentStatus statusFor(Booking b) {
            double paid = paidAmount(b.getId()), total = total(b);
            if (paid <= 0.001) return PaymentStatus.PENDING;
            if (paid + 0.001 >= total) return PaymentStatus.PAID;
            return PaymentStatus.PARTIAL;
        }

        public Payment recordPayment(Booking b, double amount, PaymentMethod method) {
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
            double bal = balance(b);
            if (amount > bal + 0.001) throw new IllegalArgumentException("Amount exceeds balance: " + String.format("%.2f", bal));
            PaymentStatus status = (amount + 0.001 >= bal) ? PaymentStatus.PAID : PaymentStatus.PARTIAL;
            Payment p = new Payment(IdGen.txn(), b.getId(), amount, method, status, LocalDateTime.now());
            DataStore.get().addPayment(p);
            return p;
        }
    }

    // ============ INVOICE (PDF) ============
    public static final class Invoice {
        private final Billing billing = new Billing();

        public java.io.File generate(Booking b, java.io.File target) throws java.io.IOException {
            try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                doc.addPage(page);
                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                             new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                    float y = 760; final float x = 60;
                    cs.setNonStrokingColor(6, 78, 59);
                    cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 26);
                    cs.newLineAtOffset(x, y); cs.showText("VELVOUR"); cs.endText();
                    cs.setNonStrokingColor(0, 0, 0);
                    y -= 20;
                    cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(x, y); cs.showText("Luxury Hotel & Residences"); cs.endText();

                    y -= 30;
                    cs.setNonStrokingColor(180, 140, 40);
                    cs.beginText(); cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
                    cs.newLineAtOffset(x, y); cs.showText("INVOICE"); cs.endText();
                    cs.setNonStrokingColor(0, 0, 0);

                    y -= 30;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    String[][] head = {
                            {"Booking ID:", b.getId()},
                            {"Guest:", b.getGuest().getFullName()},
                            {"Email:", b.getGuest().getEmail()},
                            {"Room:", b.getRoom().getNumber() + " - " + b.getRoom().getType().getLabel()},
                            {"Check-in:", b.getCheckIn().toString()},
                            {"Check-out:", b.getCheckOut().toString()},
                            {"Nights:", String.valueOf(b.getNights())},
                            {"Status:", b.getStatus().getLabel()}
                    };
                    for (String[] row : head) {
                        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText(row[0]); cs.endText();
                        cs.beginText(); cs.newLineAtOffset(x + 110, y); cs.showText(row[1]); cs.endText();
                        y -= 14;
                    }

                    y -= 10;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                    cs.beginText(); cs.newLineAtOffset(x, y); cs.showText("Description"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(x + 350, y); cs.showText("Amount"); cs.endText();
                    y -= 4;
                    cs.moveTo(x, y); cs.lineTo(x + 480, y); cs.stroke();
                    y -= 14;

                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    y = line(cs, x, y, "Room charge (" + b.getNights() + " nights)", billing.roomCharge(b));
                    for (BookingService bs : b.getServices()) {
                        int mult = bs.getService().isPerNight() ? b.getNights() : 1;
                        String label = bs.getService().getName() + " x" + bs.getQuantity() +
                                (bs.getService().isPerNight() ? " (" + mult + " nights)" : "");
                        y = line(cs, x, y, label, bs.getService().getPrice() * bs.getQuantity() * mult);
                    }
                    y -= 4;
                    cs.moveTo(x, y); cs.lineTo(x + 480, y); cs.stroke();
                    y -= 16;
                    y = line(cs, x, y, "Subtotal", billing.subtotal(b));
                    y = line(cs, x, y, "Discount (" + String.format("%.1f", b.getDiscountPercent()) + "%)", -billing.discount(b));
                    y = line(cs, x, y, "Tax (12%)", billing.tax(b));
                    y -= 4; cs.moveTo(x, y); cs.lineTo(x + 480, y); cs.stroke(); y -= 16;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                    y = line(cs, x, y, "TOTAL DUE", billing.total(b));
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                    y = line(cs, x, y, "Amount Paid", billing.paidAmount(b.getId()));
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                    y = line(cs, x, y, "Balance", billing.balance(b));

                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 9);
                    cs.beginText(); cs.newLineAtOffset(x, 60);
                    cs.showText("Thank you for choosing VELVOUR. We look forward to welcoming you again.");
                    cs.endText();
                }
                doc.save(target);
            }
            return target;
        }

        private float line(org.apache.pdfbox.pdmodel.PDPageContentStream cs, float x, float y, String label, double amount) throws java.io.IOException {
            cs.beginText(); cs.newLineAtOffset(x, y); cs.showText(label); cs.endText();
            cs.beginText(); cs.newLineAtOffset(x + 350, y); cs.showText(String.format("%.2f", amount)); cs.endText();
            return y - 14;
        }
    }
}