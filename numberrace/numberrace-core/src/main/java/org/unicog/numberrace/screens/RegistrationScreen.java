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
import org.unicog.numberrace.ui.*;
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
    public final Color DEFAULT_SCREEN_COLOR = new Color(239, 246, 252); // light blue
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
    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private JPanel pages;
    private static final Color INK = new Color(23, 62, 105);
    private static final Color GREEN = new Color(35, 123, 70);

    private final GameBackground gameBackground = new GameBackground();
    private void styleCard(JPanel panel) {
        panel.setBorder(BorderFactory.createEmptyBorder(i(16), i(20), i(20), i(20)));
    }
    private void styleButton(JButton button, Color background, Color foreground, int size) {
        button.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, i(size)));
        button.setBackground(background); button.setForeground(foreground);
        button.setUI(new GameButton()); button.setOpaque(false);
        button.setContentAreaFilled(false); button.setBorderPainted(false);
        String icon = button==startButton?"play":button==newStudentButton?"add":button==modifyButton?"edit":button==confirmButton?"save":button==deleteButton?"delete":button==quitButton?"exit":"cancel";
        button.setIcon(new GameIcon(icon,i(20))); button.setIconTextGap(i(6));
        button.setRolloverEnabled(true); button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(i(8),i(14),i(12),i(14)));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width,i(48)));
    }

    private void initialize() {
        setLayout(new java.awt.BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(i(20), i(24), i(20), i(24)));
        pages = new JPanel(cards);
        pages.setOpaque(false);
        add(pages, java.awt.BorderLayout.CENTER);
        titleLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, i(26)));
        titleLabel.setForeground(Color.WHITE);
        JPanel selection = new GamePanel(new java.awt.BorderLayout(i(12), i(12)));
        styleCard(selection);
        JPanel header = new GameHeader();
        header.setOpaque(false);
        header.add(titleLabel, java.awt.BorderLayout.CENTER);
        header.add(helpButton, java.awt.BorderLayout.EAST);
        selection.add(header, java.awt.BorderLayout.NORTH);
        studentList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        studentList.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, i(18)));
        studentList.setFixedCellHeight(i(54));
        studentList.setSelectionBackground(new Color(255, 239, 164));
        studentList.setBackground(new Color(235, 245, 252));
        studentList.setSelectionForeground(INK);
        studentList.setForeground(INK);
        studentList.setCellRenderer(new PlayerCellRenderer());
        studentList.setOpaque(false);
        studentListScrollPane.setOpaque(false);
        studentListScrollPane.getViewport().setOpaque(false);
        studentListScrollPane.setBorder(BorderFactory.createEmptyBorder());
        studentListScrollPane.setPreferredSize(new Dimension(i(460), i(210)));
        selection.add(studentListScrollPane, java.awt.BorderLayout.CENTER);
        JPanel actions = new JPanel(new java.awt.BorderLayout(i(8), i(8)));
        actions.setOpaque(false);
        styleButton(startButton, new Color(255, 207, 48), INK, 24);
        startButton.setPreferredSize(new Dimension(i(220), i(50)));
        JPanel playRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        playRow.setOpaque(false);
        playRow.add(startButton);
        actions.add(playRow, java.awt.BorderLayout.NORTH);
        JPanel secondary = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, i(8), 0));
        secondary.setOpaque(false);
        for (JButton button : new JButton[] {newStudentButton, modifyButton, quitButton}) {
            styleButton(button, button == newStudentButton ? GREEN : button == modifyButton ? new Color(32, 104, 175) : new Color(218, 236, 250), button == quitButton ? INK : Color.WHITE, 16);
            secondary.add(button);
        }
        actions.add(secondary, java.awt.BorderLayout.SOUTH);
        selection.add(actions, java.awt.BorderLayout.SOUTH);
        JPanel selectionHost = new JPanel(new GridBagLayout());
        selectionHost.setOpaque(false);
        selectionHost.add(selection, new GridBagConstraints());
        pages.add(selectionHost, "selection");

        JPanel editor = new GamePanel(new java.awt.BorderLayout(i(8), i(8)));
        styleCard(editor);
        editorTitle = new JLabel("", SwingConstants.CENTER);
        editorTitle.setFont(titleLabel.getFont());
        editorTitle.setForeground(Color.WHITE);
        JPanel editorHeader = new GameHeader();
        editorHeader.add(editorTitle, java.awt.BorderLayout.CENTER);
        editor.add(editorHeader, java.awt.BorderLayout.NORTH);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JLabel[] labels = {firstNameLabel, lastNameLabel, ageLabel, classLabel, sexLabel, startLevelLabel};
        JComponent[] fields = {textFieldFirstName, textFieldLastName, textFieldAge, textFieldClass, cbGender, cbStartLevel};
        for (int row = 0; row < labels.length; row++) {
            labels[row].setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, i(16)));
            labels[row].setForeground(INK);
            labels[row].setLabelFor(fields[row]);
            fields[row].setFont(labels[row].getFont());
            fields[row].setPreferredSize(new Dimension(i(row == 2 ? 90 : row == 3 ? 150 : 260), i(32)));
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.LINE_START;
            c.insets = new Insets(i(4), i(6), i(4), i(6));
            form.add(labels[row], c);
            GridBagConstraints fieldConstraints = new GridBagConstraints();
            fieldConstraints.gridy = row;
            fieldConstraints.gridx = 1;
            fieldConstraints.anchor = GridBagConstraints.LINE_START;
            fieldConstraints.insets = new Insets(i(4), i(6), i(4), i(6));
            fieldConstraints.fill = GridBagConstraints.NONE;
            form.add(fields[row], fieldConstraints);
        }
        JLabel avatarLabel = new JLabel("Avatar:");
        avatarLabel.setFont(labels[0].getFont()); avatarLabel.setForeground(INK); avatarLabel.setLabelFor(avatarChoice);
        avatarChoice.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList list,Object value,int index,boolean selected,boolean focus) {
                super.getListCellRendererComponent(list,value,index,selected,focus);
                int n = value instanceof Integer ? (Integer)value : -1;
                setIcon(PlayerAvatars.icon(n,i(30))); setText(n<0?"ABC": "Avatar "+(n+1)); return this;
            }
        });
        avatarChoice.setPreferredSize(new Dimension(i(150),i(38)));
        GridBagConstraints gbcAvatarLabel = new GridBagConstraints();
        gbcAvatarLabel.gridx = 0;
        gbcAvatarLabel.gridy = labels.length;
        gbcAvatarLabel.anchor = GridBagConstraints.LINE_START;
        gbcAvatarLabel.insets = new Insets(i(4), i(6), i(4), i(6));
        form.add(avatarLabel, gbcAvatarLabel);

        GridBagConstraints gbcAvatarChoice = new GridBagConstraints();
        gbcAvatarChoice.gridx = 1;
        gbcAvatarChoice.gridy = labels.length;
        gbcAvatarChoice.anchor = GridBagConstraints.LINE_START;
        gbcAvatarChoice.insets = new Insets(i(4), i(6), i(4), i(6));
        form.add(avatarChoice, gbcAvatarChoice);
        GridBagConstraints session = new GridBagConstraints();
        session.gridy = labels.length+1; session.gridwidth = 2; session.anchor = GridBagConstraints.LINE_START;
        form.add(sessionsPlayedLabel, session);
        editor.add(form, java.awt.BorderLayout.CENTER);
        JPanel editActions = new JPanel(new java.awt.BorderLayout(i(8), i(8)));
        editActions.setOpaque(false);
        JPanel saveActions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, i(8), 0));
        saveActions.setOpaque(false);
        styleButton(confirmButton, GREEN, Color.WHITE, 18);
        styleButton(cancelButton, Color.WHITE, INK, 18);
        styleButton(deleteButton, new Color(180, 35, 24), Color.WHITE, 16);
        saveActions.add(confirmButton); saveActions.add(cancelButton);
        editActions.add(saveActions, java.awt.BorderLayout.NORTH);
        JPanel deleteRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        deleteRow.setOpaque(false);
        deleteRow.add(deleteButton);
        editActions.add(deleteRow, java.awt.BorderLayout.SOUTH);
        editor.add(editActions, java.awt.BorderLayout.SOUTH);
        JPanel editorHost = new JPanel(new GridBagLayout());
        editorHost.setOpaque(false);
        editorHost.add(editor, new GridBagConstraints());
        JScrollPane editorScroll = new JScrollPane(editorHost);
        editorScroll.setBorder(null);
        editorScroll.setOpaque(false);
        editorScroll.getViewport().setOpaque(false);
        pages.add(editorScroll, "editor");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0, true), "showHelp");
        getActionMap().put("showHelp", getHelpAction());
    }

    private JLabel editorTitle;
    private final javax.swing.JComboBox<Integer> avatarChoice = new javax.swing.JComboBox<Integer>(new Integer[]{-1,0,1,2,3});

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
            studentList.clearSelection();
            studentList.requestFocusInWindow();
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
        if (e.getValueIsAdjusting()) return;
        transferSelectedStudentInfo();
        if (studentList.isSelectionEmpty()) {
            startButton.setEnabled(false);
            modifyButton.setEnabled(false);
        } else {
            startButton.setEnabled(true);
            modifyButton.setEnabled(true);
        }
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
                newStudent = false;
                modifying = true;
                transferSelectedStudentInfo();
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
            modifying = false;
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
            try {
                if (Integer.parseInt(textFieldAge.getText().trim()) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, Messages.getString("LangVars.REG_INVALIDAGE"));
                textFieldAge.requestFocusInWindow();
                return;
            }
            // First check that all the relevant information is entered
            boolean allInfoEntered = true;
            Utilities.log.info("[" + cbStartLevel.getSelectedItem() + "]");
            allInfoEntered = !("".equals(textFieldLastName.getText().trim())
                    || "".equals(textFieldFirstName.getText().trim())
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
                String lastName = textFieldLastName.getText().trim();
                String firstName = textFieldFirstName.getText().trim();
                Student tmpStudent;
                for (int i = 0; i < studentListData.size(); i++) {
                    tmpStudent = studentListData.elementAt(i);
                    if ((!modifying || tmpStudent != studentList.getSelectedValue())
                            && tmpStudent.getLastName().equalsIgnoreCase(lastName)
                            && tmpStudent.getFirstName().equalsIgnoreCase(firstName)) {
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
                        tmpStudent.setLastName(textFieldLastName.getText().trim());
                        tmpStudent.setfirstName(textFieldFirstName.getText().trim());
                        tmpStudent.setAge(Integer.parseInt(textFieldAge.getText().trim()));
                        tmpStudent.setClassLevel(textFieldClass.getText());
                        tmpStudent.setSex(cbGender.getSelectedIndex());
                        // tmpStudent.classLevel =
                        // (String)comboBoxClass.getSelectedItem();
                        // tmpStudent.sex =
                        // (String)comboBoxSex.getSelectedItem();

                        // will happen only for newly created pupil, because after pupil created you can not change start level (control disabled)
                        // Preserve the initial level of an existing player.
                        studentListData.set(studentList.getSelectedIndex(),
                                            tmpStudent);
                    } else { // otherwise, add the name to the data vector
                        // Save the info in a Student data object
                        boolean[][] characAccess = { DEFAULT_ACCESS_PARAMS.clone(),
                                DEFAULT_ACCESS_PARAMS.clone() };
                        Student newStudent = new Student(
                                studentListData.size() + 1,
                                textFieldLastName.getText().trim(),
                                textFieldFirstName.getText().trim(),
                                Integer.parseInt(textFieldAge.getText().trim()),
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
                    if(studentList.getSelectedValue()!=null) PlayerAvatars.save((Student)studentList.getSelectedValue(), (Integer)avatarChoice.getSelectedItem());
                    // save the student list
                    GameObject.getInstance()
                              .getDataFileHandler()
                              .saveAllStudentList(studentListData);
                    // reset the display
                    modifying = true;
                    setDataEntryCompntsActive(false);
                    setSelectionCompntsActive(true);
                    modifying = false;
                    newStudent = false;
                    studentList.repaint();
                }
            }
        } else if (actor == cancelButton) {
            // reset the display
            modifying = false;
            newStudent = false;
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
                        PlayerAvatars.save((Student)studentList.getSelectedValue(), -1);
                        studentListData.remove(studentList.getSelectedValue());

                        // save the student list
                        GameObject.getInstance()
                                  .getDataFileHandler()
                                  .saveAllStudentList(studentListData);

                        // reset the display
                        setDataEntryCompntsActive(false);
                        setSelectionCompntsActive(true);
                        modifying = false;
                        studentList.setListData(studentListData);
                        studentList.clearSelection();
                        newStudent = false;
                        setSelectionCompntsActive(true);
                    }
                }

            });

        }
    }

    private void setDataEntryCompntsActive(boolean active) {
        deleteButton.setVisible(active && !newStudent);
        sessionsPlayedLabel.setVisible(active && !newStudent && studentList.getSelectedValue() != null && ((Student) studentList.getSelectedValue()).getSessionNumber() > 0);
        if (active) {
            avatarChoice.setSelectedItem(newStudent || studentList.getSelectedValue()==null ? -1 : PlayerAvatars.get((Student)studentList.getSelectedValue()));
            editorTitle.setText(newStudent ? newStudentButton.getText() : modifyButton.getText());
        }
        cards.show(pages, active ? "editor" : "selection");
        if (active) SwingUtilities.invokeLater(() -> textFieldFirstName.requestFocusInWindow());
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
            modifyButton.setEnabled(!studentList.isSelectionEmpty());
            newStudentButton.setEnabled(true);
            startButton.setEnabled(!studentList.isSelectionEmpty());
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
        studentList.setListData(new Vector<Student>());

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
        helpButton.setFocusable(true);
        helpButton.getAccessibleContext().setAccessibleName("Help");
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
        gameBackground.paint(g, getWidth(), getHeight());
        if (versionStr != null) g.drawString(versionStr, 3, getHeight() - 3);
    }

}
