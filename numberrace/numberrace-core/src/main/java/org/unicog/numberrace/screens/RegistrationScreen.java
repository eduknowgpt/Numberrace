package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.setup.GamePreferences;
import org.unicog.numberrace.swing.SafeComboBox;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Resources;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.threerings.media.SafeScrollPane;

public class RegistrationScreen extends JPanel implements
        org.unicog.numberrace.screens.Screen, ActionListener,
        ListSelectionListener // ItemListener
{

    public static final boolean[] DEFAULT_ACCESS_PARAMS = { true, false, false,
            false, false, false };

    // Graphics
    public final Color DEFAULT_SCREEN_COLOR = new Color(0, 255, 255); // light blue
    public final Color BUTTON_COLOR = new Color(135, 254, 154); // yellow
    public final Color BUTTON_TEXT_COLOR = Color.BLACK;

    private boolean newStudent = false; // true if user is entering a new student
    private boolean modifying = false; // keeps track of whether user is modifying a record
    //    private boolean modified = false; // keeps track of whether changes have been made. not really used at the moment

    // GUI components
    JLabel titleLabel;
    JLabel lastNameLabel;
    JLabel firstNameLabel;
    JLabel ageLabel;
    JLabel classLabel;
    JLabel sexLabel;
    JLabel startLevelLabel;
    JLabel sessionsPlayedLabel;
    JButton quitButton;
    JButton modifyButton;
    JButton newStudentButton;
    JButton startButton;
    JButton confirmButton;
    JButton cancelButton;
    JButton deleteButton;
    JTextField textFieldLastName;
    JTextField textFieldFirstName;
    JTextField textFieldAge;
    JTextField textFieldClass;
    SafeComboBox cbGender;
    JComboBox cbStartLevel;
    // JComboBox comboBoxClass;
    // JComboBox comboBoxSex;
    JList studentList;
    JScrollPane studentListScrollPane;
    public Vector<Student> studentListData;

    private String versionStr;

    private JButton helpButton;

    private AbstractAction helpAction;

    private SafeScrollPane infoPane = null;

    public RegistrationScreen() {
        super();

        setBackground(DEFAULT_SCREEN_COLOR);

        createGUIComponents();
        initialize();
        //        Utilities.ignoreRepaint(this);
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {

        GridBagConstraints gbcTFStartLevel = new GridBagConstraints();
        gbcTFStartLevel.gridx = 3;
        gbcTFStartLevel.gridy = 7;
        gbcTFStartLevel.fill = GridBagConstraints.HORIZONTAL;
        gbcTFStartLevel.gridwidth = 1;
        gbcTFStartLevel.insets = translateInsets(new Insets(5, 5, 5, 0));
        gbcTFStartLevel.weighty = 1.0;
        gbcTFStartLevel.anchor = GridBagConstraints.NORTHEAST;

        GridBagConstraints gbcTFSex = new GridBagConstraints();
        gbcTFSex.gridy = 6;
        gbcTFSex.gridx = 3;
        gbcTFSex.fill = GridBagConstraints.HORIZONTAL;
        gbcTFSex.gridwidth = 2;
        gbcTFSex.insets = translateInsets(new Insets(5, 5, 5, 75));
        // gbcTFSex.weightx = 0.5;
        gbcTFSex.weighty = 1.0;
        gbcTFSex.anchor = GridBagConstraints.WEST;

        GridBagConstraints gbcTFClass = new GridBagConstraints();
        gbcTFClass.gridy = 5;
        gbcTFClass.gridx = 3;
        gbcTFClass.fill = GridBagConstraints.HORIZONTAL;
        gbcTFClass.gridwidth = 2;
        gbcTFClass.insets = translateInsets(new Insets(5, 5, 5, 75));
        // gbcTFClass.weightx = 0.5;
        gbcTFClass.weighty = 1.0;
        gbcTFClass.anchor = GridBagConstraints.WEST;

        GridBagConstraints gbcTFAge = new GridBagConstraints();
        gbcTFAge.gridy = 4;
        gbcTFAge.gridx = 3;
        gbcTFAge.fill = GridBagConstraints.HORIZONTAL;
        gbcTFAge.gridwidth = 2;
        gbcTFAge.insets = translateInsets(new Insets(5, 5, 5, 75));
        // gbcTFAge.weightx = 0.5;
        gbcTFAge.weighty = 1.0;
        gbcTFAge.anchor = GridBagConstraints.WEST;

        GridBagConstraints gbcTFFirstName = new GridBagConstraints();
        gbcTFFirstName.gridy = 3;
        gbcTFFirstName.gridx = 3;
        gbcTFFirstName.fill = GridBagConstraints.HORIZONTAL;
        gbcTFFirstName.gridwidth = 2;
        gbcTFFirstName.insets = translateInsets(new Insets(5, 5, 5, 75));
        // gbcTFFirstName.weightx = 0.5;
        gbcTFFirstName.weighty = 1.0;
        gbcTFFirstName.anchor = GridBagConstraints.WEST;

        GridBagConstraints gbcTFLastName = new GridBagConstraints();
        gbcTFLastName.anchor = GridBagConstraints.WEST;
        gbcTFLastName.gridy = 2;
        gbcTFLastName.gridx = 3;
        gbcTFLastName.fill = GridBagConstraints.HORIZONTAL;
        gbcTFLastName.gridwidth = 2;
        gbcTFLastName.insets = translateInsets(new Insets(5, 5, 5, 75));
        // gbcTFLastName.weightx = 0.5;
        gbcTFLastName.weighty = 1.0;
        gbcTFLastName.anchor = GridBagConstraints.WEST;

        GridBagConstraints gbcConfirmBtn = new GridBagConstraints();
        gbcConfirmBtn.fill = GridBagConstraints.BOTH;
        gbcConfirmBtn.gridx = 2;
        gbcConfirmBtn.gridy = 8;
        gbcConfirmBtn.gridwidth = 1;
        gbcConfirmBtn.insets = translateInsets(new Insets(15, 0, 10, 25));
        gbcConfirmBtn.weightx = 1.0;
        gbcConfirmBtn.weighty = 1.0;

        GridBagConstraints gbcCancelBtn = new GridBagConstraints();
        gbcCancelBtn.fill = GridBagConstraints.BOTH;
        gbcCancelBtn.gridx = 3;
        gbcCancelBtn.gridy = 8;
        gbcCancelBtn.insets = translateInsets(new Insets(15, 0, 10, 25));
        gbcCancelBtn.weightx = 1.0;
        gbcCancelBtn.weighty = 1.0;

        GridBagConstraints gbcDeleteBtn = new GridBagConstraints();
        gbcDeleteBtn.fill = GridBagConstraints.BOTH;
        gbcDeleteBtn.gridx = 4;
        gbcDeleteBtn.gridy = 8;
        gbcDeleteBtn.insets = translateInsets(new Insets(15, 0, 10, 75));
        gbcDeleteBtn.weightx = 1.0;
        gbcDeleteBtn.weighty = 1.0;

        GridBagConstraints gbcSessionsPlayedLabel = new GridBagConstraints();
        gbcSessionsPlayedLabel.gridx = 0;
        gbcSessionsPlayedLabel.gridy = 9;
        gbcSessionsPlayedLabel.anchor = GridBagConstraints.NORTHWEST;
        gbcSessionsPlayedLabel.gridwidth = 2;
        gbcSessionsPlayedLabel.insets = translateInsets(new Insets(0, 75, 0, 75));
        // gbcLastSessionLabel.weightx = 1.0;
        gbcSessionsPlayedLabel.weighty = 1.0;
        gbcSessionsPlayedLabel.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints gbcStartLevelLabel = new GridBagConstraints();
        gbcStartLevelLabel.gridx = 2;
        gbcStartLevelLabel.gridy = 7;
        gbcStartLevelLabel.anchor = GridBagConstraints.NORTHWEST;
        gbcStartLevelLabel.insets = translateInsets(new Insets(10, 0, 0, 0));
        gbcStartLevelLabel.weighty = 1.0;

        GridBagConstraints gbcSexLabel = new GridBagConstraints();
        gbcSexLabel.gridx = 2;
        gbcSexLabel.gridy = 6;
        gbcSexLabel.anchor = GridBagConstraints.WEST;
        // gbcSexLabel.weightx = 1.0;
        gbcSexLabel.weighty = 1.0;

        GridBagConstraints gbcClassLabel = new GridBagConstraints();
        gbcClassLabel.gridx = 2;
        gbcClassLabel.gridy = 5;
        gbcClassLabel.anchor = GridBagConstraints.WEST;
        // gbcClassLabel.weightx = 1.0;
        gbcClassLabel.weighty = 1.0;

        GridBagConstraints gbcAgeLabel = new GridBagConstraints();
        gbcAgeLabel.gridx = 2;
        gbcAgeLabel.gridy = 4;
        gbcAgeLabel.anchor = GridBagConstraints.WEST;
        // gbcAgeLabel.weightx = 1.0;
        gbcAgeLabel.weighty = 1.0;

        GridBagConstraints gbcFirstNameLabel = new GridBagConstraints();
        gbcFirstNameLabel.gridx = 2;
        gbcFirstNameLabel.gridy = 3;
        gbcFirstNameLabel.anchor = GridBagConstraints.WEST;
        // gbcFirstNameLabel.weightx = 1.0;
        gbcFirstNameLabel.weighty = 1.0;

        GridBagConstraints gbcLastNameLabel = new GridBagConstraints();
        gbcLastNameLabel.gridx = 2;
        gbcLastNameLabel.gridy = 2;
        gbcLastNameLabel.anchor = GridBagConstraints.WEST;
        // gbcLastNameLabel.weightx = 1.0;
        gbcLastNameLabel.weighty = 1.0;

        GridBagConstraints gbcNewStudentBtn = new GridBagConstraints();
        gbcNewStudentBtn.gridx = 0;
        gbcNewStudentBtn.gridy = 10;
        gbcNewStudentBtn.fill = GridBagConstraints.BOTH;
        gbcNewStudentBtn.insets = translateInsets(new Insets(15, 75, 10, 10));
        // gbcNewStudentBtn.weightx = 1.0;
        gbcNewStudentBtn.weighty = 1.0;

        GridBagConstraints gbcModifyBtn = new GridBagConstraints();
        gbcModifyBtn.gridx = 1;
        gbcModifyBtn.gridy = 10;
        gbcModifyBtn.insets = translateInsets(new Insets(15, 10, 10, 50));
        gbcModifyBtn.fill = GridBagConstraints.BOTH;
        gbcModifyBtn.weightx = 1.0;
        gbcModifyBtn.weighty = 1.0;

        GridBagConstraints gbcStartBtn = new GridBagConstraints();
        gbcStartBtn.gridx = 3;
        gbcStartBtn.gridy = 9;
        gbcStartBtn.gridwidth = 2;
        gbcStartBtn.fill = GridBagConstraints.BOTH;
        gbcStartBtn.insets = translateInsets(new Insets(15, 10, 5, 75));
        // gbcNextSessionBtn.weightx = 1.0;
        gbcStartBtn.weighty = 1.0;

        GridBagConstraints gbcQuitBtn = new GridBagConstraints();
        gbcQuitBtn.gridx = 3;
        gbcQuitBtn.gridy = 10;
        gbcQuitBtn.gridwidth = 2;
        gbcQuitBtn.fill = GridBagConstraints.BOTH;
        gbcQuitBtn.insets = translateInsets(new Insets(5, 10, 10, 75));
        // gbcQuitBtn.weightx = 1.0;
        gbcQuitBtn.weighty = 1.0;

        GridBagConstraints gbcStudentsSP = new GridBagConstraints();
        gbcStudentsSP.fill = GridBagConstraints.BOTH;
        gbcStudentsSP.insets = translateInsets(new Insets(0, 75, 0, 50));
        gbcStudentsSP.gridx = 0;
        gbcStudentsSP.gridy = 2;
        gbcStudentsSP.weightx = 1.0;
        gbcStudentsSP.weighty = 1.0;
        gbcStudentsSP.gridwidth = 2;
        gbcStudentsSP.gridheight = 7;

        GridBagConstraints gbcTitleLabel = new GridBagConstraints();
        gbcTitleLabel.gridx = 0;
        gbcTitleLabel.gridy = 0;
        gbcTitleLabel.gridwidth = 5;
        gbcTitleLabel.insets = translateInsets(new Insets(10, 10, 10, 10));
        gbcTitleLabel.weightx = 1.0;
        gbcTitleLabel.weighty = 1.0;

        GridBagConstraints gbcHelpButton = new GridBagConstraints();
        gbcHelpButton.gridx = 0;
        gbcHelpButton.gridy = 0;
        gbcHelpButton.gridwidth = 5;
        gbcHelpButton.insets = translateInsets(new Insets(10, 10, 10, 75));
        gbcHelpButton.weightx = 0;
        gbcHelpButton.weighty = 0;
        gbcHelpButton.anchor = GridBagConstraints.NORTHEAST;

        this.setLayout(new GridBagLayout());
        this.add(titleLabel, gbcTitleLabel);
        this.add(helpButton, gbcHelpButton);
        this.add(studentListScrollPane, gbcStudentsSP);
        this.add(modifyButton, gbcModifyBtn);
        this.add(startButton, gbcStartBtn);
        this.add(lastNameLabel, gbcLastNameLabel);
        this.add(firstNameLabel, gbcFirstNameLabel);
        this.add(ageLabel, gbcAgeLabel);
        this.add(classLabel, gbcClassLabel);
        this.add(sexLabel, gbcSexLabel);
        this.add(startLevelLabel, gbcStartLevelLabel);
        this.add(sessionsPlayedLabel, gbcSessionsPlayedLabel);
        this.add(quitButton, gbcQuitBtn);
        this.add(newStudentButton, gbcNewStudentBtn);
        this.add(confirmButton, gbcConfirmBtn);
        this.add(cancelButton, gbcCancelBtn);
        this.add(deleteButton, gbcDeleteBtn);
        this.add(textFieldLastName, gbcTFLastName);
        this.add(textFieldFirstName, gbcTFFirstName);
        this.add(textFieldAge, gbcTFAge);
        this.add(textFieldClass, gbcTFClass);
        this.add(cbGender, gbcTFSex);
        this.add(cbStartLevel, gbcTFStartLevel);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP,
                                                                                  0,
                                                                                  true),
                                                           "showHelp");
        getActionMap().put("showHelp", getHelpAction());

    }

    public void load() {
        // load up the studentlist file
        studentListData = new Vector<Student>();
        GameObject.getInstance().getDataFileHandler().createAllStudentList();
        loadStudentData();
        versionStr = GamePreferences.getVersionStr();
    }

    public void start() {
        setDataEntryCompntsActive(false);
        setSelectionCompntsActive(true);
        if (studentList.getModel().getSize() > 0) {
            studentList.setSelectedIndex(0);
        } else {
            startButton.setEnabled(false);
            modifyButton.setEnabled(false);
        }
    }

    public void unload() {
        // save the student list data
        infoPane = null;
        GameObject.getInstance()
                  .getDataFileHandler()
                  .saveAllStudentList(studentListData);
    }

    public void valueChanged(ListSelectionEvent e) {
        transferSelectedStudentInfo();
        if (studentList.isSelectionEmpty()) {
            startButton.setEnabled(false);
            modifyButton.setEnabled(false);
        } else {
            startButton.setEnabled(true);
            modifyButton.setEnabled(true);
        }
        newStudent = false;
    }

    public void actionPerformed(ActionEvent e) {
        Object actor = e.getSource();
        if (actor == quitButton) {
            // quit the game
            GameObject.getInstance().changeState(GameStates.END);
        } else if (actor == modifyButton) {
            // activate fields for currently selected student, and enter/delete
            // buttons
            if (studentList.getSelectedValue() != null) {
                modifying = true;
                setDataEntryCompntsActive(true);
                setSelectionCompntsActive(false);
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        JOptionPane.showInternalMessageDialog(RegistrationScreen.this.getParent(),
                                                              Messages.getString("LangVars.REG_NEEDTOSELECTTOMODIFY"));
                    }

                });
            }
        } else if (actor == newStudentButton) {
            // enter a new student
            // clear any list selection
            studentList.clearSelection();
            newStudent = true;
            // activate and clear the text fields
            setDataEntryCompntsActive(true);
            // disable selection of another student on the list
            setSelectionCompntsActive(false);

            cbStartLevel.setSelectedIndex(0);
            deleteButton.setEnabled(false);

        } else if (actor == startButton) {
            // get the selected student
            Student tmpStudent = (Student) studentList.getSelectedValue();
            if (tmpStudent != null) {
                studentListData.set(studentList.getSelectedIndex(), tmpStudent);
                // start the next session with the selected student
                // ie. using the appropriate data file
                GameObject.getInstance().setStudent(tmpStudent);
                GameObject.getInstance().changeState(GameStates.THEME);
                //                GameObject.getInstance().changeState(GameStates.SAVE);
                //                GameObject.getInstance().changeState(GameStates.ZOO);
                //                GameObject.getInstance().changeState(GameStates.INSTRUCTIONS);
                //                game
                //                        .delay(1000,
                //                                "game.changeCurrentScreen(game.themeChoiceScreen)"); //$NON-NLS-1$
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {

                        JOptionPane.showInternalMessageDialog(RegistrationScreen.this.getParent(),
                                                              Messages.getString("LangVars.REG_NEEDTOSELECTTOSTART"));
                    }
                });
            }
        } else if (actor == confirmButton) {
            // First check that all the relevant information is entered
            boolean allInfoEntered = true;
            Utilities.log.info("[" + cbStartLevel.getSelectedItem() + "]");
            allInfoEntered = !("".equals(textFieldLastName.getText())
                    || "".equals(textFieldFirstName.getText())
                    || "".equals(textFieldAge.getText().trim())
                    || "".equals(textFieldClass.getText().trim())
                    || (cbGender.getSelectedIndex() == -1 /* "?" */) || (newStudent && (cbStartLevel.getSelectedIndex() == -1)));

            if (!allInfoEntered) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {

                        JOptionPane.showInternalMessageDialog(RegistrationScreen.this.getParent(),
                                                              Messages.getString("LangVars.REG_NEEDTOENTERALLINFO"));
                    }
                });
            } else {
                // check this student doesn't already exist, if they do, show an
                // error msg and exit
                boolean duplicateStudentExists = false;
                String lastName = textFieldLastName.getText();
                String firstName = textFieldFirstName.getText();
                Student tmpStudent;
                for (int i = 0; i < studentListData.size(); i++) {
                    tmpStudent = studentListData.elementAt(i);
                    if ((tmpStudent.getLastName() == lastName)
                            && (tmpStudent.getFirstName() == firstName)) {
                        duplicateStudentExists = true;
                    }
                }
                if (duplicateStudentExists) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            JOptionPane.showInternalMessageDialog(RegistrationScreen.this.getParent(),
                                                                  Messages.getString("LangVars.REG_STUDENTALREADYEXISTS"));
                        }
                    });
                } else {
                    // if modifying, replace the current student's values with
                    // the entered ones
                    if (modifying) {
                        tmpStudent = (Student) studentList.getSelectedValue();
                        tmpStudent.setLastName(textFieldLastName.getText());
                        tmpStudent.setfirstName(textFieldFirstName.getText());
                        tmpStudent.setAge(Integer.parseInt(textFieldAge.getText()));
                        tmpStudent.setClassLevel(textFieldClass.getText());
                        tmpStudent.setSex(cbGender.getSelectedIndex());
                        // tmpStudent.classLevel =
                        // (String)comboBoxClass.getSelectedItem();
                        // tmpStudent.sex =
                        // (String)comboBoxSex.getSelectedItem();

                        // will happen only for newly created pupil, because after pupil created you can not change start level (control disabled)
                        tmpStudent.setStartLevel(cbStartLevel.getSelectedItem()
                                                             .toString());
                        studentListData.set(studentList.getSelectedIndex(),
                                            tmpStudent);
                    } else { // otherwise, add the name to the data vector
                        // Save the info in a Student data object
                        boolean[][] characAccess = { DEFAULT_ACCESS_PARAMS,
                                DEFAULT_ACCESS_PARAMS };
                        Student newStudent = new Student(
                                studentListData.size() + 1,
                                textFieldLastName.getText(),
                                textFieldFirstName.getText(),
                                Integer.parseInt(textFieldAge.getText()),
                                textFieldClass.getText(),
                                cbGender.getSelectedIndex(),
                                cbStartLevel.getSelectedItem().toString(),
                                0,
                                0,
                                0,
                                characAccess,
                                new byte[ThemeVariables.NUMBER_OF_THEMES][Constants.NUMBER_POSS_REWARDS]);
                        // (String)comboBoxClass.getSelectedItem(),(String)comboBoxSex.getSelectedItem()
                        studentListData.add(newStudent);
                        studentList.setListData(studentListData);
                        studentList.setSelectedValue(newStudent, true);
                    }
                    // save the student list
                    GameObject.getInstance()
                              .getDataFileHandler()
                              .saveAllStudentList(studentListData);
                    // reset the display
                    modifying = true;
                    setDataEntryCompntsActive(false);
                    setSelectionCompntsActive(true);
                    modifying = false;
                }
            }
        } else if (actor == cancelButton) {
            // reset the display
            modifying = false;
            setDataEntryCompntsActive(false);
            setSelectionCompntsActive(true);
            transferSelectedStudentInfo();
        } else if (actor == deleteButton) {
            // check user really does want to delete

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    Object[] options = { Messages.getString("LangVars.REG_NO"),
                            Messages.getString("LangVars.REG_YES") };
                    int choice = JOptionPane.showInternalOptionDialog(RegistrationScreen.this.getParent(),
                                                                      Messages.getString("LangVars.REG_REALLYDELETE"),
                                                                      "", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, //$NON-NLS-1$
                                                                      null,
                                                                      options,
                                                                      options[0]);

                    if (choice == 1) {
                        // remove from the data list
                        studentListData.remove(studentList.getSelectedValue());

                        // save the student list
                        GameObject.getInstance()
                                  .getDataFileHandler()
                                  .saveAllStudentList(studentListData);

                        // reset the display
                        setDataEntryCompntsActive(false);
                        setSelectionCompntsActive(true);
                        modifying = false;
                        studentList.clearSelection();
                    }
                }

            });

        }
    }

    private void setDataEntryCompntsActive(boolean active) {
        // used to activate/deactivate the data entry components on the RH of the screen
        // and to pass the selected student's values into them for modification
        if (!active) {
            // deactivate and clear the text fields
            textFieldLastName.setEditable(false);
            textFieldFirstName.setEditable(false);
            textFieldAge.setEditable(false);
            textFieldClass.setEditable(false);
            cbGender.setEnabled(false);
            cbStartLevel.setEnabled(false);
            // comboBoxClass.setEditable(false);
            // comboBoxSex.setEditable(false);
            confirmButton.setEnabled(false);
            cancelButton.setEnabled(false);
            deleteButton.setEnabled(false);

            if (!modifying) {
                textFieldLastName.setText(""); //$NON-NLS-1$
                textFieldFirstName.setText(""); //$NON-NLS-1$
                textFieldAge.setText(""); //$NON-NLS-1$
                sessionsPlayedLabel.setText(Messages.getString("LangVars.REG_LASTSESSION"));
                textFieldClass.setText(""); //$NON-NLS-1$
                cbGender.setSelectedIndex(-1); //$NON-NLS-1$
                cbStartLevel.setSelectedIndex(-1); //$NON-NLS-1$
            }
            // comboBoxClass.setSelectedIndex(0);
            // comboBoxSex.setSelectedIndex(0);
        } else {
            //            modified = false;
            // activate the text fields and buttons
            textFieldLastName.setEditable(true);
            textFieldFirstName.setEditable(true);
            textFieldAge.setEditable(true);
            textFieldClass.setEditable(true);
            cbGender.setEnabled(true);
            if (newStudent) {
                cbStartLevel.setEnabled(true);
            }
            // comboBoxClass.setEditable(true);
            // comboBoxSex.setEditable(true);
            confirmButton.setEnabled(true);
            cancelButton.setEnabled(true);
            deleteButton.setEnabled(true);
        }
        //        setRendered(false);
    }

    private void setSelectionCompntsActive(boolean active) {
        // used to activate/deactivate the selection list and buttons on the LH side of the screen
        if (!active) {
            // deactivate selection on the list looks like may have to do this with a selection Listener??
            studentList.setEnabled(false);
            quitButton.setEnabled(false);
            modifyButton.setEnabled(false);
            newStudentButton.setEnabled(false);
            startButton.setEnabled(false);
        } else {
            studentList.setEnabled(true);
            quitButton.setEnabled(true);
            modifyButton.setEnabled(true);
            newStudentButton.setEnabled(true);
            startButton.setEnabled(true);
        }
    }

    private void transferSelectedStudentInfo() {
        if ((studentList.getSelectedValue()) != null) { // if a student is selected fill in the information from the selected student
            textFieldLastName.setText(((Student) studentList.getSelectedValue()).getLastName());
            textFieldFirstName.setText(((Student) studentList.getSelectedValue()).getFirstName());
            textFieldAge.setText(Integer.toString(((Student) studentList.getSelectedValue()).getAge()));
            textFieldClass.setText(((Student) studentList.getSelectedValue()).getClassLevel());
            cbGender.setSelectedIndex(((Student) studentList.getSelectedValue()).getSex());
            cbStartLevel.setSelectedItem(((Student) studentList.getSelectedValue()).getStartLevel()); //TODO: log if there is no such level in ComboBox
            sessionsPlayedLabel.setText(Messages.getString("LangVars.REG_LASTSESSION")
                    + ((Student) studentList.getSelectedValue()).getSessionNumber());

        } else {
            // clear the text fields
            textFieldLastName.setText(""); //$NON-NLS-1$
            textFieldFirstName.setText(""); //$NON-NLS-1$
            textFieldAge.setText(""); //$NON-NLS-1$
            textFieldClass.setText(""); //$NON-NLS-1$
            cbGender.setSelectedIndex(-1); //$NON-NLS-1$
            cbStartLevel.setSelectedIndex(-1);
            sessionsPlayedLabel.setText(Messages.getString("LangVars.REG_LASTSESSION"));
        }
    }

    private boolean loadStudentData() {
        GameObject.getInstance()
                  .getDataFileHandler()
                  .loadAllStudentList(studentListData);
        // if student list exists, assign data (this is also done in CreateGUIComponents)
        if (studentList != null) {
            studentList.setListData(studentListData);
        }
        return true;
    }

    private boolean createGUIComponents() {

        titleLabel = new JLabel(
                Messages.getString("LangVars.REG_SELECTASTUDENT"));
        titleLabel.setFont(GameObject.getInstance().getTheme().regScreenTitleFont);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        lastNameLabel = new JLabel(Messages.getString("LangVars.REG_LASTNAME"));
        lastNameLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        lastNameLabel.setHorizontalAlignment(SwingConstants.LEFT);

        firstNameLabel = new JLabel(
                Messages.getString("LangVars.REG_FIRSTNAME"));
        firstNameLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        firstNameLabel.setHorizontalAlignment(SwingConstants.LEFT);

        ageLabel = new JLabel(Messages.getString("LangVars.REG_AGE"));
        ageLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        ageLabel.setHorizontalAlignment(SwingConstants.LEFT);

        classLabel = new JLabel(Messages.getString("LangVars.REG_CLASS"));
        classLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        classLabel.setHorizontalAlignment(SwingConstants.LEFT);

        sexLabel = new JLabel(Messages.getString("LangVars.REG_SEX"));
        sexLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        sexLabel.setHorizontalAlignment(SwingConstants.LEFT);

        startLevelLabel = new JLabel(
                Messages.getString("LangVars.REG_STARTLEVEL"));
        startLevelLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        startLevelLabel.setHorizontalAlignment(SwingConstants.LEFT);

        sessionsPlayedLabel = new JLabel(
                Messages.getString("LangVars.REG_LASTSESSION"));
        sessionsPlayedLabel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        sessionsPlayedLabel.setHorizontalAlignment(SwingConstants.LEFT);

        modifyButton = new JButton(Messages.getString("LangVars.REG_MODIFY"));
        modifyButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        modifyButton.setBackground(new Color(135, 254, 154));
        modifyButton.setForeground(Color.BLACK);

        modifyButton.addActionListener(this);

        startButton = new JButton(Messages.getString("LangVars.REG_START"));
        startButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        startButton.setBackground(BUTTON_COLOR);
        startButton.setForeground(BUTTON_TEXT_COLOR);

        startButton.addActionListener(this);

        quitButton = new JButton(Messages.getString("LangVars.REG_QUIT"));
        quitButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        quitButton.setBackground(BUTTON_COLOR);
        quitButton.setForeground(BUTTON_TEXT_COLOR);

        quitButton.addActionListener(this);

        newStudentButton = new JButton(
                Messages.getString("LangVars.REG_NEWSTUDENT"));
        newStudentButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        newStudentButton.setBackground(BUTTON_COLOR);
        newStudentButton.setForeground(BUTTON_TEXT_COLOR);

        newStudentButton.addActionListener(this);

        confirmButton = new JButton(Messages.getString("LangVars.REG_ENTER"));
        confirmButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        confirmButton.setBackground(BUTTON_COLOR);
        confirmButton.setForeground(BUTTON_TEXT_COLOR);

        confirmButton.addActionListener(this);
        confirmButton.setEnabled(false);

        cancelButton = new JButton(Messages.getString("LangVars.REG_CANCEL"));
        cancelButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        cancelButton.setBackground(BUTTON_COLOR);
        cancelButton.setForeground(BUTTON_TEXT_COLOR);

        cancelButton.addActionListener(this);
        cancelButton.setEnabled(false);

        deleteButton = new JButton(Messages.getString("LangVars.REG_DELETE"));
        deleteButton.setFont(GameObject.getInstance().getTheme().buttonTextFont);
        deleteButton.setBackground(BUTTON_COLOR);
        deleteButton.setForeground(BUTTON_TEXT_COLOR);

        deleteButton.addActionListener(this);
        deleteButton.setEnabled(false);

        textFieldLastName = new JTextField(20);
        textFieldLastName.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        textFieldLastName.setEditable(false);

        textFieldFirstName = new JTextField(20);
        textFieldFirstName.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        textFieldFirstName.setEditable(false);

        textFieldAge = new JTextField(3);
        textFieldAge.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        textFieldAge.setEditable(false);

        textFieldClass = new JTextField(10);
        textFieldClass.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        textFieldClass.addActionListener(this);
        textFieldClass.setEditable(false);

        cbGender = new SafeComboBox(new Object[] { Messages.getString("BOY"),
                Messages.getString("GIRL") });
        cbGender.setFont(GameObject.getInstance().getTheme().regScreenTextFont);

        cbGender.addActionListener(this);
        cbGender.setEditable(false);

        Vector<String> lvls = new Vector<String>();
        String levels = Messages.getString("DIFFICULTY_LEVELS");
        StringTokenizer st = new StringTokenizer(levels, ",");
        for (int l = 1; st.hasMoreTokens(); l++) {
            lvls.add(st.nextToken());
        }

        cbStartLevel = new SafeComboBox(lvls);
        cbStartLevel.setEditable(false);
        cbStartLevel.getUI().setPopupVisible(cbStartLevel, false);
        //        textFieldStartLevel = new JTextField(10);
        cbStartLevel.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        cbStartLevel.addActionListener(this);
        cbStartLevel.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,
                                                          (value == null) ? value
                                                                  : Messages.getString((String) value),
                                                          index,
                                                          isSelected,
                                                          cellHasFocus);
            }
        });

        studentList = new JList();
        studentList.setFont(GameObject.getInstance().getTheme().regScreenTextFont);
        studentList.addListSelectionListener(this);
        studentList.setListData(studentListData);

        studentListScrollPane = new SafeScrollPane();
        studentListScrollPane.setViewportView(studentList);

        helpButton = new JButton(getHelpAction());
        helpButton.setBorderPainted(false);
        helpButton.setContentAreaFilled(false);
        ImageIcon imageIcon = new ImageIcon(
                ImageFactory.getImage(Resources.getString("help_button")));
        helpButton.setIcon(imageIcon);
        helpButton.setPressedIcon(new ImageIcon(
                ImageFactory.getImage(Resources.getString("help_button.pressed"))));
        //        helpButton.setRolloverIcon(new ImageIcon(ImageFactory.getImage(Resources.getString("help_button.over"))));
        //        helpButton.setRolloverEnabled(true);
        helpButton.setFocusable(false);
        helpButton.setPreferredSize(new Dimension(imageIcon.getIconWidth() + 1,
                imageIcon.getIconHeight() + 1));
        return true;
    }

    private Action getHelpAction() {
        if (helpAction == null) {
            helpAction = new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showInternalMessageDialog(RegistrationScreen.this.getParent(),
                                                          getInfoPane(),
                                                          "",
                                                          JOptionPane.INFORMATION_MESSAGE);
                }

            };
        }
        return helpAction;
    }

    protected Object getInfoPane() {
        if (infoPane == null) {
            JTextPane tp = new JTextPane();
            tp.setEditable(false);
            tp.setFocusable(false);
            infoPane = new SafeScrollPane(tp);
            infoPane.setPreferredSize(new Dimension(500, 300));
            URL regInfo = ResourceProvider.getResource(Resources.getString("REG_SCREEN_INFO")); //$NON-NLS-1$

            if (regInfo != null) {
                try {
                	
                	String path = Resources.getString("REG_SCREEN_INFO");

                	// DIAGNÓSTICO: imprime o caminho absoluto e a data de modificação
                	if ("file".equals(regInfo.getProtocol())) {
                	    File arquivo = new File(regInfo.toURI());
                	    System.out.println("📅 Última modificação: " + new java.util.Date(arquivo.lastModified()));
                	    System.out.println("✅ Arquivo existe? " + arquivo.exists());
                	}
                	
                    Reader r = new InputStreamReader(regInfo.openStream(),
                            "UTF-8");
                    tp.read(r, regInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                tp.setText(null);
            }
        }
        return infoPane;
    }

    public Component getComponent() {
        return this;
    }

    public boolean needsPaint() {
        return false; // from AbstractMedia, but this is JPanel we don't need it at least at the begining
    }

    public void tick(long tickStamp) {
    }

    public void stop() {
    }

    public void pause() {
    }

    public void unpause() {
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.addRenderingHints(Utilities.antialiasRH);
        super.paint(g2d);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawString(versionStr, 3, getHeight() - 3);
    }

}
