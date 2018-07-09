package hacklympics.student.pages;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.Event;
import javafx.event.ActionEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import com.jfoenix.controls.JFXComboBox;
import com.kodedu.terminalfx.TerminalTab;
import com.kodedu.terminalfx.TerminalBuilder;
import com.kodedu.terminalfx.config.TerminalConfig;
import com.hacklympics.api.communication.Response;
import com.hacklympics.api.material.Answer;
import com.hacklympics.api.material.Exam;
import com.hacklympics.api.material.Problem;
import com.hacklympics.api.session.Session;
import com.hacklympics.api.user.Student;
import com.hacklympics.api.user.User;
import hacklympics.student.StudentController;
import hacklympics.utility.FileTab;
import hacklympics.utility.AlertDialog;
import hacklympics.utility.ConfirmDialog;
import hacklympics.utility.Utils;

public class CodeController implements Initializable {

    private TerminalConfig terminalConfig;
    private TerminalBuilder terminalBuilder;
    private TerminalTab terminal;
    
    private Timeline timeline;
    private int remainingTime;

    @FXML
    private TabPane fileTabPane;
    @FXML
    private TabPane terminalTabPane;
    @FXML
    private StackPane dialogPane;

    @FXML
    private Label filepathLabel;
    @FXML
    private Label examLabel;
    @FXML
    private JFXComboBox problemBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        terminalConfig = new TerminalConfig();
        terminalConfig.setBackgroundColor("#eff1f5");

        terminalBuilder = new TerminalBuilder(terminalConfig);
        terminal = terminalBuilder.newTerminal();
        terminal.getStyleClass().add("minimal-tab");
        terminalTabPane.getTabs().add(terminal);

        // Update filepath label whenever the selected tab is changed.
        fileTabPane.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Tab> ov, Tab ot, Tab nt) -> {
                    updateFilepathLabel();
                }
        );

        // Close the terminal whenever user clicks on the code area.
        fileTabPane.setOnMouseClicked((Event event) -> {
            closeTerminal();
        });

        createFileTab();
    }

    @FXML
    public void newFile(ActionEvent events) {
        createFileTab();
    }

    @FXML
    public void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File...");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Java Source Code", "*.java"),
                new ExtensionFilter("All Files", "*.*")
        );

        File selected = fileChooser.showOpenDialog(fileTabPane.getScene().getWindow());

        // If the user selects nothing (e.g., clicking the cancel button),
        // return immediately.
        if (selected == null) {
            return;
        }

        // Try to open the file in a new tab.
        try {
            createFileTab().open(selected);
        } catch (IOException ioe) {
            fileTabPane.getTabs().remove(getCurrentFileTab());

            AlertDialog alert = new AlertDialog(
                    dialogPane,
                    "Error",
                    "Unable to open the specified file."
            );

            alert.show();
        }
    }

    @FXML
    public void saveFile(ActionEvent event) {
        // If the current file is still an untitled file,
        // prompt the user to save it as a new file instead.
        if (getCurrentFileTab().getFile() == null) {
            saveFileAs(event);
            return;
        }

        // The code below will do the effective task of saving the file.
        try {
            getCurrentFileTab().save();
        } catch (IOException ioe) {
            AlertDialog alert = new AlertDialog(
                    dialogPane,
                    "Error",
                    "Unable to write to the specified file."
            );

            alert.show();
        }
    }

    @FXML
    public void saveFileAs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Java Source Code", "*.java"),
                new ExtensionFilter("All Files", "*.*")
        );

        File selected = fileChooser.showSaveDialog(fileTabPane.getScene().getWindow());

        getCurrentFileTab().setFile(selected);
        saveFile(event);
    }

    @FXML
    public void closeFile(ActionEvent event) {
        // If the current file does not have any unsaved change,
        // close the tab directly.
        if (!getCurrentFileTab().unsaved()) {
            fileTabPane.getTabs().remove(getCurrentFileTab());
            return;
        }

        // Otherwise, show a ConfirmDialog to the user.
        ConfirmDialog confirm = new ConfirmDialog(
                dialogPane,
                "Unsaved Changes",
                "Do you want to close it without saving?"
        );

        confirm.getConfirmBtn().setOnAction((ActionEvent e) -> {
            fileTabPane.getTabs().remove(getCurrentFileTab());
            confirm.close();
        });

        confirm.show();
    }

    @FXML
    public void toggleTerminal(ActionEvent event) {
        if (terminalTabPane.isVisible()) {
            closeTerminal();
        } else {
            showTerminal();
        }
    }

    @FXML
    public void compile(ActionEvent event) throws IOException {
        // If current file is still unsaved, save it first.
        if (getCurrentFileTab().unsaved()) {
            saveFile(event);
        }

        showTerminal();

        String location = getCurrentFileTab().getLocation();
        String filepath = getCurrentFileTab().getFilepath();

        terminal.onTerminalFxReady(() -> {
            terminal.command(String.join(" ", "javac", "-cp", location, filepath, "\r"));
        });
    }

    @FXML
    public void execute(ActionEvent event) {
        showTerminal();

        String location = getCurrentFileTab().getLocation();
        String className = getCurrentFileTab().getFilename().split("[.]")[0];

        terminal.onTerminalFxReady(() -> {
            terminal.command(String.join(" ", "java", "-cp", location, className, "\r"));
        });
    }

    @FXML
    public void showHint(ActionEvent e) {
        Problem selectedProblem = (Problem) problemBox.getSelectionModel().getSelectedItem();
        if (selectedProblem == null) {
            return;
        }

        AlertDialog hint = new AlertDialog(
                dialogPane,
                selectedProblem.getTitle(),
                selectedProblem.getDesc()
        );

        hint.show();
    }

    @FXML
    public void submit(ActionEvent event) {
        Student student = (Student) Session.getInstance().getCurrentUser();
        Exam selectedExam = Session.getInstance().getCurrentExam();
        Problem selectedProblem = (Problem) problemBox.getSelectionModel().getSelectedItem();

        if (selectedExam == null | selectedProblem == null) {
            AlertDialog alert = new AlertDialog(
                    dialogPane,
                    "Tips",
                    "Please make sure both Exam and Problem are selected."
            );

            alert.show();
            return;
        }

        ConfirmDialog confirm = new ConfirmDialog(
                dialogPane,
                "Submit Answer",
                String.format(
                        "Submitting \"%s\" for \"%s\".\n\n"
                      + "Once submitted, you will NOT be able to revise it.\n"
                      + "Submit your code now?",
                        getCurrentFileTab().getFilename(),
                        selectedProblem
                )
        );

        confirm.getConfirmBtn().setOnAction((ActionEvent e) -> {
            Response create = Answer.create(
                    selectedExam.getCourseID(),
                    selectedExam.getExamID(),
                    selectedProblem.getProblemID(),
                    getCurrentFileTab().getFilename(),
                    getCurrentFileTab().getCodeArea().getText(),
                    student.getUsername()
            );

            confirm.close();

            switch (create.getStatusCode()) {
                case SUCCESS:
                    Answer answer = new Answer(
                            selectedExam.getCourseID(),
                            selectedExam.getExamID(),
                            selectedProblem.getProblemID(),
                            (int) Double.parseDouble(create.getContent().get("id").toString())
                    );

                    validate(answer);
                    break;

                case ALREADY_SUBMITTED:
                    AlertDialog submitted = new AlertDialog(
                            dialogPane,
                            "Tips",
                            "You've already submitted your code for this problem."
                    );

                    submitted.show();
                    break;

                default:
                    AlertDialog error = new AlertDialog(
                            dialogPane,
                            "Submission Error",
                            "ErrorCode: " + create.getStatusCode().toString()
                    );

                    error.show();
                    break;
            }
        });

        confirm.show();
    }

    private void validate(Answer answer) {
        Response validate = answer.validate();

        switch (validate.getStatusCode()) {
            case SUCCESS:
                AlertDialog correct = new AlertDialog(
                        dialogPane,
                        "Congratulations",
                        "Your code works correctly. Nice work!"
                );

                correct.show();
                break;

            case INCORRECT_ANSWER:
                AlertDialog failed = new AlertDialog(
                        dialogPane,
                        "Sorry",
                        "It seems that your code doesn't work out,\n\n"
                      + "keep going!"
                );

                failed.show();
                break;

            default:
                AlertDialog error = new AlertDialog(
                        dialogPane,
                        "Validation Error",
                        "ErrorCode: " + validate.getStatusCode().toString()
                );

                error.show();
                break;
        }
    }

    @FXML
    public void leave(ActionEvent event) {
        Exam currentExam = Session.getInstance().getCurrentExam();
        User currentUser = Session.getInstance().getCurrentUser();

        ConfirmDialog confirmation = new ConfirmDialog(
                dialogPane,
                "Leave Exam",
                "Once left, you will not be able to enter again!\n\n"
              + "Leave the exam now?"
        );

        confirmation.getConfirmBtn().setOnAction((ActionEvent e) -> {
            Response leave = currentUser.leave(currentExam);
            
            switch (leave.getStatusCode()) {
                case SUCCESS:
                    Session.getInstance().setCurrentExam(null);

                    StudentController sc = (StudentController) Session.getInstance().getMainController();
                    CodeController cc = (CodeController) sc.getControllers().get("Code");

                    cc.setExamLabel("No Exam Selected");
                    cc.setProblemBox(null);
                    sc.showOngoingExams(event);
                    break;

                default:
                    break;
            }
            
            confirmation.close();
        });

        confirmation.show();
    }

    
    /**
     * Creates a new tab in FileTabPane.
     *
     * @return the newly created FileTab.
     */
    private FileTab createFileTab() {
        // Create a new tab "c", add it to our fileTabPane
        // and select (switch to) that tab.
        FileTab c = new FileTab();
        fileTabPane.getTabs().add(c);
        fileTabPane.getSelectionModel().select(c);

        // Overrides the default behavior of the close button on each tab by
        // consuming the original event, and then call my own closing method.
        c.setOnCloseRequest((Event event) -> {
            // Buggy here.
            event.consume();
            closeFile(null);
        });

        return c;
    }

    /**
     * Gets the currently selected tab in FileTabPane.
     *
     * @return the currently selected tab.
     */
    private FileTab getCurrentFileTab() {
        return ((FileTab) fileTabPane.getSelectionModel().getSelectedItem());
    }

    /**
     * Updates the filepath label on the bottom left.
     */
    private void updateFilepathLabel() {
        FileTab current = getCurrentFileTab();
        filepathLabel.setText((current == null) ? "" : current.getFilepath());
    }

    /**
     * Shows the terminal panel.
     */
    private void showTerminal() {
        if (terminalTabPane.isVisible()) {
            return;
        }

        terminalTabPane.setVisible(true);
        terminalTabPane.setMouseTransparent(false);
    }

    /**
     * Closes the terminal panel.
     */
    private void closeTerminal() {
        if (!terminalTabPane.isVisible()) {
            return;
        }

        terminalTabPane.setVisible(false);
        terminalTabPane.setMouseTransparent(true);
    }
    
    /**
     * Sets the exam label which shows the title of currently ongoing exam.
     * @param examTitle title of exam.
     */
    public void setExamLabel(String examTitle) {
        examLabel.setText(examTitle);
    }
    
    /**
     * Sets the exam label which shows the title of currently ongoing exam,
     * and shows the remaining time of the exam as well.
     * @param examTitle name of exam.
     * @param remainingTime remaining time of exam.
     */
    public void setExamLabel(String examTitle, int remainingTime) {
        examLabel.setText(String.format("%s (%s)", examTitle, Utils.formatTime(remainingTime)));
        
        this.remainingTime = remainingTime;
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateExamLabelTimer(examTitle)));
        this.timeline.setCycleCount(Timeline.INDEFINITE);
        this.timeline.playFromStart();
    }
    
    /**
     * Updates the remaining time in the exam label.
     */
    private void updateExamLabelTimer(String examTitle) {
        if (this.remainingTime > 0) {
            this.remainingTime--;
        }
        
        examLabel.setText(String.format("%s (%s)", examTitle, Utils.formatTime(this.remainingTime)));
    }

    /**
     * Adds all problems of a exam to the problem ComboBox.
     *
     * @param problems all problems of currently ongoing exams.
     */
    public void setProblemBox(List<Problem> problems) {
        problemBox.getItems().clear();

        if (problems != null) {
            problemBox.getItems().addAll(problems);
        }
    }

}
