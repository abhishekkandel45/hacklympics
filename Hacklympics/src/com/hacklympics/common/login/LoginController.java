package com.hacklympics.common.login;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXSpinner;
import com.hacklympics.api.communication.Response;
import com.hacklympics.api.communication.StatusCode;
import com.hacklympics.api.communication.SocketServer;
import com.hacklympics.api.session.Session;
import com.hacklympics.api.user.User;
import com.hacklympics.utility.FXMLTable;
import com.hacklympics.utility.Utils;
import com.hacklympics.api.user.Student;
import com.hacklympics.api.user.Teacher;

public class LoginController {
    
    private User user;
    
    @FXML
    private Label warningMsg;
    @FXML
    private JFXTextField usernameField;
    @FXML
    private JFXPasswordField passwordField;
    @FXML
    private JFXButton loginBtn;
    @FXML
    private JFXButton exitBtn;
    @FXML
    private JFXSpinner spinner;
    
    
    @FXML
    public void login(ActionEvent e) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        try {
            Response login = User.login(username, password);
            
            if (login.success()) {
                // Note that the SocketServer will only start after the user has
                // SUCCESSFULLY logged in.
                Session.getInstance().getExecutor().execute(SocketServer.getInstance());
                
                String role = login.getContent().get("role").toString();
                switch (role) {
                    case "student":
                        user = new Student(username);
                        Session.getInstance().setCurrentUser(user);
                        
                        String studentFXML = FXMLTable.getInstance().get("Student");
                        Utils.showUserStage(new FXMLLoader(getClass().getResource(studentFXML)));
                        
                        loginBtn.getScene().getWindow().hide();
                        break;
                        
                    case "teacher":
                        user = new Teacher(username);
                        Session.getInstance().setCurrentUser(user);
                        
                        String teacherFXML = FXMLTable.getInstance().get("Teacher");
                        Utils.showUserStage(new FXMLLoader(getClass().getResource(teacherFXML)));
                        
                        loginBtn.getScene().getWindow().hide();
                        break;
                        
                    default:
                        break;
                }
                // TODO: Shutdown eventListener upon abnormal client shutdown.
            } else {
                StatusCode statusCode = login.getStatusCode();
                
                switch(statusCode) {
                    case VALIDATION_ERR:
                    case NOT_REGISTERED:
                        warningMsg.setText("Incorrect username or password.");
                        break;
                    
                    case ALREADY_LOGGED_IN:
                        warningMsg.setText("This account is currently in use.");
                        break;
                    
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            warningMsg.setText("Unable to connect to the server.");
            ex.printStackTrace();
        }
    }
    
    
    @FXML
    public void register(ActionEvent e) {
        String registerFXML = FXMLTable.getInstance().get("Register");
        Utils.showStage(new FXMLLoader(getClass().getResource(registerFXML)));
        
        loginBtn.getScene().getWindow().hide();
    }
    
    @FXML
    public void exit(ActionEvent e) {
        System.exit(0);
    }
    
    @FXML
    public void clearWarningMsg(KeyEvent e) {
        warningMsg.setText("");
    }
    
}