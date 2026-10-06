package com.velvour;

import com.velvour.Models.*;
import com.velvour.Services.*;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.util.StringConverter;

public class VelvourApp extends Application {

    // ==== THEME ====
    static final String BG_DARK = "#0a0f0d";
    static final String BG_PANEL = "#0f1a16";
    static final String BG_CARD = "#132520";
    static final String EMERALD = "#0d5e46";
    static final String EMERALD_LIGHT = "#167a5c";
    static final String GOLD = "#c9a227";
    static final String TEXT = "#e8f0ec";
    static final String TEXT_MUTED = "#8fa89e";

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Auth auth = new Auth();
    private final Hotel hotel = new Hotel();
    private final Billing billing = new Billing();
    private final Invoice invoice = new Invoice();

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("VELVOUR — Hotel Management");
        showLogin();
        stage.show();
    }

    // ==================== LOGIN ====================
    private void showLogin() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        Label brand = new Label("VELVOUR");
        brand.setFont(Font.font("Georgia", FontWeight.BOLD, 42));
        brand.setTextFill(Color.web(GOLD));

        Label sub = new Label("LUXURY HOTEL & RESIDENCES");
        sub.setFont(Font.font("System", 11));
        sub.setTextFill(Color.web(TEXT_MUTED));

        VBox card = new VBox(14);
        card.setPadding(new Insets(35));
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 8; -fx-border-color: " +
                EMERALD + "; -fx-border-radius: 8; -fx-border-width: 1;");

        Label title = new Label("VELVOUR Sign In");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(TEXT));

        TextField user = styledField("Username");
        PasswordField pass = new PasswordField();
        styleInput(pass, "Password");

        Label err = new Label();
        err.setTextFill(Color.web("#e05c5c"));
        err.setFont(Font.font("System", 11));

        Button login = primaryButton("SIGN IN");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setOnAction(e -> {
            if (auth.login(user.getText(), pass.getText())) {
                if ("Customer".equalsIgnoreCase(auth.current().getRole())) {
                    showCustomerMain();
                } else {
                    showMain();
                }
            } else {
                err.setText("Invalid username or password.");
            }
        });
        pass.setOnAction(e -> login.fire());

        Label hint = new Label("Staff: admin / admin123    |    Customer: customer / customer123");
        hint.setFont(Font.font("System", 10));
        hint.setTextFill(Color.web(TEXT_MUTED));

        card.getChildren().addAll(title, user, pass, err, login, hint);
        root.getChildren().addAll(brand, sub, card);

        stage.setScene(new Scene(root, 900, 620));
    }

    private void showMain() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Top bar
        HBox top = new HBox(15);
        top.setPadding(new Insets(14, 22, 14, 22));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-border-color: transparent transparent " +
                EMERALD + " transparent; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("VELVOUR");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setTextFill(Color.web(GOLD));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label who = new Label(auth.current().getFullName() + "  •  " + auth.current().getRole());
        who.setTextFill(Color.web(TEXT_MUTED));
        who.setFont(Font.font("System", 12));

        Button logout = new Button("Logout");
        logout.setStyle(ghostButtonStyle());
        logout.setOnAction(e -> { auth.logout(); showLogin(); });

        top.getChildren().addAll(logo, sp, who, logout);

        // Sidebar
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: " + BG_PANEL + ";");

        StackPane content = new StackPane();
        content.setPadding(new Insets(22));
        content.setStyle("-fx-background-color: " + BG_DARK + ";");

        String[][] nav = {
                {"Dashboard", "dash"},
                {"Rooms", "rooms"},
                {"Guests", "guests"},
                {"Bookings", "bookings"},
                {"Check-In / Out", "checkin"},
                {"Services", "services"},
                {"Payments", "payments"},
                {"Reports", "reports"}
        };

        ToggleGroup group = new ToggleGroup();
        for (String[] n : nav) {
            ToggleButton tb = new ToggleButton(n[0]);
            tb.setToggleGroup(group);
            tb.setMaxWidth(Double.MAX_VALUE);
            tb.setAlignment(Pos.CENTER_LEFT);
            tb.setStyle(navButtonStyle(false));
            tb.selectedProperty().addListener((o, was, is) -> {
                tb.setStyle(navButtonStyle(is));
                if (is) content.getChildren().setAll(buildView(n[1]));
            });
            tb.setOnAction(ev -> { if (!tb.isSelected()) tb.setSelected(true); });
            if (n[1].equals("dash")) tb.setSelected(true);
            sidebar.getChildren().add(tb);
        }

        root.setTop(top);
        root.setLeft(sidebar);
        root.setCenter(content);
        content.getChildren().setAll(buildView("dash"));

        stage.setScene(new Scene(root, 1240, 760));
    }

    private javafx.scene.Node buildView(String key) {
        switch (key) {
            case "rooms": return roomsView();
            case "guests": return guestsView();
            case "bookings": return bookingsView();
            case "checkin": return checkInOutView();
            case "services": return servicesView();
            case "payments": return paymentsView();
            case "reports": return reportsView();
            default: return dashboardView();
        }
    }


    // ==================== CUSTOMER PORTAL ====================

    private void showCustomerMain() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        HBox top = new HBox(15);
        top.setPadding(new Insets(14, 22, 14, 22));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setStyle("-fx-background-color: " + BG_PANEL +
                "; -fx-border-color: transparent transparent " + EMERALD +
                " transparent; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("VELVOUR");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setTextFill(Color.web(GOLD));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label who = new Label(auth.current().getFullName() + "  •  Customer");
        who.setTextFill(Color.web(TEXT_MUTED));

        Button logout = new Button("Logout");
        logout.setStyle(ghostButtonStyle());
        logout.setOnAction(e -> {
            auth.logout();
            showLogin();
        });

        top.getChildren().addAll(logo, spacer, who, logout);

        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color: " + BG_PANEL + ";");

        StackPane content = new StackPane();
        content.setPadding(new Insets(22));
        content.setStyle("-fx-background-color: " + BG_DARK + ";");

        String[][] nav = {
                {"Dashboard", "customerDashboard"},
                {"Browse Rooms", "customerRooms"},
                {"My Bookings", "customerBookings"},
                {"Hotel Services", "customerServices"}
        };

        ToggleGroup group = new ToggleGroup();

        for (String[] n : nav) {
            ToggleButton tb = new ToggleButton(n[0]);
            tb.setToggleGroup(group);
            tb.setMaxWidth(Double.MAX_VALUE);
            tb.setAlignment(Pos.CENTER_LEFT);
            tb.setStyle(navButtonStyle(false));

            tb.selectedProperty().addListener((obs, oldValue, selected) -> {
                tb.setStyle(navButtonStyle(selected));
                if (selected) {
                    content.getChildren().setAll(customerView(n[1]));
                }
            });

            if (n[1].equals("customerDashboard")) {
                tb.setSelected(true);
            }

            sidebar.getChildren().add(tb);
        }

        root.setTop(top);
        root.setLeft(sidebar);
        root.setCenter(content);

        content.getChildren().setAll(customerDashboardView());

        stage.setScene(new Scene(root, 1240, 760));
    }

    private javafx.scene.Node customerView(String key) {
        switch (key) {
            case "customerRooms":
                return customerRoomsView();
            case "customerBookings":
                return customerBookingsView();
            case "customerServices":
                return customerServicesView();
            default:
                return customerDashboardView();
        }
    }

    private Guest currentCustomerGuest() {
        if (auth.current() == null) return null;

        if ("customer".equalsIgnoreCase(auth.current().getUsername())) {
            return hotel.store().findGuest("G1006");
        }

        return null;
    }

    private List<Booking> customerBookings() {
        Guest guest = currentCustomerGuest();

        if (guest == null) {
            return new java.util.ArrayList<>();
        }

        return hotel.store().getBookings().stream()
                .filter(b -> b.getGuest().getId().equals(guest.getId()))
                .collect(Collectors.toList());
    }

    private javafx.scene.Node customerDashboardView() {
        VBox box = new VBox(20);

        Label title = sectionTitle("Customer Dashboard");

        Guest guest = currentCustomerGuest();

        Label welcome = new Label(
                guest == null
                        ? "Welcome to VELVOUR."
                        : "Welcome back, " + guest.getFullName() + "."
        );
        welcome.setTextFill(Color.web(TEXT_MUTED));
        welcome.setFont(Font.font("System", 15));

        long total = customerBookings().size();

        long active = customerBookings().stream()
                .filter(b -> b.getStatus() == BookingStatus.RESERVED ||
                        b.getStatus() == BookingStatus.CHECKED_IN)
                .count();

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                statCard("MY BOOKINGS",
                        String.valueOf(total),
                        "Total reservations"),

                statCard("ACTIVE STAYS",
                        String.valueOf(active),
                        "Current reservations"),

                statCard("AVAILABLE ROOMS",
                        String.valueOf(
                                hotel.store().getRooms().stream()
                                        .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                                        .count()
                        ),
                        "Ready to reserve")
        );

        Button browse = primaryButton("BROWSE ROOMS");
        browse.setOnAction(e -> showCustomerMain());

        box.getChildren().addAll(title, welcome, cards, browse);

        return box;
    }

    private javafx.scene.Node customerRoomsView() {
        VBox box = new VBox(16);

        Label title = sectionTitle("Browse Rooms");

        DatePicker checkIn = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker checkOut = new DatePicker(LocalDate.now().plusDays(2));
        styleDatePicker(checkIn);
        styleDatePicker(checkOut);

        Spinner<Integer> guests = new Spinner<>(1, 6, 1);
        guests.setEditable(true);

        TableView<Room> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
                col("Room", Room::getNumber),
                col("Type", r -> r.getType().getLabel()),
                col("Capacity", r -> String.valueOf(r.getCapacity())),
                col("Price / Night",
                        r -> "$" + String.format("%,.2f", r.getBasePrice())),
                col("Status", r -> r.getStatus().getLabel())
        );

        Runnable refresh = () -> {
            try {
                if (checkOut.getValue() != null &&
                        checkIn.getValue() != null &&
                        checkOut.getValue().isAfter(checkIn.getValue())) {

                    table.setItems(FXCollections.observableArrayList(
                            hotel.searchRooms(
                                    null,
                                    checkIn.getValue(),
                                    checkOut.getValue(),
                                    guests.getValue(),
                                    null
                            )
                    ));
                }
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Search Failed", ex.getMessage());
            }
        };

        Button search = primaryButton("SEARCH AVAILABLE ROOMS");
        search.setOnAction(e -> refresh.run());

        Button book = primaryButton("BOOK SELECTED ROOM");
        book.setOnAction(e -> {
            Room room = table.getSelectionModel().getSelectedItem();

            if (room == null) {
                alert(Alert.AlertType.WARNING,
                        "Select Room",
                        "Please select a room first.");
                return;
            }

            Guest guest = currentCustomerGuest();

            if (guest == null) {
                alert(Alert.AlertType.ERROR,
                        "Customer Profile",
                        "Customer profile could not be found.");
                return;
            }

            if (checkOut.getValue() == null ||
                    checkIn.getValue() == null ||
                    !checkOut.getValue().isAfter(checkIn.getValue())) {

                alert(Alert.AlertType.WARNING,
                        "Invalid Dates",
                        "Please select a valid check-in and check-out date.");
                return;
            }

            try {
                Booking booking = hotel.createBooking(
                        guest,
                        room,
                        checkIn.getValue(),
                        checkOut.getValue(),
                        0
                );

                alert(Alert.AlertType.INFORMATION,
                        "Booking Confirmed",
                        "Booking " + booking.getId() +
                                " has been created successfully.\n\n" +
                                "Room: " + room.getNumber() + "\n" +
                                "Check-in: " + FMT.format(booking.getCheckIn()) + "\n" +
                                "Check-out: " + FMT.format(booking.getCheckOut()) + "\n" +
                                "Estimated total: $" +
                                String.format("%,.2f", billing.total(booking)));

                customerBookingsView();

            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR,
                        "Booking Failed",
                        ex.getMessage());
            }
        });

        HBox filters = new HBox(
                10,
                label("Check-in"), checkIn,
                label("Check-out"), checkOut,
                label("Guests"), guests,
                search
        );

        HBox actions = new HBox(10, book);

        VBox.setVgrow(table, Priority.ALWAYS);

        box.getChildren().addAll(
                title,
                filters,
                actions,
                table
        );

        refresh.run();

        return box;
    }

    private javafx.scene.Node customerBookingsView() {
        VBox box = new VBox(16);

        Label title = sectionTitle("My Bookings");

        TableView<Booking> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
                col("Booking ID", Booking::getId),
                col("Room",
                        b -> b.getRoom().getNumber()),
                col("Room Type",
                        b -> b.getRoom().getType().getLabel()),
                col("Check-in",
                        b -> FMT.format(b.getCheckIn())),
                col("Check-out",
                        b -> FMT.format(b.getCheckOut())),
                col("Status",
                        b -> b.getStatus().getLabel()),
                col("Total",
                        b -> "$" + String.format("%,.2f", billing.total(b))),
                col("Balance",
                        b -> "$" + String.format("%,.2f", billing.balance(b)))
        );

        table.setItems(
                FXCollections.observableArrayList(customerBookings())
        );

        Button cancel = secondaryButton("CANCEL BOOKING");

        cancel.setOnAction(e -> {
            Booking selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert(Alert.AlertType.WARNING,
                        "Select Booking",
                        "Please select a booking.");
                return;
            }

            if (!confirm(
                    "Cancel booking " + selected.getId() + "?")) {
                return;
            }

            try {
                hotel.cancel(selected.getId());

                table.setItems(
                        FXCollections.observableArrayList(
                                customerBookings()
                        )
                );

                alert(Alert.AlertType.INFORMATION,
                        "Booking Cancelled",
                        "Your booking has been cancelled.");

            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR,
                        "Cancellation Failed",
                        ex.getMessage());
            }
        });

        Button invoiceButton =
                primaryButton("DOWNLOAD INVOICE");

        invoiceButton.setOnAction(e -> {
            Booking selected =
                    table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert(Alert.AlertType.WARNING,
                        "Select Booking",
                        "Please select a booking first.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save VELVOUR Invoice");
            chooser.setInitialFileName(
                    "VELVOUR-" + selected.getId() + ".pdf"
            );
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PDF Files", "*.pdf")
            );

            File file = chooser.showSaveDialog(stage);

            if (file != null) {
                try {
                    invoice.generate(selected, file);

                    alert(Alert.AlertType.INFORMATION,
                            "Invoice Generated",
                            "Invoice saved successfully.");
                } catch (Exception ex) {
                    alert(Alert.AlertType.ERROR,
                            "Invoice Failed",
                            ex.getMessage());
                }
            }
        });

        HBox actions = new HBox(
                10,
                cancel,
                invoiceButton
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        box.getChildren().addAll(
                title,
                actions,
                table
        );

        return box;
    }

    private javafx.scene.Node customerServicesView() {
        VBox box = new VBox(16);

        Label title = sectionTitle("Hotel Services");

        TableView<HotelService> servicesTable =
                new TableView<>();

        servicesTable.setStyle(tableStyle());
        servicesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        servicesTable.getColumns().addAll(
                col("Code", HotelService::getCode),
                col("Service", HotelService::getName),
                col("Price",
                        s -> "$" + String.format(
                                "%,.2f", s.getPrice())),
                col("Billing",
                        s -> s.isPerNight()
                                ? "Per night"
                                : "One-time")
        );

        servicesTable.setItems(
                FXCollections.observableArrayList(
                        hotel.store().getServices()
                )
        );

        ComboBox<Booking> bookingBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                customerBookings().stream()
                                        .filter(b ->
                                                b.getStatus() ==
                                                        BookingStatus.RESERVED ||
                                                        b.getStatus() ==
                                                                BookingStatus.CHECKED_IN)
                                        .collect(Collectors.toList())
                        )
                );

        bookingBox.setPromptText("Select your booking");
        bookingBox.setPrefWidth(320);
        bookingBox.setConverter(bookingConverter());
        styleCombo(bookingBox);

        Spinner<Integer> quantity =
                new Spinner<>(1, 20, 1);

        quantity.setEditable(true);

        Button add = primaryButton("ADD SERVICE");

        add.setOnAction(e -> {
            HotelService service =
                    servicesTable.getSelectionModel()
                            .getSelectedItem();

            Booking booking = bookingBox.getValue();

            if (service == null) {
                alert(Alert.AlertType.WARNING,
                        "Select Service",
                        "Please select a service.");
                return;
            }

            if (booking == null) {
                alert(Alert.AlertType.WARNING,
                        "Select Booking",
                        "Please select one of your active bookings.");
                return;
            }

            try {
                quantity.commitValue();

                hotel.addService(
                        booking,
                        service,
                        quantity.getValue()
                );

                alert(Alert.AlertType.INFORMATION,
                        "Service Added",
                        service.getName() +
                                " was added to booking " +
                                booking.getId() + ".");

            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR,
                        "Service Failed",
                        ex.getMessage());
            }
        });

        Label note = new Label(
                "Services added here are included automatically in your booking bill."
        );
        note.setTextFill(Color.web(TEXT_MUTED));

        HBox selection = new HBox(
                10,
                label("Booking"),
                bookingBox,
                label("Quantity"),
                quantity,
                add
        );

        VBox.setVgrow(servicesTable, Priority.ALWAYS);

        box.getChildren().addAll(
                title,
                note,
                selection,
                servicesTable
        );

        return box;
    }

    // ==================== DASHBOARD ====================
    private javafx.scene.Node dashboardView() {
        VBox box = new VBox(20);
        Label h = sectionTitle("Dashboard Overview");

        long totalRooms = hotel.store().getRooms().size();
        long occupied = hotel.store().getRooms().stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long available = hotel.store().getRooms().stream().filter(r -> r.getStatus() == RoomStatus.AVAILABLE).count();
        double occupancyPct = totalRooms == 0 ? 0 : (occupied * 100.0 / totalRooms);

        long activeBookings = hotel.store().getBookings().stream()
                .filter(b -> b.getStatus() == BookingStatus.RESERVED || b.getStatus() == BookingStatus.CHECKED_IN).count();
        long activeGuests = hotel.store().getGuests().size();

        double revenue = hotel.store().getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                .mapToDouble(Payment::getAmount).sum();

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                statCard("OCCUPANCY", String.format("%.0f%%", occupancyPct), occupied + " of " + totalRooms + " rooms"),
                statCard("AVAILABLE", String.valueOf(available), "Ready to book"),
                statCard("ACTIVE BOOKINGS", String.valueOf(activeBookings), "Reserved + checked-in"),
                statCard("REGISTERED GUESTS", String.valueOf(activeGuests), "Total in system"),
                statCard("REVENUE", "$" + String.format("%,.0f", revenue), "Collected payments")
        );

        // Recent bookings
        TableView<Booking> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Booking, String> c1 = col("Booking", b -> b.getId());
        TableColumn<Booking, String> c2 = col("Guest", b -> b.getGuest().getFullName());
        TableColumn<Booking, String> c3 = col("Room", b -> b.getRoom().getNumber() + " " + b.getRoom().getType().getLabel());
        TableColumn<Booking, String> c4 = col("Check-In", b -> FMT.format(b.getCheckIn()));
        TableColumn<Booking, String> c5 = col("Check-Out", b -> FMT.format(b.getCheckOut()));
        TableColumn<Booking, String> c6 = col("Status", b -> b.getStatus().getLabel());
        TableColumn<Booking, String> c7 = col("Total", b -> "$" + String.format("%,.2f", billing.total(b)));
        table.getColumns().addAll(c1, c2, c3, c4, c5, c6, c7);
        table.setItems(FXCollections.observableArrayList(hotel.store().getBookings()));
        VBox.setVgrow(table, Priority.ALWAYS);

        box.getChildren().addAll(h, cards, sectionTitle("Recent Bookings"), table);
        return box;
    }

    // ==================== ROOMS ====================
    private javafx.scene.Node roomsView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Rooms & Availability");

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<RoomType> typeBox = new ComboBox<>(FXCollections.observableArrayList(RoomType.values()));
        typeBox.setPromptText("All Types");
        styleCombo(typeBox);

        TextField capField = styledField("Min Capacity");
        TextField priceField = styledField("Max Price");
        capField.setPrefWidth(130);
        priceField.setPrefWidth(130);

        DatePicker ciPick = new DatePicker(LocalDate.now());
        DatePicker coPick = new DatePicker(LocalDate.now().plusDays(1));
        styleDatePicker(ciPick); styleDatePicker(coPick);

        Button search = primaryButton("SEARCH");
        Button reset = secondaryButton("RESET");

        TableView<Room> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
                col("Room", r -> r.getNumber()),
                col("Type", r -> r.getType().getLabel()),
                col("Floor", r -> String.valueOf(r.getFloor())),
                col("Capacity", r -> String.valueOf(r.getCapacity())),
                col("Price / Night", r -> "$" + String.format("%,.2f", r.getBasePrice())),
                col("Status", r -> r.getStatus().getLabel())
        );
        table.setItems(FXCollections.observableArrayList(hotel.store().getRooms()));
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(hotel.store().getRooms()));

        search.setOnAction(e -> {
            try {
                Integer cap = capField.getText().isBlank() ? null : Integer.parseInt(capField.getText().trim());
                Double max = priceField.getText().isBlank() ? null : Double.parseDouble(priceField.getText().trim());
                List<Room> result = hotel.searchRooms(typeBox.getValue(), ciPick.getValue(), coPick.getValue(), cap, max);
                table.setItems(FXCollections.observableArrayList(result));
            } catch (NumberFormatException ex) {
                alert(Alert.AlertType.ERROR, "Validation", "Capacity and price must be numeric.");
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Search Failed", ex.getMessage());
            }
        });
        reset.setOnAction(e -> {
            typeBox.setValue(null); capField.clear(); priceField.clear();
            refresh.run();
        });

        // Admin actions
        HBox actions = new HBox(10);
        Button maint = secondaryButton("Toggle Maintenance");
        Button ready = secondaryButton("Mark Available");
        maint.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { alert(Alert.AlertType.WARNING, "Select a room", "Please select a room first."); return; }
            try {
                hotel.setMaintenance(r.getNumber(), r.getStatus() != RoomStatus.MAINTENANCE);
                refresh.run();
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
        });
        ready.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { alert(Alert.AlertType.WARNING, "Select a room", "Please select a room first."); return; }
            try { hotel.markAvailable(r.getNumber()); refresh.run(); }
            catch (Exception ex) { alert(Alert.AlertType.ERROR, "Error", ex.getMessage()); }
        });
        actions.getChildren().addAll(maint, ready);

        filters.getChildren().addAll(typeBox, capField, priceField, ciPick, coPick, search, reset);
        box.getChildren().addAll(h, filters, actions, table);
        return box;
    }

    // ==================== GUESTS ====================
    private javafx.scene.Node guestsView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Guest Management");

        TableView<Guest> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
                col("ID", Guest::getId),
                col("Name", Guest::getFullName),
                col("Email", Guest::getEmail),
                col("Phone", Guest::getPhone),
                col("Nationality", Guest::getNationality),
                col("ID Proof", Guest::getIdProof)
        );
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(hotel.store().getGuests()));
        refresh.run();
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button add = primaryButton("ADD GUEST");
        Button edit = secondaryButton("EDIT");
        Button remove = secondaryButton("REMOVE");

        add.setOnAction(e -> guestDialog(null, refresh));
        edit.setOnAction(e -> {
            Guest g = table.getSelectionModel().getSelectedItem();
            if (g == null) { alert(Alert.AlertType.WARNING, "Select", "Select a guest to edit."); return; }
            guestDialog(g, refresh);
        });
        remove.setOnAction(e -> {
            Guest g = table.getSelectionModel().getSelectedItem();
            if (g == null) { alert(Alert.AlertType.WARNING, "Select", "Select a guest to remove."); return; }
            boolean hasActive = hotel.store().getBookings().stream()
                    .anyMatch(b -> b.getGuest().getId().equals(g.getId())
                            && (b.getStatus() == BookingStatus.RESERVED || b.getStatus() == BookingStatus.CHECKED_IN));
            if (hasActive) { alert(Alert.AlertType.ERROR, "Cannot Remove", "Guest has active bookings."); return; }
            hotel.store().removeGuest(g.getId());
            refresh.run();
        });

        actions.getChildren().addAll(add, edit, remove);
        box.getChildren().addAll(h, actions, table);
        return box;
    }

    private void guestDialog(Guest existing, Runnable onSave) {
        Dialog<Guest> d = new Dialog<>();
        d.setTitle(existing == null ? "Add Guest" : "Edit Guest");
        d.getDialogPane().setStyle("-fx-background-color: " + BG_CARD + ";");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField name = styledField("Full Name");
        TextField email = styledField("Email");
        TextField phone = styledField("Phone");
        TextField addr = styledField("Address");
        TextField proof = styledField("ID Proof Number");
        TextField nat = styledField("Nationality");

        if (existing != null) {
            name.setText(existing.getFullName()); email.setText(existing.getEmail());
            phone.setText(existing.getPhone()); addr.setText(existing.getAddress());
            proof.setText(existing.getIdProof()); nat.setText(existing.getNationality());
        }

        grid.addRow(0, label("Name"), name);
        grid.addRow(1, label("Email"), email);
        grid.addRow(2, label("Phone"), phone);
        grid.addRow(3, label("Address"), addr);
        grid.addRow(4, label("ID Proof"), proof);
        grid.addRow(5, label("Nationality"), nat);

        d.getDialogPane().setContent(grid);
        d.setResultConverter(bt -> {
            if (bt != saveType) return null;
            try {
                if (name.getText().isBlank()) throw new IllegalArgumentException("Name required.");
                if (!email.getText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
                    throw new IllegalArgumentException("Valid email required.");
                if (phone.getText().isBlank()) throw new IllegalArgumentException("Phone required.");
                if (existing == null) {
                    Guest g = new Guest(IdGen.guest(), name.getText().trim(), email.getText().trim(),
                            phone.getText().trim(), addr.getText().trim(), proof.getText().trim(), nat.getText().trim());
                    hotel.store().addGuest(g);
                    return g;
                } else {
                    existing.setFullName(name.getText().trim());
                    existing.setEmail(email.getText().trim());
                    existing.setPhone(phone.getText().trim());
                    existing.setAddress(addr.getText().trim());
                    existing.setIdProof(proof.getText().trim());
                    existing.setNationality(nat.getText().trim());
                    return existing;
                }
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.ERROR, "Validation", ex.getMessage());
                return null;
            }
        });
        Optional<Guest> r = d.showAndWait();
        if (r.isPresent()) onSave.run();
    }

    // ==================== BOOKINGS ====================
    private javafx.scene.Node bookingsView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Bookings");

        HBox actions = new HBox(10);
        Button create = primaryButton("NEW BOOKING");
        Button view = secondaryButton("VIEW DETAILS");
        Button addSvc = secondaryButton("ADD SERVICE");
        Button cancel = secondaryButton("CANCEL BOOKING");
        Button invoiceBtn = secondaryButton("GENERATE INVOICE (PDF)");

        TableView<Booking> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                col("ID", Booking::getId),
                col("Guest", b -> b.getGuest().getFullName()),
                col("Room", b -> b.getRoom().getNumber()),
                col("In", b -> FMT.format(b.getCheckIn())),
                col("Out", b -> FMT.format(b.getCheckOut())),
                col("Nights", b -> String.valueOf(b.getNights())),
                col("Status", b -> b.getStatus().getLabel()),
                col("Total", b -> "$" + String.format("%,.2f", billing.total(b))),
                col("Balance", b -> "$" + String.format("%,.2f", billing.balance(b)))
        );
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(
                hotel.store().getBookings().stream()
                        .sorted((a, b) -> b.getId().compareTo(a.getId()))
                        .collect(Collectors.toList())));
        refresh.run();
        VBox.setVgrow(table, Priority.ALWAYS);

        create.setOnAction(e -> { bookingDialog(); refresh.run(); });

        view.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            alert(Alert.AlertType.INFORMATION, "Booking " + b.getId(), bookingSummary(b));
        });

        addSvc.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            addServiceDialog(b);
            refresh.run();
        });

        cancel.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            if (!confirm("Cancel booking " + b.getId() + "?")) return;
            try { hotel.cancel(b.getId()); refresh.run(); }
            catch (Exception ex) { alert(Alert.AlertType.ERROR, "Cannot Cancel", ex.getMessage()); }
        });

        invoiceBtn.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Invoice");
            fc.setInitialFileName("Invoice_" + b.getId() + ".pdf");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File f = fc.showSaveDialog(stage);
            if (f == null) return;
            try {
                invoice.generate(b, f);
                alert(Alert.AlertType.INFORMATION, "Invoice Saved", "Saved to:\n" + f.getAbsolutePath());
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Invoice Failed", String.valueOf(ex.getMessage()));
            }
        });

        actions.getChildren().addAll(create, view, addSvc, cancel, invoiceBtn);
        box.getChildren().addAll(h, actions, table);
        return box;
    }

    private String bookingSummary(Booking b) {
        StringBuilder sb = new StringBuilder();
        sb.append("Guest:     ").append(b.getGuest().getFullName()).append('\n');
        sb.append("Room:      ").append(b.getRoom().getNumber()).append(" (").append(b.getRoom().getType().getLabel()).append(")\n");
        sb.append("Stay:      ").append(FMT.format(b.getCheckIn())).append(" -> ").append(FMT.format(b.getCheckOut()))
                .append(" (").append(b.getNights()).append(" nights)\n");
        sb.append("Status:    ").append(b.getStatus().getLabel()).append("\n\n");
        sb.append(String.format("Room charge:      $%,.2f%n", billing.roomCharge(b)));
        sb.append(String.format("Services:         $%,.2f%n", billing.servicesCharge(b)));
        for (BookingService bs : b.getServices())
            sb.append("   - ").append(bs.getService().getName()).append(" x").append(bs.getQuantity()).append('\n');
        sb.append(String.format("Subtotal:         $%,.2f%n", billing.subtotal(b)));
        sb.append(String.format("Discount (%.1f%%):  -$%,.2f%n", b.getDiscountPercent(), billing.discount(b)));
        sb.append(String.format("Tax (12%%):        $%,.2f%n", billing.tax(b)));
        sb.append(String.format("TOTAL:            $%,.2f%n", billing.total(b)));
        sb.append(String.format("Paid:             $%,.2f%n", billing.paidAmount(b.getId())));
        sb.append(String.format("Balance:          $%,.2f%n", billing.balance(b)));
        return sb.toString();
    }

    private void bookingDialog() {
        Dialog<Booking> d = new Dialog<>();
        d.setTitle("New Booking");
        d.getDialogPane().setStyle(dialogStyle());
        ButtonType saveType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Guest> guestBox = new ComboBox<>(FXCollections.observableArrayList(
                hotel.store().getGuests().stream()
                        .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                        .collect(Collectors.toList())));
        guestBox.setPromptText("Select guest");
        guestBox.setPrefWidth(280);
        styleCombo(guestBox);

        DatePicker ci = new DatePicker(LocalDate.now());
        DatePicker co = new DatePicker(LocalDate.now().plusDays(1));
        styleDatePicker(ci); styleDatePicker(co);

        ComboBox<Room> roomBox = new ComboBox<>();
        roomBox.setPromptText("Select room");
        roomBox.setPrefWidth(280);
        roomBox.setConverter(new StringConverter<Room>() {
            @Override public String toString(Room r) {
                return r == null ? "" : r.getNumber() + " - " + r.getType().getLabel()
                        + " ($" + String.format("%,.0f", r.getBasePrice()) + "/night, sleeps " + r.getCapacity() + ")";
            }
            @Override public Room fromString(String s) { return null; }
        });
        styleCombo(roomBox);

        Runnable loadRooms = () -> {
            roomBox.getItems().clear();
            if (ci.getValue() != null && co.getValue() != null && co.getValue().isAfter(ci.getValue()))
                roomBox.getItems().addAll(hotel.searchRooms(null, ci.getValue(), co.getValue(), null, null));
        };
        ci.valueProperty().addListener((o, a, b) -> loadRooms.run());
        co.valueProperty().addListener((o, a, b) -> loadRooms.run());
        loadRooms.run();

        TextField disc = styledField("Discount % (0-50)");
        disc.setText("0");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, label("Guest"), guestBox);
        grid.addRow(1, label("Check-in"), ci);
        grid.addRow(2, label("Check-out"), co);
        grid.addRow(3, label("Room"), roomBox);
        grid.addRow(4, label("Discount"), disc);
        d.getDialogPane().setContent(grid);

        // Validate without closing the dialog on error
        Button okBtn = (Button) d.getDialogPane().lookupButton(saveType);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            try {
                double dv = disc.getText().isBlank() ? 0 : Double.parseDouble(disc.getText().trim());
                Booking b = hotel.createBooking(guestBox.getValue(), roomBox.getValue(), ci.getValue(), co.getValue(), dv);
                alert(Alert.AlertType.INFORMATION, "Booking Created", "Booking " + b.getId() + " created.");
            } catch (NumberFormatException ex) {
                alert(Alert.AlertType.ERROR, "Validation", "Discount must be a number.");
                ev.consume();
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Cannot Create Booking", ex.getMessage());
                ev.consume();
            }
        });
        d.showAndWait();
    }

    private void addServiceDialog(Booking b) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Add Service - " + b.getId());
        d.getDialogPane().setStyle(dialogStyle());
        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        ComboBox<HotelService> svc = new ComboBox<>(FXCollections.observableArrayList(hotel.store().getServices()));
        svc.setPromptText("Select service");
        svc.setPrefWidth(280);
        styleCombo(svc);
        Spinner<Integer> qty = new Spinner<>(1, 50, 1);
        qty.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, label("Service"), svc);
        grid.addRow(1, label("Quantity"), qty);
        d.getDialogPane().setContent(grid);

        Button ok = (Button) d.getDialogPane().lookupButton(addType);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            try {
                if (svc.getValue() == null) throw new IllegalArgumentException("Select a service.");
                qty.commitValue();
                hotel.addService(b, svc.getValue(), qty.getValue());
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Cannot Add Service", ex.getMessage());
                ev.consume();
            }
        });
        d.showAndWait();
    }

    // ==================== CHECK-IN / OUT ====================
    private javafx.scene.Node checkInOutView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Check-In / Check-Out");

        TableView<Booking> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                col("ID", Booking::getId),
                col("Guest", b -> b.getGuest().getFullName()),
                col("Room", b -> b.getRoom().getNumber()),
                col("In", b -> FMT.format(b.getCheckIn())),
                col("Out", b -> FMT.format(b.getCheckOut())),
                col("Status", b -> b.getStatus().getLabel()),
                col("Balance", b -> "$" + String.format("%,.2f", billing.balance(b)))
        );
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(
                hotel.store().getBookings().stream()
                        .filter(b -> b.getStatus() == BookingStatus.RESERVED || b.getStatus() == BookingStatus.CHECKED_IN)
                        .sorted((a, b) -> a.getCheckIn().compareTo(b.getCheckIn()))
                        .collect(Collectors.toList())));
        refresh.run();
        VBox.setVgrow(table, Priority.ALWAYS);

        Button in = primaryButton("CHECK IN");
        Button out = primaryButton("CHECK OUT");

        in.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            try { hotel.checkIn(b.getId()); refresh.run(); }
            catch (Exception ex) { alert(Alert.AlertType.ERROR, "Check-In Failed", ex.getMessage()); }
        });
        out.setOnAction(e -> {
            Booking b = table.getSelectionModel().getSelectedItem();
            if (b == null) { alert(Alert.AlertType.WARNING, "Select", "Select a booking first."); return; }
            double bal = billing.balance(b);
            if (bal > 0.001 && !confirm(String.format("Outstanding balance is $%,.2f. Check out anyway?", bal))) return;
            try {
                hotel.checkOut(b.getId());
                refresh.run();
                alert(Alert.AlertType.INFORMATION, "Checked Out", bookingSummary(b));
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, "Check-Out Failed", ex.getMessage()); }
        });

        HBox actions = new HBox(10, in, out);
        box.getChildren().addAll(h, actions, table);
        return box;
    }

    // ==================== SERVICES ====================
    private javafx.scene.Node servicesView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Hotel Services");

        TableView<HotelService> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                col("Code", HotelService::getCode),
                col("Service", HotelService::getName),
                col("Price", s -> "$" + String.format("%,.2f", s.getPrice())),
                col("Billing", s -> s.isPerNight() ? "Per night" : "One-time")
        );
        table.setItems(FXCollections.observableArrayList(hotel.store().getServices()));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button add = primaryButton("ADD TO BOOKING");
        add.setOnAction(e -> {
            HotelService s = table.getSelectionModel().getSelectedItem();
            if (s == null) { alert(Alert.AlertType.WARNING, "Select", "Select a service first."); return; }
            List<Booking> active = hotel.store().getBookings().stream()
                    .filter(b -> b.getStatus() == BookingStatus.RESERVED || b.getStatus() == BookingStatus.CHECKED_IN)
                    .sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .collect(Collectors.toList());
            if (active.isEmpty()) { alert(Alert.AlertType.WARNING, "No Bookings", "There are no active bookings."); return; }

            Dialog<Void> d = new Dialog<>();
            d.setTitle("Add " + s.getName());
            d.getDialogPane().setStyle(dialogStyle());
            ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            d.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

            ComboBox<Booking> bk = new ComboBox<>(FXCollections.observableArrayList(active));
            bk.setPromptText("Select booking");
            bk.setPrefWidth(300);
            bk.setConverter(bookingConverter());
            styleCombo(bk);
            Spinner<Integer> qty = new Spinner<>(1, 50, 1);
            qty.setEditable(true);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            grid.addRow(0, label("Booking"), bk);
            grid.addRow(1, label("Quantity"), qty);
            d.getDialogPane().setContent(grid);

            Button ok = (Button) d.getDialogPane().lookupButton(addType);
            ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                try {
                    if (bk.getValue() == null) throw new IllegalArgumentException("Select a booking.");
                    qty.commitValue();
                    hotel.addService(bk.getValue(), s, qty.getValue());
                } catch (Exception ex) {
                    alert(Alert.AlertType.ERROR, "Cannot Add Service", ex.getMessage());
                    ev.consume();
                }
            });
            d.showAndWait();
        });

        box.getChildren().addAll(h, new HBox(10, add), table);
        return box;
    }

    private StringConverter<Booking> bookingConverter() {
        return new StringConverter<Booking>() {
            @Override public String toString(Booking b) {
                return b == null ? "" : b.getId() + " - " + b.getGuest().getFullName() + " (Room " + b.getRoom().getNumber() + ")";
            }
            @Override public Booking fromString(String s) { return null; }
        };
    }

    // ==================== PAYMENTS ====================
    private javafx.scene.Node paymentsView() {
        VBox box = new VBox(16);
        Label h = sectionTitle("Payments");

        TableView<Payment> table = new TableView<>();
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                col("Transaction", Payment::getTransactionId),
                col("Booking", Payment::getBookingId),
                col("Guest", p -> {
                    Booking b = hotel.store().findBooking(p.getBookingId());
                    return b == null ? "-" : b.getGuest().getFullName();
                }),
                col("Amount", p -> "$" + String.format("%,.2f", p.getAmount())),
                col("Method", p -> p.getMethod().getLabel()),
                col("Status", p -> p.getStatus().getLabel()),
                col("Date", p -> p.getTimestamp().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))
        );
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(
                hotel.store().getPayments().stream()
                        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                        .collect(Collectors.toList())));
        refresh.run();
        VBox.setVgrow(table, Priority.ALWAYS);

        Button record = primaryButton("RECORD PAYMENT");
        record.setOnAction(e -> {
            List<Booking> due = hotel.store().getBookings().stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED && billing.balance(b) > 0.001)
                    .sorted((a, b) -> a.getId().compareTo(b.getId()))
                    .collect(Collectors.toList());
            if (due.isEmpty()) { alert(Alert.AlertType.INFORMATION, "All Settled", "No bookings have an outstanding balance."); return; }

            Dialog<Void> d = new Dialog<>();
            d.setTitle("Record Payment");
            d.getDialogPane().setStyle(dialogStyle());
            ButtonType payType = new ButtonType("Record", ButtonBar.ButtonData.OK_DONE);
            d.getDialogPane().getButtonTypes().addAll(payType, ButtonType.CANCEL);

            ComboBox<Booking> bk = new ComboBox<>(FXCollections.observableArrayList(due));
            bk.setPromptText("Select booking");
            bk.setPrefWidth(320);
            bk.setConverter(bookingConverter());
            styleCombo(bk);
            Label balLbl = new Label("Balance: -");
            balLbl.setTextFill(Color.web(GOLD));
            TextField amount = styledField("Amount");
            ComboBox<PaymentMethod> method = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
            method.setValue(PaymentMethod.CARD);
            styleCombo(method);
            bk.valueProperty().addListener((o, a, b) -> {
                if (b != null) {
                    double bal = billing.balance(b);
                    balLbl.setText(String.format("Balance: $%,.2f", bal));
                    amount.setText(String.format("%.2f", bal));
                }
            });

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            grid.addRow(0, label("Booking"), bk);
            grid.addRow(1, label(""), balLbl);
            grid.addRow(2, label("Amount"), amount);
            grid.addRow(3, label("Method"), method);
            d.getDialogPane().setContent(grid);

            Button ok = (Button) d.getDialogPane().lookupButton(payType);
            ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                try {
                    if (bk.getValue() == null) throw new IllegalArgumentException("Select a booking.");
                    double amt = Double.parseDouble(amount.getText().trim());
                    Payment p = billing.recordPayment(bk.getValue(), amt, method.getValue());
                    alert(Alert.AlertType.INFORMATION, "Payment Recorded", "Transaction " + p.getTransactionId() + " recorded.");
                } catch (NumberFormatException ex) {
                    alert(Alert.AlertType.ERROR, "Validation", "Amount must be a number.");
                    ev.consume();
                } catch (Exception ex) {
                    alert(Alert.AlertType.ERROR, "Payment Failed", ex.getMessage());
                    ev.consume();
                }
            });
            d.showAndWait();
            refresh.run();
        });

        box.getChildren().addAll(h, new HBox(10, record), table);
        return box;
    }

    // ==================== REPORTS ====================
    private javafx.scene.Node reportsView() {
        VBox box = new VBox(18);
        Label h = sectionTitle("Reports");

        List<Booking> all = hotel.store().getBookings().stream().collect(Collectors.toList());
        double billed = all.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .mapToDouble(billing::total).sum();
        double collected = hotel.store().getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                .mapToDouble(Payment::getAmount).sum();
        double outstanding = all.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .mapToDouble(billing::balance).sum();

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                statCard("TOTAL BILLED", "$" + String.format("%,.0f", billed), "Excluding cancelled"),
                statCard("COLLECTED", "$" + String.format("%,.0f", collected), "Paid + partial"),
                statCard("OUTSTANDING", "$" + String.format("%,.0f", outstanding), "Open balances")
        );

        StringBuilder sb = new StringBuilder();
        sb.append("BOOKINGS BY STATUS\n");
        for (BookingStatus s : BookingStatus.values()) {
            long n = all.stream().filter(b -> b.getStatus() == s).count();
            sb.append(String.format("  %-14s %d%n", s.getLabel(), n));
        }
        sb.append("\nROOMS BY STATUS\n");
        for (RoomStatus s : RoomStatus.values()) {
            long n = hotel.store().getRooms().stream().filter(r -> r.getStatus() == s).count();
            sb.append(String.format("  %-14s %d%n", s.getLabel(), n));
        }
        sb.append("\nROOM REVENUE BY TYPE (billed, non-cancelled)\n");
        for (RoomType t : RoomType.values()) {
            double v = all.stream().filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getRoom().getType() == t)
                    .mapToDouble(billing::total).sum();
            sb.append(String.format("  %-14s $%,.2f%n", t.getLabel(), v));
        }
        sb.append("\nCOLLECTED BY PAYMENT METHOD\n");
        for (PaymentMethod m : PaymentMethod.values()) {
            double v = hotel.store().getPayments().stream()
                    .filter(p -> p.getMethod() == m)
                    .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                    .mapToDouble(Payment::getAmount).sum();
            sb.append(String.format("  %-14s $%,.2f%n", m.getLabel(), v));
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setFont(Font.font("Monospaced", 13));
        area.setStyle("-fx-control-inner-background: " + BG_CARD + "; -fx-text-fill: " + TEXT + ";");
        VBox.setVgrow(area, Priority.ALWAYS);

        box.getChildren().addAll(h, cards, area);
        return box;
    }

    // ==================== HELPERS ====================
    private <T> TableColumn<T, String> col(String title, Function<T, String> f) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        return c;
    }

    private Label sectionTitle(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        l.setTextFill(Color.web(TEXT));
        return l;
    }

    private Label label(String t) {
        Label l = new Label(t);
        l.setTextFill(Color.web(TEXT_MUTED));
        return l;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        styleInput(tf, prompt);
        return tf;
    }

    private void styleInput(TextInputControl tf, String prompt) {
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: " + BG_PANEL + "; -fx-text-fill: " + TEXT +
                "; -fx-prompt-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + EMERALD +
                "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
    }

    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle("-fx-base: " + BG_CARD + "; -fx-background-color: " + BG_PANEL +
                "; -fx-border-color: " + EMERALD + "; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private void styleDatePicker(DatePicker dp) {
        dp.setStyle("-fx-base: " + BG_CARD + "; -fx-background-color: " + BG_PANEL +
                "; -fx-border-color: " + EMERALD + "; -fx-border-radius: 4; -fx-background-radius: 4;");
    }

    private Button primaryButton(String t) {
        Button b = new Button(t);
        String base = "-fx-background-color: " + EMERALD + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 9 18; -fx-background-radius: 4; -fx-cursor: hand;";
        String hover = base.replace(EMERALD, EMERALD_LIGHT);
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button secondaryButton(String t) {
        Button b = new Button(t);
        String base = "-fx-background-color: transparent; -fx-text-fill: " + GOLD + "; -fx-border-color: " + GOLD +
                "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 16; -fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(201,162,39,0.15); -fx-text-fill: " + GOLD + "; -fx-border-color: " + GOLD +
                "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 16; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private String ghostButtonStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-border-color: " + TEXT_MUTED +
                "; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;";
    }

    private String navButtonStyle(boolean selected) {
        return selected
                ? "-fx-background-color: " + EMERALD + "; -fx-text-fill: " + GOLD + "; -fx-font-size: 13px; -fx-padding: 11 14; -fx-background-radius: 4;"
                : "-fx-background-color: transparent; -fx-text-fill: " + TEXT + "; -fx-font-size: 13px; -fx-padding: 11 14; -fx-background-radius: 4;";
    }

    private String tableStyle() {
        return "-fx-base: " + BG_CARD + "; -fx-control-inner-background: " + BG_CARD +
                "; -fx-background-color: " + BG_CARD + "; -fx-table-cell-border-color: #1d3a31;" +
                " -fx-border-color: " + EMERALD + ";";
    }

    private String dialogStyle() {
        return "-fx-base: " + BG_CARD + "; -fx-background-color: " + BG_CARD + ";";
    }

    private VBox statCard(String title, String value, String subtitle) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(18));
        c.setPrefWidth(200);
        HBox.setHgrow(c, Priority.ALWAYS);
        c.setStyle("-fx-background-color: " + BG_CARD + "; -fx-background-radius: 8; -fx-border-color: " +
                EMERALD + "; -fx-border-radius: 8;");
        Label t = new Label(title);
        t.setTextFill(Color.web(TEXT_MUTED));
        t.setFont(Font.font("System", FontWeight.BOLD, 11));
        Label v = new Label(value);
        v.setTextFill(Color.web(GOLD));
        v.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        Label s = new Label(subtitle);
        s.setTextFill(Color.web(TEXT_MUTED));
        s.setFont(Font.font("System", 11));
        c.getChildren().addAll(t, v, s);
        return c;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg == null ? "An unexpected error occurred." : msg);
        a.getDialogPane().setStyle(dialogStyle());
        a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Please confirm");
        a.getDialogPane().setStyle(dialogStyle());
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
