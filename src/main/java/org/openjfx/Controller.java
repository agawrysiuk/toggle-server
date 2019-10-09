package org.openjfx;

import com.sothawo.mapjfx.*;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.openjfx.model.Datasource;
import org.openjfx.model.Employee;

import java.text.DateFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

public class Controller {

    private static final Coordinate coordWarsaw = new Coordinate(52.2395, 21.0050);

    //ponizszy Extent pozwala nam laczyc kilka coordynatow razem:
//    private static final Extent extentAllLocations = Extent.forCoordinates(coordWarsaw);
    //nastepnie mozemy wywolac nastepujaca metode (zamiast mapView.setCenter(...)), zeby ustawic widok na wszystkie markery
//    mapView.setExtent(extentAllLocations);

    //default zoom value
    private static final int ZOOM_DEFAULT = 14;

    //markers
    private final Marker markerWarsaw;

    @FXML
    private MapView mapView;

    @FXML
    private BorderPane mainPane;
    @FXML
    private TableView<Employee> employeesTable;
    @FXML
    private TableColumn<Employee, String> onlineColumn;

    private List<Coordinate> activeCoordinates;
    private List<Marker> activeMarkers;

    public Controller() {
        markerWarsaw = Marker.createProvided(Marker.Provided.BLUE).setPosition(coordWarsaw).setVisible(false);
    }

    public void initialize() {
        List<Employee> list = Datasource.getInstance().retrieveDatabase();
        employeesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        employeesTable.setItems(FXCollections.observableArrayList(list));
//        employeesTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Employee>() {
//            @Override
//            public void changed(ObservableValue<? extends Employee> observableValue, Employee employee, Employee t1) {
//                textName.setText(t1.getFirstName());
//                textSurname.setText(t1.getSurname());
//                textEmail.setText(t1.getEmail());
//                textEmployeeID.setText(String.valueOf(t1.getEmployeeID()));
//                textEmployeePIN.setText(String.valueOf(t1.getEmployeePIN()));
//            }
//        });
        onlineColumn.setCellValueFactory(employeeStringCellDataFeatures -> {
            if (employeeStringCellDataFeatures.getValue() != null) {
                if (employeeStringCellDataFeatures.getValue().isOnline()) {
                    return new SimpleStringProperty("ONLINE");
                } else {
                    return new SimpleStringProperty("offline");
                }
            }
            return new SimpleStringProperty("");
        });
        onlineColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Employee, String> call(TableColumn<Employee, String> employeeStringTableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String s, boolean b) {
                        super.updateItem(s, b);
                        if (b) {
                            setText(null);
                        } else {
                            setText(s);
                            if (s.equals("ONLINE")) {
                                setTextFill(Color.GREEN);
                            } else {
                                setTextFill(Color.RED);
                            }
                        }
                    }
                };
            }
        });
        ScheduledService<Void> service = new ScheduledService<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Platform.runLater(() -> {
                            int index = employeesTable.getSelectionModel().getSelectedIndex();
                            employeesTable.getSelectionModel().clearSelection();
                            employeesTable.setItems(FXCollections.observableList(Datasource.getInstance().retrieveDatabase()));
                            employeesTable.getSelectionModel().select(index);
                        });
                        return null;
                    }
                };
            }
        };
        service.setPeriod(javafx.util.Duration.seconds(10));
        service.start();
    }

    @FXML
    public void getEmployeeDetails() {
        if (employeesTable.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        Employee employee = employeesTable.getSelectionModel().getSelectedItem();

        //creating a window
        Stage secondStage = new Stage();
        secondStage.setTitle("Info for " + employee.getName());
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(10, 10, 10, 10));

        //top hbox
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setAlignment(Pos.CENTER);

        //calendar
        DatePicker datePicker = new DatePicker(LocalDate.now());
        DatePickerSkin datePickerSkin = new DatePickerSkin(datePicker);

        //comboboxes on the top
        ComboBox<String> pickYear = new ComboBox<>();
        pickYear.setPrefWidth(150);
        String[] array = IntStream.range(1989, LocalDateTime.now().getYear() + 1).map(i -> LocalDateTime.now().getYear() - i + 1989).mapToObj(String::valueOf).toArray(value -> new String[(LocalDateTime.now().getYear() + 1 - 1989)]);
        pickYear.setItems(FXCollections.observableArrayList(new ArrayList<>(Arrays.asList(array))));
        ComboBox<String> pickMonth = new ComboBox<>();
        array = IntStream.range(1, 13).mapToObj(i -> {
            if (i < 10) {
                return "0" + i;
            } else {
                return String.valueOf(i);
            }
        }).toArray(value -> new String[12]);
        pickMonth.setItems(FXCollections.observableArrayList(array));
        pickMonth.setPrefWidth(150);
        pickMonth.setDisable(true);

        Map<String, Duration> map = Datasource.getInstance().queryEmployeeAllHours(employee.getEmployeeID()); //getting info about the employee

        TextFlow textFlow = new TextFlow();
        //what you are doing after picking a year
        pickYear.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 != null) {
                //enable picking a month
                pickMonth.setDisable(false);
                pickMonth.getSelectionModel().clearSelection();
            }
        });
        //what you are doing after picking a month
        pickMonth.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            //everything here works only if there is some data, otherwise all controls are disabled
            if (t1 != null) {
                //removing old line chart if exists
                removeLineChart(grid);
                //making a window bigger for all the data
                secondStage.getScene().getWindow().setHeight(900);
                secondStage.getScene().getWindow().setWidth(1000);
                //moving the values from comboboxes to the calendar
                datePicker.setValue(LocalDate.parse(pickYear.getValue() + "-" + pickMonth.getValue() + "-01"));
                //setting up statistics for the current month
                textFlow.getChildren().clear();
                textFlow.getChildren().add(new Text(employee.getName() + "'s statistics for " +
                        new DateFormatSymbols(Locale.US).getMonths()[Integer.parseInt(pickMonth.getValue()) - 1] + " " + pickYear.getValue() + "\n" +
                        "Total working hours: "));
                Map<Integer,Duration> mapForChart = new HashMap<>();
                Duration fullDurMonth = Duration.ZERO;
                for (Map.Entry<String, Duration> entry : map.entrySet()) {
                    if (pickYear.getValue().equals(entry.getKey().substring(6, 10)) && pickMonth.getValue().equals(entry.getKey().substring(3, 5))) {
                        fullDurMonth = fullDurMonth.plus(entry.getValue());
                        int dayOf = Integer.parseInt(entry.getKey().substring(0,2));
                        if(mapForChart.containsKey(dayOf)) {
                            mapForChart.put(dayOf,mapForChart.get(dayOf).plus(entry.getValue()));
                        } else {
                            mapForChart.put(dayOf,entry.getValue());
                        }
                    }

                }
                Text totalWorking = new Text(String.format("%02dh %02dm %02ds\n", fullDurMonth.toHoursPart(), fullDurMonth.toMinutesPart(), fullDurMonth.toSecondsPart()));
                totalWorking.setStyle("-fx-font-weight: bold;");
                textFlow.getChildren().add(totalWorking);
                //setting up a graph
                NumberAxis xAxis = new NumberAxis(0,
                        (YearMonth.of
                                (Integer.parseInt(pickYear.getValue()),
                                        Integer.parseInt(pickMonth.getValue()))).lengthOfMonth(),
                        1);
                xAxis.setLabel("Days");
                NumberAxis yAxis = new NumberAxis(0,12,1);
                yAxis.setLabel("Hours");
                LineChart lineChart  = new LineChart(xAxis,yAxis);
                if(!mapForChart.isEmpty()) {
                    XYChart.Series series = new XYChart.Series();
                    series.setName("Hours worked on a particular day for " +
                            new DateFormatSymbols(Locale.US).getMonths()[Integer.parseInt(pickMonth.getValue()) - 1] + " " + pickYear.getValue());
                    for(Map.Entry<Integer,Duration> item : mapForChart.entrySet()) {
                        series.getData().add(new XYChart.Data(item.getKey(), item.getValue().toHoursPart()+item.getValue().toMinutesPart()/60.0));
                    }
                    lineChart.getData().add(series);
                    grid.add(lineChart,0,3);
                }

                //setting up map
//                mapView.setPrefSize(500,900);
                mapView.setPrefWidth(900);
                mapView.setPrefHeight(900);
                List<String> listString = Datasource.getInstance().getLocationsMonth(employee.getEmployeeID(),pickYear.getValue(),pickMonth.getValue());
                //deleting old markers
                if(activeMarkers!=null && !activeMarkers.isEmpty()) {
                    for(Marker marker : activeMarkers) {
                        mapView.removeMarker(marker);
                    }
                } else {
                    activeMarkers = new ArrayList<>();
                }
                //adding new ones
                if(listString!=null && !listString.isEmpty()) {
                    if(activeCoordinates==null || !activeCoordinates.isEmpty()) {
                        activeCoordinates = new ArrayList<>();
                    }
                    for(String stringLoc : listString) {
                        Coordinate coordinate = new Coordinate(
                                Double.parseDouble(stringLoc.split(",")[0]),
                                Double.parseDouble(stringLoc.split(",")[1]));
                        activeCoordinates.add(coordinate);
                        Marker marker = Marker.createProvided(Marker.Provided.BLUE).setPosition(coordinate).setVisible(true);
                        activeMarkers.add(marker);
                        mapView.addMarker(marker);
                    }
                    Extent extentAllLocations = Extent.forCoordinates(activeCoordinates);
                    mapView.setExtent(extentAllLocations);
//                            Extent.forCoordinates(coordKarlsruheCastle,
//                            coordKarlsruheHarbour, coordKarlsruheStation, coordKarlsruheSoccer);
                    if(!grid.getChildren().contains(mapView)) {
                        grid.add(mapView,1,0,1,4); //span for 1 column and 3 rows, two last digits
                    }

                    }
                // TODO: 2019-10-04 add markers to existing map
            }
        });
        //setting up calendar
        if (map != null && !map.isEmpty()) {
            datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
                public DateCell call(final DatePicker datePicker) {
                    return new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            setMinSize(60, 60);
                            for (Map.Entry<String, Duration> employeesItem : map.entrySet()) {
                                if (item.equals(LocalDate.parse(employeesItem.getKey(), DateTimeFormatter.ofPattern("dd-MM-yyyy")))) {
                                    Duration dur = employeesItem.getValue();
                                    setText(getText() + "\n" + String.format("%02d:%02d:%02d", dur.toHoursPart(), dur.toMinutesPart(), dur.toSecondsPart()));
                                    if (dur.toHoursPart() < 8) {
                                        setStyle("-fx-background-color: #ff4444;");
                                    } else {
                                        setStyle("-fx-background-color: #009933;");
                                    }
                                } else {
                                    setText(getText());
                                }
                            }
                        }
                    };
                }
            });
        } else {
            datePicker.setDayCellFactory(new Callback<DatePicker, DateCell>() {
                public DateCell call(final DatePicker datePicker) {
                    return new DateCell() {
                        @Override
                        public void updateItem(LocalDate item, boolean empty) {
                            super.updateItem(item, empty);
                            setMinSize(60, 60);
                            setText(getText() + "\n\t\t");
                        }
                    };
                }
            });
        }
        hBox.getChildren().addAll(new Text("Pick year:"), pickYear, new Text("Pick month:"), pickMonth);

        //getting a calendar
        Node dateContent = datePickerSkin.getPopupContent();
        dateContent.prefWidth(500);
        dateContent.prefHeight(500);
        //disabling controls if we have no data
        if (map == null || map.isEmpty()) {
            dateContent.setDisable(true);
            pickMonth.setDisable(true);
            pickYear.setDisable(true);
            textFlow.getChildren().add(new Text("Employee " + employee.getName() + " doesn't have any sign-ins to work."));
        } else {
            textFlow.getChildren().add(new Text("Pick a month with a drop-down list at the top to see statistics."));
//            createMap();
        }
        grid.add(hBox, 0, 0);
        grid.add(dateContent, 0, 1);
        grid.add(textFlow, 0, 2);

        secondStage.setScene(new Scene(grid, 480, 480));
        secondStage.setX(517);
        secondStage.setY(0);

        secondStage.initModality(Modality.APPLICATION_MODAL); //this is the only window you can use
        secondStage.initOwner(mainPane.getScene().getWindow());
        secondStage.show();
    }

    private void removeLineChart(GridPane gridPane) {
        ObservableList<Node> children = gridPane.getChildren();
        for(Node node : children) {
            if(node instanceof LineChart && gridPane.getRowIndex(node) == 3) {
                LineChart lineChart = (LineChart) node;
                gridPane.getChildren().remove(lineChart);
                break;
            }
        }
    }

    public void addEmployee() {
        Dialog<ButtonType> secondStage = new Dialog<>();
        secondStage.setTitle("Add new employee");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.add(new Text("Employee name: "),0,0);
        grid.add(new Text("Employee surname: "),0,1);
        grid.add(new Text("Employee company mail: "),0,2);
        TextField tfName = new TextField();
        TextField tfSurname = new TextField();
        TextField tfEmail = new TextField();
        tfName.setPrefWidth(150);
        tfSurname.setPrefWidth(150);
        tfEmail.setPrefWidth(150);
        grid.add(tfName,1,0);
        grid.add(tfSurname,1,1);
        grid.add(tfEmail,1,2);

        secondStage.getDialogPane().getButtonTypes().setAll(ButtonType.OK,ButtonType.CANCEL);
        BooleanBinding booleanBinding = tfName.textProperty().isEqualTo("")
                .or(tfSurname.textProperty().isEqualTo(""))
                .or(tfEmail.textProperty().isEqualTo(""));
        secondStage.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(booleanBinding);
        secondStage.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, actionEvent -> {
            String name = tfName.getText();
            String surname = tfSurname.getText();
            String email = tfEmail.getText();
            if(!executeAddingShowAlert(name,surname,email)) {
                actionEvent.consume();
            }
        });

        secondStage.initModality(Modality.APPLICATION_MODAL);
        secondStage.initOwner(mainPane.getScene().getWindow());

        secondStage.getDialogPane().setContent(grid);
        secondStage.showAndWait();
    }

    private boolean executeAddingShowAlert(String name,String surname,String email) {
        String dbResponse = Datasource.getInstance().addNewEmployee(name,surname,email);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Database response");
        alert.setHeaderText(dbResponse.split("%")[0]);
        alert.setContentText(dbResponse.split("%")[1]);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(mainPane.getScene().getWindow());
        alert.show();
        return dbResponse.contains("Successfully added new employee.");
    }

//    private void createMap() {
//        //tu najpierw wydobycie listy??
////        List<Coordinate> listCoords = new ArrayList<>();
////        if(listCoords == null || listCoords.isEmpty()) {
////            return null;
////        }
//        Coordinate coordWarsaw = new Coordinate(52.2395, 21.0050);
////        Marker markerWarsaw = Marker.createProvided(Marker.Provided.BLUE).setPosition(coordWarsaw).setVisible(false);
//
//        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue) {
//                mapView.setZoom(14);
//                mapView.setCenter(coordWarsaw);
////                mapView.addMarker(markerWarsaw);
//            }
//        });
//
//        mapView.initialize(Configuration.builder()
//                .projection(Projection.WEB_MERCATOR)
//                .showZoomControls(true)
//                .build());
//
//    }

    private boolean addMarkers(int employeeID,String year,String month) {

        return false;
    }

    public void initMapAndControls() {
        // watch the MapView's initialized property to finish initialization
        mapView = new MapView();
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mapView.setZoom(ZOOM_DEFAULT);
                mapView.setCenter(coordWarsaw);
                mapView.addMarker(markerWarsaw);
            }
        });

        // observe the map type radiobuttons
        mapView.initialize(Configuration.builder()
                .projection(Projection.WEB_MERCATOR)
                .showZoomControls(true)
                .build());


    }

}
