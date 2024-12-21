package com.example.connect;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application {
    private boolean isSignUpMode = false; // Tracks Sign-In or Sign-Up mode

    @Override
    public void start(Stage primaryStage) {
        // Main Layout
        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(10);
        root.setAlignment(Pos.CENTER);

        // Title Label
        Label titleLabel = new Label("Sign In");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

        // Form Layout
        GridPane formGrid = new GridPane();
        formGrid.setPadding(new Insets(20));
        formGrid.setVgap(15);
        formGrid.setHgap(10);
        formGrid.setAlignment(Pos.CENTER);

        // Fields and Labels
        Label usernameLabel = new Label("Username:");
        usernameLabel.setTextFill(Color.WHITE);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        Label emailLabel = new Label("Email:");
        emailLabel.setTextFill(Color.WHITE);
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");

        usernameLabel.setVisible(false);
        usernameField.setVisible(false);

        Label passwordLabel = new Label("Password:");
        passwordLabel.setTextFill(Color.WHITE);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Label confirmPasswordLabel = new Label("Confirm Password:");
        confirmPasswordLabel.setTextFill(Color.WHITE);
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-enter your password");
        confirmPasswordLabel.setVisible(false);
        confirmPasswordField.setVisible(false);

        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // Submit Button
        Button submitButton = new Button("Sign In");
        submitButton.setPrefWidth(200);
        submitButton.setOnAction(e -> {
            errorLabel.setVisible(false);
            if (isSignUpMode) {
                if (!validateSignUpForm(usernameField, emailField, passwordField, confirmPasswordField, errorLabel)) {
                    errorLabel.setVisible(true);
                    return;
                }
                sendRequest("http://localhost:8080/api/auth/signup",
                        createPayload(usernameField, emailField, passwordField), errorLabel, primaryStage);
            } else {
                if (!validateSignInForm(emailField, passwordField, errorLabel)) {
                    errorLabel.setVisible(true);
                    return;
                }
                sendRequest("http://localhost:8080/api/auth/login", createPayload(null, emailField, passwordField),
                        errorLabel, primaryStage);
            }
        });

        // Toggle Button
        Button toggleButton = new Button("Switch to Sign Up");
        toggleButton.setPrefWidth(200);
        toggleButton.setOnAction(e -> {
            isSignUpMode = !isSignUpMode;
            if (isSignUpMode) {
                titleLabel.setText("Sign Up");
                submitButton.setText("Sign Up");
                toggleButton.setText("Switch to Sign In");
                usernameLabel.setVisible(true);
                usernameField.setVisible(true);
                confirmPasswordLabel.setVisible(true);
                confirmPasswordField.setVisible(true);
            } else {
                titleLabel.setText("Sign In");
                submitButton.setText("Sign In");
                toggleButton.setText("Switch to Sign Up");
                usernameLabel.setVisible(false);
                usernameField.setVisible(false);
                confirmPasswordLabel.setVisible(false);
                confirmPasswordField.setVisible(false);
            }
        });

        // Add Elements to the Form Grid
        formGrid.add(usernameLabel, 0, 0);
        formGrid.add(usernameField, 1, 0);
        formGrid.add(emailLabel, 0, 1);
        formGrid.add(emailField, 1, 1);
        formGrid.add(passwordLabel, 0, 2);
        formGrid.add(passwordField, 1, 2);
        formGrid.add(confirmPasswordLabel, 0, 3);
        formGrid.add(confirmPasswordField, 1, 3);

        // Add Components to the Root
        root.getChildren().addAll(titleLabel, formGrid, errorLabel, submitButton, toggleButton);

        // Scene and Stage
        Scene scene = new Scene(root, 400, 400);
        scene.getStylesheets().add("styles.css"); // Attach CSS
        primaryStage.setTitle("Dark Theme Authentication");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Validate Sign-In Form
    private boolean validateSignInForm(TextField emailField, PasswordField passwordField, Label errorLabel) {
        if (emailField.getText().isEmpty()) {
            errorLabel.setText("email cannot be empty.");
            return false;
        }
        if (passwordField.getText().isEmpty()) {
            errorLabel.setText("Password cannot be empty.");
            return false;
        }
        return true;
    }

    // Validate Sign-Up Form
    private boolean validateSignUpForm(TextField usernameField, TextField emailField, PasswordField passwordField,
            PasswordField confirmPasswordField, Label errorLabel) {
        if (usernameField.getText().isEmpty()) {
            errorLabel.setText("Username cannot be empty.");
            return false;
        }
        if (emailField.getText().isEmpty() || !emailField.getText().matches("[^@]+@[^\\.]+\\..+")) {
            errorLabel.setText("Invalid email address.");
            return false;
        }
        if (passwordField.getText().length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            errorLabel.setText("Passwords do not match.");
            return false;
        }
        return true;
    }

    // Create Payload for API
    private Map<String, String> createPayload(TextField usernameField, TextField emailField,
            PasswordField passwordField) {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", emailField.getText());
        payload.put("password", passwordField.getText());
        if (usernameField != null)
            payload.put("userName", usernameField.getText());
        return payload;
    }

    private void sendRequest(String urlString, Map<String, String> payload, Label errorLabel, Stage primaryStage) {
        try {
            // Convert the payload to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Connect to the server
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send JSON payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            // Handle the response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println(isSignUpMode ? "Sign Up Successful!" : "Sign In Successful!");
                // Read the response body to get user information
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> response = mapper.readValue(
                        conn.getInputStream(),
                        new TypeReference<Map<String, String>>() {
                        });

                System.out.println(response);

                // Get user ID from response
                String userId = response.get("id");
                String userName = response.get("userName");
                // lunch UI
                if (userId != null && userName != null) {
                    Platform.runLater(() -> {
                        primaryStage.close();
                        GlobalChat chatApp = new GlobalChat(userId, userName);
                        Stage chatStage = new Stage();
                        try {
                            chatApp.start(chatStage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } else {
                    errorLabel.setText("Error: Invalid user data received");
                    errorLabel.setVisible(true);
                }
            }
        } catch (Exception e) {
            errorLabel.setText("Error: Unable to connect to the server.");
            errorLabel.setVisible(true);
            e.printStackTrace(); // For debugging purposes
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
