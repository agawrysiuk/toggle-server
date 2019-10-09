package org.openjfx.model;

import javafx.application.Platform;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Datasource {

    private static Datasource instance = new Datasource();
    private Connection conn;

    public static final String DB_NAME = "employees.db";
    public static final String CONNECTION_INFO = "jdbc:sqlite:C:\\Users\\admin\\IdeaProjects\\CompanyToggleServerMAVEN\\src\\main\\resources\\" + DB_NAME;

    public static final String VIEW_FULL_INFO = "full_info";

    public static final String COLUMN_EMPLOYEE_ID = "employee_id";

    public static final String TABLE_INFORMATION = "information";
    public static final String COLUMN_INFORMATION_NAME = "name";
    public static final String COLUMN_INFORMATION_SURNAME = "surname";
    public static final String COLUMN_INFORMATION_EMAIL = "email";

    public static final String TABLE_APP = "app";
    public static final String COLUMN_APP_PIN = "login_pin";
    public static final String COLUMN_APP_ONLINE = "online";

    public static final String TABLE_WORKING = "working_time";
    public static final String COLUMN_WORKING_IN = "date_in";
    public static final String COLUMN_WORKING_OUT = "date_out";

    public static final String TABLE_LOCATION = "location";
    public static final String COLUMN_LOCATION_YEAR = "year";
    public static final String COLUMN_LOCATION_MONTH = "month";
    public static final String COLUMN_LOCATION_DAY = "day";
    public static final String COLUMN_LOCATION_LATITUDE = "latitude";
    public static final String COLUMN_LOCATION_LONGITUDE = "longitude";

    public static final String CREATE_FULL_INFO = "CREATE VIEW IF NOT EXISTS " + VIEW_FULL_INFO + " AS " +
            "SELECT " + TABLE_INFORMATION + "." + COLUMN_EMPLOYEE_ID + "," + TABLE_INFORMATION + "." + COLUMN_INFORMATION_NAME + "," + TABLE_INFORMATION + "." + COLUMN_INFORMATION_SURNAME + "," + TABLE_INFORMATION + "." + COLUMN_INFORMATION_EMAIL + "," + TABLE_APP + "." + COLUMN_APP_PIN + "," + TABLE_APP + "." + COLUMN_APP_ONLINE + " FROM " + TABLE_INFORMATION + " " +
            "INNER JOIN " + TABLE_APP + " ON " + TABLE_INFORMATION + "." + COLUMN_EMPLOYEE_ID + " = " + TABLE_APP + "." + COLUMN_EMPLOYEE_ID + " " +
            "ORDER BY " + TABLE_INFORMATION + "." + COLUMN_INFORMATION_SURNAME + " COLLATE NOCASE";

    public static final String QUERY_FULL_INFO = "SELECT * FROM " + VIEW_FULL_INFO;

    public static final String QUERY_EMPLOYEE_HOURS = "SELECT * FROM " + TABLE_WORKING +
            " WHERE " + COLUMN_EMPLOYEE_ID + " = ?";

    public static final String QUERY_EMAIL_FOR_LOGIN = "SELECT count(*) AS count FROM " + TABLE_INFORMATION +
            " WHERE " + COLUMN_INFORMATION_EMAIL + " = ?";

    public static final String QUERY_LOGIN_CHECK = "SELECT " + COLUMN_APP_PIN + "," + COLUMN_APP_ONLINE + " FROM " + VIEW_FULL_INFO +
            " WHERE " + COLUMN_INFORMATION_EMAIL + " = ?";

    public static final String CHECK_IN_STRING = "UPDATE " + TABLE_APP + " SET " + COLUMN_APP_ONLINE + "= ? " +
            "WHERE " + COLUMN_EMPLOYEE_ID + "= ?";

    public static final String INSERT_NEW_CHECKIN_TIME = "INSERT INTO " + TABLE_WORKING + "(" + COLUMN_EMPLOYEE_ID + "," +
            COLUMN_WORKING_IN + ") VALUES(?,?)";

    public static final String INSERT_NEW_CHECKOUT_TIME = "UPDATE " + TABLE_WORKING + " SET " + COLUMN_WORKING_OUT + "= ? " +
            "WHERE " + COLUMN_EMPLOYEE_ID + "= ? AND " + COLUMN_WORKING_OUT + " IS NULL OR " + COLUMN_WORKING_OUT + "=''";

    public static final String GET_ID_STRING = "SELECT * FROM " + VIEW_FULL_INFO +
            " WHERE " + COLUMN_INFORMATION_EMAIL + " = ?";

    public static final String ADD_NEW_EMPLOYEE = "INSERT INTO " + TABLE_INFORMATION +
            "(" + COLUMN_INFORMATION_NAME + "," + COLUMN_INFORMATION_SURNAME + "," + COLUMN_INFORMATION_EMAIL + ") " +
            "VALUES (?,?,?)";

    public static final String CREATE_APP_DETAILS = "INSERT INTO " + TABLE_APP + " VALUES(?,?,0)";

    public static final String GET_NUMBER_OF_EMPLOYEES = "SELECT count(*) AS count FROM "+TABLE_INFORMATION;

    public static final String GET_LOCATION_MONTH = "SELECT " + COLUMN_LOCATION_LATITUDE +","+COLUMN_LOCATION_LONGITUDE+
            " FROM "+TABLE_LOCATION+" WHERE " + COLUMN_EMPLOYEE_ID + " = ? " +
            "AND "+COLUMN_LOCATION_YEAR+" = ? " +
            "AND "+COLUMN_LOCATION_MONTH+" = ? ";

    public static final String SET_LOCATION_CHECKINOUT = "INSERT INTO " + TABLE_LOCATION + " VALUES(?,?,?,?,?,?)";

    private PreparedStatement createFullInfoIfNotExists;
    private PreparedStatement retrieveDatabase;
    private PreparedStatement queryEmployeeHours;
    private PreparedStatement queryLogin;
    private PreparedStatement queryEmailForLogin;
    private PreparedStatement checkInStatement;
    private PreparedStatement getIDStatement;
    private PreparedStatement insertNewCheckInTime;
    private PreparedStatement insertNewCheckOutTime;
    private PreparedStatement addNewEmployee;
    private PreparedStatement createAppLogin;
    private PreparedStatement getEmployeesCount;
    private PreparedStatement getLocationInMonth;
    private PreparedStatement setLocation;

    private Datasource() {
    }

    public static Datasource getInstance() {
        return instance;
    }

    public boolean open() {
        try {
            conn = DriverManager.getConnection(CONNECTION_INFO);

            createFullInfoIfNotExists = conn.prepareStatement(CREATE_FULL_INFO);
            createFullInfoIfNotExists.executeUpdate();

            retrieveDatabase = conn.prepareStatement(QUERY_FULL_INFO);
            queryEmployeeHours = conn.prepareStatement(QUERY_EMPLOYEE_HOURS);
            queryLogin = conn.prepareStatement(QUERY_LOGIN_CHECK);
            queryEmailForLogin = conn.prepareStatement(QUERY_EMAIL_FOR_LOGIN);
            checkInStatement = conn.prepareStatement(CHECK_IN_STRING);
            getIDStatement = conn.prepareStatement(GET_ID_STRING);
            insertNewCheckInTime = conn.prepareStatement(INSERT_NEW_CHECKIN_TIME);
            insertNewCheckOutTime = conn.prepareStatement(INSERT_NEW_CHECKOUT_TIME);
            addNewEmployee = conn.prepareStatement(ADD_NEW_EMPLOYEE);
            createAppLogin = conn.prepareStatement(CREATE_APP_DETAILS);
            getEmployeesCount = conn.prepareStatement(GET_NUMBER_OF_EMPLOYEES);
            getLocationInMonth = conn.prepareStatement(GET_LOCATION_MONTH);
            setLocation = conn.prepareStatement(SET_LOCATION_CHECKINOUT);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.exit();
        }
        return false;
    }

    public void close() {
        try {
            if (setLocation != null) {
                setLocation.close();
            }
            if (getLocationInMonth != null) {
                getLocationInMonth.close();
            }
            if (getEmployeesCount != null) {
                getEmployeesCount.close();
            }
            if (createAppLogin != null) {
                createAppLogin.close();
            }
            if (addNewEmployee != null) {
                addNewEmployee.close();
            }
            if (insertNewCheckOutTime != null) {
                insertNewCheckOutTime.close();
            }
            if (insertNewCheckInTime != null) {
                insertNewCheckInTime.close();
            }
            if (getIDStatement != null) {
                getIDStatement.close();
            }
            if (checkInStatement != null) {
                checkInStatement.close();
            }
            if (queryEmailForLogin != null) {
                queryEmailForLogin.close();
            }
            if (queryLogin != null) {
                queryLogin.close();
            }
            if (createFullInfoIfNotExists != null) {
                createFullInfoIfNotExists.close();
            }
            if (retrieveDatabase != null) {
                retrieveDatabase.close();
            }
            if (queryEmployeeHours != null) {
                queryEmployeeHours.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Employee> retrieveDatabase() {
        if (conn == null) {
            return null;
        }
        try (ResultSet results = retrieveDatabase.executeQuery()) {
            List<Employee> list = new ArrayList<>();
            while (results.next()) {
                Employee employee = new Employee();
                employee.setEmployeeID(results.getInt(1));
                employee.setName(results.getString(2));
                employee.setSurname(results.getString(3));
                employee.setEmail(results.getString(4));
                employee.setEmployeePIN(results.getInt(5));
                employee.setOnline(results.getBoolean(6));
                list.add(employee);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Duration> queryEmployeeAllHours(int employeeID) {
        try {
            queryEmployeeHours.setInt(1, employeeID);
            ResultSet results = queryEmployeeHours.executeQuery();
            Map<String, Duration> map = new HashMap<>();
            while (results.next()) {
                String stringIN = results.getString(2);
                String stringOUT = results.getString(3);
                if (stringOUT == null) {
                    continue;
                }
                LocalDateTime dateIN = LocalDateTime.parse(stringIN);
                LocalDateTime dateOUT = LocalDateTime.parse(stringOUT);
                Duration dur = Duration.between(dateIN, dateOUT);
                String key = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(dateIN);
                if (map.containsKey(key)) {
                    dur = dur.plus(map.get(key));
                    map.put(key, dur);
                } else {
                    map.put(key, dur);
                }
            }
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int emailCheck(String mail) {
        if (conn == null) {
            return 0;
        }
        try {
            queryEmailForLogin.setString(1, mail);
            ResultSet results = queryEmailForLogin.executeQuery();
            return results.getInt("count");
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String loginCheck(String mail, String PIN) {
        if (conn == null) {
            return null;
        }
        if (emailCheck(mail) != 1) {
            return "Incorrect email. Can't log in.:";
        }
        try {
            queryLogin.setString(1, mail);
            ResultSet results = queryLogin.executeQuery();
            boolean match = false;
            String status = "";
            while (results.next()) {
                if (String.valueOf(results.getInt(1)).equals(PIN)) {
                    match = true;
                }
                status = results.getBoolean(2) ? "online" : "offline";
            }
            return match ? "Successfully logged in:" + status + ":" : "Incorrect PIN. Can't log in.:";
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getID(String mail) {
        if (conn == null) {
            return 0;
        }
        if (emailCheck(mail) != 1) {
            return 0;
        }
        try {
            getIDStatement.setString(1, mail);
            ResultSet results = getIDStatement.executeQuery();
            boolean match = false;
            String status = "";
            while (results.next()) {
                return results.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String checkInOut(boolean checkin, String info) {
        if (conn == null) {
            return null;
        }
        String[] array = info.split(":");
        String mail = array[0];
        double latitude = Double.parseDouble(array[1]);
        double longitude = Double.parseDouble(array[2]);
        if (emailCheck(mail) != 1) {
            return "Incorrect email. Can't " + (checkin ? "check-in.:" : "check-out.:");
        }
        int employeeID = getID(mail);
        if (employeeID == 0) {
            return "Couldn't find your employee id.:";
        }
        try {
            conn.setAutoCommit(false);
            checkInStatement.setBoolean(1, checkin);
            checkInStatement.setInt(2, employeeID);
            int affectedRows = checkInStatement.executeUpdate(); //setting up online boolean in DB
            if (affectedRows != 1) {
                conn.rollback();
                return "Couldn't " + (checkin ? "check-in" : "check-out") + ".:";
            }
            LocalDateTime dateTime = LocalDateTime.now();
            if (checkin) { //adding checkin time, inserting new row
                insertNewCheckInTime.setInt(1, employeeID);
                insertNewCheckInTime.setString(2, dateTime.toString());
                affectedRows = insertNewCheckInTime.executeUpdate(); //setting up online boolean in DB
                if (affectedRows != 1) {
                    conn.rollback();
                    return "Couldn't check-in.:";
                }
            } else { //... or adding checkout time
                insertNewCheckOutTime.setInt(2, employeeID);
                insertNewCheckOutTime.setString(1, dateTime.toString());
                affectedRows = insertNewCheckOutTime.executeUpdate(); //setting up online boolean in DB
                if (affectedRows != 1) {
                    conn.rollback();
                    return "Couldn't check-out.:";
                }
            }
            //setting up location checkin;
            setLocation.setInt(1,employeeID);
            setLocation.setString(2, String.valueOf(dateTime.getYear()));
            setLocation.setString(3,String.format("%02d",dateTime.getMonthValue()));
            setLocation.setString(4,String.format("%02d",dateTime.getDayOfMonth()));
            setLocation.setDouble(5,latitude);
            setLocation.setDouble(6,longitude);
            affectedRows = setLocation.executeUpdate();
            if (affectedRows != 1) {
                conn.rollback();
                return "Couldn't " + (checkin ? "check-in" : "check-out") + ".:";
            }
            conn.commit();
            return "Successfully " + (checkin ? "checked-in" : "checked-out") + " at " +
                    String.format("%02d;%02d;%02d", dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond()) +
                    " on " + dateTime.getMonth().toString() + " " + dateTime.getYear() + ":";
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return "Couldn't " + (checkin ? "check-in" : "check-out") + ".:";
    }

    public String addNewEmployee(String name, String surname, String email) {
        String failedString = "Couldn't add new employee.%";
        if (conn == null) {
            return failedString + "No connection to the database.";
        }
        String regex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        if(!matcher.matches()) {
            return failedString + "Incorrect email expression. Use something@company.com";
        }
        if (emailCheck(email) == 1) {
            return failedString + "Such an email already exists.";
        }
        try {
            conn.setAutoCommit(false);
            addNewEmployee.setString(1, name);
            addNewEmployee.setString(2, surname);
            addNewEmployee.setString(3, email);
            int affectedRows = addNewEmployee.executeUpdate();
            if (affectedRows != 1) {
                conn.rollback();
                return failedString + "Something's wrong with inserting new employee.";
            }
            int employeeID = getEmployeesCount.executeQuery().getInt(1); //adding him/her a new ID
            if (employeeID <= 0) {
                conn.rollback();
                return failedString + "There was an error while creating an ID for the employee.";
            }
            int[] pinArray = {new Random().nextInt(9) + 1,
                    new Random().nextInt(10),
                    new Random().nextInt(10),
                    new Random().nextInt(10)};
            int pinString = Integer.parseInt(Arrays.stream(pinArray).mapToObj(String::valueOf).collect(Collectors.joining()));
            createAppLogin.setInt(1,pinString);
            createAppLogin.setInt(2,employeeID);
            affectedRows = createAppLogin.executeUpdate();
            if (affectedRows != 1) {
                conn.rollback();
                return failedString + "Something's wrong with creating user's login details.";
            }
            conn.commit();
            return "Successfully added new employee.%Soon it will appear in the main window.";
        } catch (SQLException e) {
            e.printStackTrace();
            failedString += "SQLEXception: " + e.getMessage();
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return failedString;
    }

    public List<String> getLocationsMonth(int employeeId, String year,String month) {
        if (conn == null) {
            return null;
        }
        try {
            List<String> list = new ArrayList<>();
            getLocationInMonth.setInt(1,employeeId);
            getLocationInMonth.setString(2,year);
            getLocationInMonth.setString(3,month);
            ResultSet results = getLocationInMonth.executeQuery();
            while(results.next()) {
                list.add(results.getString(1)+","+results.getString(2));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
