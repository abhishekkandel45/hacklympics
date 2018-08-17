package com.hacklympics.student.exams;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import com.jfoenix.controls.JFXTextField;
import com.hacklympics.api.communication.Response;
import com.hacklympics.api.event.EventManager;
import com.hacklympics.api.event.EventType;
import com.hacklympics.api.event.EventHandler;
import com.hacklympics.api.event.exam.LaunchExamEvent;
import com.hacklympics.api.event.exam.HaltExamEvent;
import com.hacklympics.api.session.Session;
import com.hacklympics.api.material.Exam;
import com.hacklympics.api.user.User;
import com.hacklympics.student.StudentController;
import com.hacklympics.student.code.CodeController;
import com.hacklympics.student.code.logging.KeystrokeLogger;
import com.hacklympics.student.code.logging.ScreenRecorder;
import com.hacklympics.utility.dialog.AlertDialog;
import com.hacklympics.utility.dialog.ConfirmDialog;

public class OngoingExamsController implements Initializable {
    
    private ObservableList<Exam> records;
    private List<Exam> recordsCache;
    
    private String keyword;

    @FXML
    private TableView<Exam> table;
    @FXML
    private TableColumn<Exam, String> examTitleCol;
    @FXML
    private TableColumn<Exam, Integer> examDurationCol;
    @FXML
    private TableColumn<Exam, String> examDescCol;
        
    @FXML
    private JFXTextField keywordField;
    @FXML
    private StackPane dialogPane;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        fetchAndUpdate();
    }
    
    
    private void initTable() {
        records = FXCollections.observableArrayList();
        
        examTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        examDurationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        examDescCol.setCellValueFactory(new PropertyValueFactory<>("desc"));
        
        // Override default empty message of exam table.
        table.setPlaceholder(new Label("No ongoing exams yet."));
        
        // Update the OngoingExams table whenever an exam is launched or halted.
        setOnLaunchExam((LaunchExamEvent e) -> {
            Platform.runLater(() -> {
                fetchAndUpdate();
            });
        });
        
        setOnHaltExam((HaltExamEvent e) -> {
            Platform.runLater(() -> {
                fetchAndUpdate();
            });
        });
    }
    
    private void fetchAndUpdate() {
        keyword = (keyword == null) ? "" : keyword;
        
        records.clear();
        
        recordsCache = Exam.getOngoingExams();
        for (Exam e: recordsCache) {
            if (e.getTitle().contains(keyword) | e.getDesc().contains(keyword)) {
                records.add(e);
            }
        }
        
        table.getItems().clear();
        table.getItems().addAll(records);
    }
    
    private void updateLocally() {
        keyword = (keyword == null) ? "" : keyword;
        
        records.clear();
        
        for (Exam e: recordsCache) {
            if (e.getTitle().contains(keyword) | e.getDesc().contains(keyword)) {
                records.add(e);
            }
        }
        
        table.getItems().clear();
        table.getItems().addAll(records);
    }
    
    @FXML
    public void search(KeyEvent event) {
        keyword = keywordField.getText();
        updateLocally();
    }
    
    /**
     * Invoked when the attend exam button is clicked.
     * Asks the user for confirmation for attending the exam.
     * If the user answers yes, then he/she will be taken to the code page.
     * @param event emitted by JavaFX.
     */
    @FXML
    public void attendExam(ActionEvent event) {
        // If the user is trying to attend an exam, but no exam is selected,
        // block this attempt and alert the user.
        Exam selectedExam = table.getSelectionModel().getSelectedItem();
        
        if (selectedExam == null) {
            AlertDialog alert = new AlertDialog(
                    "Alert",
                    "You have not selected any exam to attend to."
            );
            
            alert.show();
            return;
        }
        
        // If the user is trying to launch an exam, but is already
        // in an exam, block this attempt and alert the user.
        if (Session.getInstance().isInExam()) {
            AlertDialog alert = new AlertDialog(
                    "Alert",
                    "You are already in an exam."
            );
            
            alert.show();
            return;
        }
        
        // If everything alright, then ask the user for confirmation.
        // If yes, then we will proceed.
        ConfirmDialog confirmation = new ConfirmDialog(
                "Taking Exam",
                "You have selected the exam: " + selectedExam + "\n\n"
              + "Take the exam now?"
        );
        
        confirmation.getConfirmBtn().setOnAction((ActionEvent e) -> {
            User currentUser = Session.getInstance().getCurrentUser();
            Response attend = currentUser.attend(selectedExam);
            
            // TODO: Students should not be able to enter the same exam again.
            switch (attend.getStatusCode()) {
                case SUCCESS:
                    // Execute the snapshot and keystroke logging thread.
                    Session.getInstance().getExecutor().execute(ScreenRecorder.getInstance());
                    Session.getInstance().getExecutor().execute(KeystrokeLogger.getInstance());
                    
                    // Setup session data, examLabel and problemBox
                    // to inform user that he/she is taking an exam.
                    Session.getInstance().setCurrentExam(selectedExam);
                    
                    StudentController sc = (StudentController) Session.getInstance().getMainController();
                    CodeController cc = (CodeController) sc.getControllers().get("Code");
                    
                    cc.setExamLabel(selectedExam.getTitle(), selectedExam.getRemainingTime());
                    cc.setProblemBox(selectedExam.getProblems());
                    sc.showCode(e);
                    
                    confirmation.close();
                    break;
                    
                case ALREADY_ATTENDED:
                    AlertDialog alert = new AlertDialog(
                            "Alert",
                            "You cannot take this exam again.\n\n"
                          + "If you have any problem, please contact your teacher."
                    );
                    
                    confirmation.close();
                    alert.show();
                    
                default:
                    break;
            }
        });
        
        confirmation.show();
    }
    
    
    private void setOnLaunchExam(EventHandler<LaunchExamEvent> listener) {
        EventManager.getInstance().addEventHandler(EventType.LAUNCH_EXAM, listener);
    }
    
    private void setOnHaltExam(EventHandler<HaltExamEvent> listener) {
        EventManager.getInstance().addEventHandler(EventType.HALT_EXAM, listener);
    }
    
}