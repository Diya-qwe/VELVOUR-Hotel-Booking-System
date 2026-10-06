package com.velvour;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** All domain model classes and enums grouped for compactness. */
public final class Models {
    private Models() {}

    // ================= ENUMS =================
    public enum RoomType {
        STANDARD(120.0, "Standard"), DELUXE(220.0, "Deluxe"),
        EXECUTIVE(350.0, "Executive"), SUITE(550.0, "Suite"),
        PRESIDENTIAL(1200.0, "Presidential");
        private final double basePrice; private final String label;
        RoomType(double p, String l) { basePrice = p; label = l; }
        public double getBasePrice() { return basePrice; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum RoomStatus {
        AVAILABLE("Available"), OCCUPIED("Occupied"),
        MAINTENANCE("Maintenance"), CLEANING("Cleaning");
        private final String label;
        RoomStatus(String l) { label = l; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum BookingStatus {
        RESERVED("Reserved"), CHECKED_IN("Checked In"),
        CHECKED_OUT("Checked Out"), CANCELLED("Cancelled");
        private final String label;
        BookingStatus(String l) { label = l; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum PaymentMethod {
        CASH("Cash"), CARD("Card"), UPI("UPI"), NET_BANKING("Net Banking");
        private final String label;
        PaymentMethod(String l) { label = l; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum PaymentStatus {
        PENDING("Pending"), PAID("Paid"), PARTIAL("Partial"),
        REFUNDED("Refunded"), FAILED("Failed");
        private final String label;
        PaymentStatus(String l) { label = l; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    // ================= MODELS =================
    public static class User {
        private final String username, password, fullName, role;
        public User(String u, String p, String f, String r) { username = u; password = p; fullName = f; role = r; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
    }

    public static class Guest {
        private final String id;
        private String fullName, email, phone, address, idProof, nationality;
        public Guest(String id, String n, String e, String p, String a, String ip, String nat) {
            this.id = id; fullName = n; email = e; phone = p; address = a; idProof = ip; nationality = nat;
        }
        public String getId() { return id; }
        public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
        public String getEmail() { return email; } public void setEmail(String v) { email = v; }
        public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
        public String getAddress() { return address; } public void setAddress(String v) { address = v; }
        public String getIdProof() { return idProof; } public void setIdProof(String v) { idProof = v; }
        public String getNationality() { return nationality; } public void setNationality(String v) { nationality = v; }
        @Override public String toString() { return fullName; }
    }

    public static class Room {
        private final String number; private final RoomType type;
        private RoomStatus status; private final int floor, capacity;
        public Room(String n, RoomType t, int f, int c, RoomStatus s) {
            number = n; type = t; floor = f; capacity = c; status = s;
        }
        public String getNumber() { return number; }
        public RoomType getType() { return type; }
        public RoomStatus getStatus() { return status; } public void setStatus(RoomStatus s) { status = s; }
        public int getFloor() { return floor; }
        public int getCapacity() { return capacity; }
        public double getBasePrice() { return type.getBasePrice(); }
    }

    public static class HotelService {
        private final String code, name; private final double price; private final boolean perNight;
        public HotelService(String c, String n, double p, boolean pn) { code = c; name = n; price = p; perNight = pn; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public boolean isPerNight() { return perNight; }
        @Override public String toString() { return name + " (" + String.format("%.2f", price) + ")"; }
    }

    public static class BookingService {
        private final HotelService service; private int quantity;
        public BookingService(HotelService s, int q) { service = s; quantity = q; }
        public HotelService getService() { return service; }
        public int getQuantity() { return quantity; } public void setQuantity(int q) { quantity = q; }
        public double getSubtotal() { return service.getPrice() * quantity; }
    }

    public static class Booking {
        private final String id; private final Guest guest; private final Room room;
        private final LocalDate checkIn, checkOut; private BookingStatus status;
        private final List<BookingService> services = new ArrayList<>();
        private double discountPercent; private final LocalDateTime createdAt;
        private LocalDateTime actualCheckIn, actualCheckOut;

        public Booking(String id, Guest g, Room r, LocalDate ci, LocalDate co, BookingStatus s, LocalDateTime ca) {
            this.id = id; guest = g; room = r; checkIn = ci; checkOut = co; status = s; createdAt = ca;
        }
        public int getNights() { return (int) (checkOut.toEpochDay() - checkIn.toEpochDay()); }
        public String getId() { return id; }
        public Guest getGuest() { return guest; }
        public Room getRoom() { return room; }
        public LocalDate getCheckIn() { return checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public BookingStatus getStatus() { return status; } public void setStatus(BookingStatus s) { status = s; }
        public List<BookingService> getServices() { return services; }
        public double getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(double d) { discountPercent = d; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getActualCheckIn() { return actualCheckIn; } public void setActualCheckIn(LocalDateTime t) { actualCheckIn = t; }
        public LocalDateTime getActualCheckOut() { return actualCheckOut; } public void setActualCheckOut(LocalDateTime t) { actualCheckOut = t; }
        public void addService(HotelService s, int q) {
            for (BookingService bs : services) {
                if (bs.getService().getCode().equals(s.getCode())) { bs.setQuantity(bs.getQuantity() + q); return; }
            }
            services.add(new BookingService(s, q));
        }
    }

    public static class Payment {
        private final String transactionId, bookingId; private final double amount;
        private final PaymentMethod method; private PaymentStatus status; private final LocalDateTime timestamp;
        public Payment(String t, String b, double a, PaymentMethod m, PaymentStatus s, LocalDateTime ts) {
            transactionId = t; bookingId = b; amount = a; method = m; status = s; timestamp = ts;
        }
        public String getTransactionId() { return transactionId; }
        public String getBookingId() { return bookingId; }
        public double getAmount() { return amount; }
        public PaymentMethod getMethod() { return method; }
        public PaymentStatus getStatus() { return status; } public void setStatus(PaymentStatus s) { status = s; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}