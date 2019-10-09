package org.openjfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.openjfx.model.Datasource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        String fxmlFile = "/org/openjfx/program.fxml";
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent rootNode = fxmlLoader.load(getClass().getResourceAsStream(fxmlFile));

        final Controller controller = fxmlLoader.getController();
        controller.initMapAndControls();

        Scene scene = new Scene(rootNode,502,500);

        primaryStage.setTitle("CompanyToggleServerMaven");
        primaryStage.setX(0);
        primaryStage.setY(0);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Thread acceptingConnections = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(5777)) {
                while(true) {
                    Socket socket = server.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
                    String string = input.readLine();
                    System.out.println(string);
                    String response;
                    switch (string.split(":")[0]) {
                        case "LOGIN":
                            response = Datasource.getInstance().loginCheck(string.split(":")[1],string.split(":")[2]);
                            if(response==null) {
                                output.println("Issue with server database. Can't log in.:"+new Random().nextInt());
                            } else {
                                output.println(response+new Random().nextInt());
                            }
                            break;
                        case "CHECKIN":
                            response = Datasource.getInstance().checkInOut(true,string.split(":",2)[1]);
                            if(response==null) {
                                output.println("Issue with server database. Can't log in.:"+new Random().nextInt());
                            } else {
                                output.println(response+new Random().nextInt());
                            }
                            break;
                        case "CHECKOUT":
                            response = Datasource.getInstance().checkInOut(false,string.split(":",2)[1]);
                            if(response==null) {
                                output.println("Issue with server database. Can't log in.:"+new Random().nextInt());
                            } else {
                                output.println(response+new Random().nextInt());
                            }
                            break;
                    }
                    socket.close();
                    input.close();
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.exit();
            }
        });
        acceptingConnections.start();
        launch();
    }

    @Override
    public void init() throws Exception {
        super.init();
        Datasource.getInstance().open();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        Datasource.getInstance().close();
    }

}