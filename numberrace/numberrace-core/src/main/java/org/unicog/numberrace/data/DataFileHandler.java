//The Number Race: Remediation Software for dyscalculia.
//Copyright (C) Anna Wilson and Stanlislas Dehaene, 2004
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * This class is responsible for handling all game data output. Output from
 * NumCompManager is written into a comma delimited text file. This file is read
 * back in using FileReader, LineNumberReader and StringTokenizer.
 * 
 * The class manages three types of files:
 * 	studentData, which is the output of the data for each student for each session
 * 	studentAlgData, which stores the adaptive algorithm data from the last 20 trials
 *  studentList, which is the "database" of all students
 * 
 * Note: eventually may be better to have buffer so that data is not constantly
 * read/written
 * 
 * Note: need to put in protection for failure to open files, find
 * data, find data in right format etc.
 *  
 */

package org.unicog.numberrace.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Vector;

import org.jmat.data.Matrix;
import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.algorithms.Matrix3D;
import org.unicog.numberrace.algorithms.NewMultiDimAlg;
import org.unicog.numberrace.managers.HazardManager;
import org.unicog.numberrace.setup.GamePreferences;

//TODO: some references needed, e.g. ncm - maybe remove it somehow. Now it's just workaround :)
public class DataFileHandler {

    //This inner class is for saving a 3DMatrix in a separate thread
    class SaveMatrix implements Runnable {
        private Matrix3D matrix;

        public void setMatrix(Matrix3D mtrx) {
            this.matrix = mtrx;
        };

        public void run() {
            File log = new File(getDataDir() + File.separator
                    + currentStudent.getFileHeader() + "_" //$NON-NLS-1$ //$NON-NLS-2$
                    + currentStudent.getSessionNumber() + "_" + "edMatrix.txt"); //$NON-NLS-1$ //$NON-NLS-2$
            FileWriter fileWriter;
            try {
                //open a writer
                //		    fileWriter = new OutputStreamWriter(new BufferedOutputStream(new
                // FileOutputStream(log,false), 32768));
                fileWriter = new FileWriter(log, false);
                fileWriter.write("x,y,z,ed\r\n"); //$NON-NLS-1$
                //			fileWriter.close();
                //			fileWriter = new FileWriter(log, true);
                for (int x = 0; x < NewMultiDimAlg.NUM_AXIS_DIVISIONS[0]; x++) {
                    for (int y = 0; y < NewMultiDimAlg.NUM_AXIS_DIVISIONS[1]; y++) {
                        for (int z = 0; z < NewMultiDimAlg.NUM_AXIS_DIVISIONS[2]; z++) {
                            StringBuffer buffer = new StringBuffer(32);
                            buffer.append(x);
                            buffer.append(',');
                            buffer.append(y);
                            buffer.append(',');
                            buffer.append(z);
                            buffer.append(',');
                            buffer.append(matrix.get(x, y, z));
                            buffer.append("\r\n"); //$NON-NLS-1$
                            //String outString = x + "," + y + "," + z + "," + matrix.get(x,y,z);
                            fileWriter.write(buffer.toString());
                        }
                    }
                }
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Matrix saved"); //$NON-NLS-1$
        }
    };

    SaveMatrix save = new SaveMatrix();

    //files & associated objects
    private File studentDataFile;
    //private FileWriter studentDataFileWriter;
    // private FileReader studentDataFileReader;
    // private LineNumberReader studentDataFileLineReader;
    private File studentAlgDataFile;
    private FileOutputStream studentAlgDataFileOutStream;
    private ObjectOutputStream studentAlgDataObjOutStream;
    private FileInputStream studentAlgDataFileInStream;
    private ObjectInputStream studentAlgDataObjInStream;
    private File allStudentListFile;
    private FileWriter studentListFileWriter;
    private FileReader studentListFileReader;
    private LineNumberReader studentListFileLineReader;
    private DateFormat dateFormatter;
    private DateFormat timeFormatter;

    //data classes
    public Student currentStudent;
    Vector studentListData;

    //constants
    private final int MODEL_DATA = 0;
    private final int ED_MATRIX = 1;

    public DataFileHandler() {
        dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT,
                                                   Locale.FRANCE);
        timeFormatter = DateFormat.getTimeInstance(DateFormat.MEDIUM,
                                                   Locale.FRANCE);
        createAllStudentList();
    }

    private String getDataDir() {
        return GamePreferences.getDataDir();
    }

    /**
     * STUDENT LIST FILE METHODS File AllStudentList keeps a list of students'
     * personal data, and summary data (ie. current session, current mean probs
     * etc.)
     */
    public boolean createAllStudentList() {
        //create the data File, if it doesn't already exist
        allStudentListFile = new File(getDataDir() + File.separator
                + "AllStudentsList.txt"); //$NON-NLS-1$
        try {
            allStudentListFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean loadAllStudentList(Vector studentListData) {
        //set up the readers
        try {
            studentListFileReader = new FileReader(allStudentListFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //this reader is so we can read whole data lines at once
        studentListFileLineReader = new LineNumberReader(studentListFileReader);

        //read a line of the data file
        boolean endOfData = false;
        do {
            Student studentReadFromFile = Student.readAllStudentListLine(studentListFileLineReader);
            if (studentReadFromFile != null) {
                studentListData.add(studentReadFromFile);
            } else
                endOfData = true;
        } while (!endOfData);

        return true;
    }

    public boolean saveAllStudentList(Vector studentListData) {
        //This function saves a fresh version of the WHOLE list, passed in as a Vector of Student
        //objects. Do not use for partial data, because it will wipe the original file!
        try {
            studentListFileWriter = new FileWriter(allStudentListFile); //no append
            //do not append, wipes file to start with blank slate
        } catch (IOException e) {
            e.printStackTrace();
        }

        //write the header
        writeAllStudentListHeader(studentListFileWriter);

        //write the data
        int listLength = studentListData.size();
        Student tmpStudent;
        for (int i = 0; i < listLength; i++) {
            tmpStudent = (Student) studentListData.get(i);
            tmpStudent.setDataLine(i); //add one for header line
            tmpStudent.writeAllStudentListLine(studentListFileWriter);
        }

        //close the file (note: otherwise file will be blank!)
        boolean success = closeAllStudentListFile();

        //destroy the writer
        studentListFileWriter = null;

        return success;
    }

    private boolean replaceAllStudentListLine(Student student) {
        //This function replaces one line in the student list file
        //Right now it sucks - because there is no equivalent writer class to line number reader,
        //so we have to read in all the data into a Vector, and then use saveAllStudentList() to
        //rewrite it - incredibly inefficient! Need to fix...

        //Load up all the data
        Vector studentListData = new Vector();
        loadAllStudentList(studentListData);
        //replace the relevant record
        studentListData.set(student.getDataLine(), student);
        //save all the data
        boolean success = saveAllStudentList(studentListData);
        return success;
    }

    private void writeAllStudentListHeader(FileWriter fileWriter) {
        if (fileWriter != null) {
            try {
                fileWriter.write(Student.getHeaders() + "\r\n"); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean closeAllStudentListFile() {
        if (studentListFileWriter != null) {
            try {
                studentListFileWriter.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (studentListFileReader != null) {
            try {
                studentListFileReader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /*
     * STUDENT DATA FILE METHODS Student data files store the detailed trial by
     * trial data for each session
     * 
     */

    public void createStudentDataFile(Student student) {
        //create the data File
        studentDataFile = new File(getDataDir() + File.separator
                + student.getFileHeader() + "_" //$NON-NLS-1$ //$NON-NLS-2$
                + student.getSessionNumber() + "_Data.txt"); //$NON-NLS-1$
        try {
            studentDataFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeStudentFileDataHeader(Writer writer) throws IOException {
        writer.write("lastName,firstName,session,game,turn,date,time,meanSucc,currDesir,edChosen," //$NON-NLS-1$
                + "DiffSpeed,DiffDist,DiffNotn,cDiffSpeed,cDiffDist,cDiffNotn,ptChosn1," //$NON-NLS-1$
                + "ptChosn2,ptChosn3," //$NON-NLS-1$
                + "anlgMag,verbal,arabic,dotFade,rstrRge,hazard,add,subtrc," //$NON-NLS-1$
                + "fadeSpd,controlFor,leftStim,rightStim," //$NON-NLS-1$
                + "leftSubLeft,leftSubOp,leftSubRight,rightSubLeft,rightSubOp,rightSubRight," //$NON-NLS-1$
                + "respSide,respCorr,finalCorr,RT,relNetGain,p1NetGain,p2NetGain,p1movfor,p2movfor,p1movback,p2movback," //$NON-NLS-1$
                + "p1square,p2square,h_relNetGain,h_p1NetGain,h_p2NetGain,h_p1movfor,h_p2movfor,h_p1movback,h_p2movback," //$NON-NLS-1$
                + HazardManager.getHazardHeadersCommaDelim() + "\r"); //$NON-NLS-1$
    }

    public void writeStudentFileDataLine(GameTurn currentTurn) {
        try {
            Writer writer = new FileWriter(studentDataFile, true);
            if (currentTurn.getTurnNumber() == 1) {
                writeStudentFileDataHeader(writer);
            }
            GameObject go = GameObject.getInstance();
            org.unicog.numberrace.managers.NumCompManager ncm = go.getNumCompManager();
            writer.write(currentStudent.getLastName()
                    + "," //$NON-NLS-1$
                    + currentStudent.getFirstName()
                    + "," //$NON-NLS-1$
                    + currentStudent.getSessionNumber()
                    + "," //$NON-NLS-1$
                    + (ncm.getNumGamesPlayed() + 1)
                    + "," //$NON-NLS-1$
                    + currentTurn.getTurnNumber()
                    + "," //$NON-NLS-1$
                    + dateFormatter.format(currentTurn.getTurnStartTime())
                    + "," //$NON-NLS-1$
                    + timeFormatter.format(currentTurn.getTurnStartTime())
                    + "," //$NON-NLS-1$
                    + currentTurn.getMeanSuccess()
                    + "," //$NON-NLS-1$
                    + currentTurn.getCurrDesiredDiff()
                    + "," //$NON-NLS-1$
                    + currentTurn.getEstimatedDiff()
                    + "," //$NON-NLS-1$
                    + currentTurn.getDiffLvlsCommaDelim()
                    + currentTurn.getConvDiffLvlsCommaDelim()
                    + currentTurn.getCoordsChosenCommaDelim()
                    + ncm.getNotnAttrCommaDelim(currentTurn.getCurrentNotnLevel())
                    + currentTurn.getControlDensityStr()
                    + "," //$NON-NLS-1$
                    + currentTurn.getNumbersCommaDelim()
                    + currentTurn.getSubNumbersAndOpsCommaDelim()
                    + currentTurn.getResponseSideString()
                    + "," //$NON-NLS-1$
                    + currentTurn.getResponseCorrectStr()
                    + "," //$NON-NLS-1$
                    + currentTurn.getFinalCorrectStr()
                    + "," //$NON-NLS-1$
                    + currentTurn.getRTStr()
                    + "," //$NON-NLS-1$
                    + currentTurn.getRelativeGainInfo()
                    + currentTurn.getHypRelativeGainInfo()
                    //					+ game.choiceScreen.boardScreen.hazardManager.getHazardPositionsCommaDelim()
                    + GameObject.getInstance()
                                .getHazardManager()
                                .getHazardPositionsCommaDelim() + "\r"); //$NON-NLS-1$
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * STUDENT ALG DATA FILE METHODS Student data alg files store the matrix
     * objects needed for the adaptive algorithm
     * 
     */

    public void setStudentAlgDataFile(Student student) {
        //set the name of the data file
        studentAlgDataFile = new File(getDataDir() + File.separator
                + student.getFileHeader() //$NON-NLS-1$
                + "_Alg.txt"); //$NON-NLS-1$
        //test to see if the file already exists
        boolean previousDataPresent = studentAlgDataFile.isFile();
        //copy any data that was in file to the algorithm
        if (previousDataPresent) {
            //read data
            Object[] algData = getStudentAlgData();
            //copy data into algorithm
            GameObject.getInstance()
                      .getNumCompManager()
                      .setModelData((Matrix) algData[MODEL_DATA],
                                    (Matrix3D) algData[ED_MATRIX]);
        }
    }

    public void writeStudentAlgDataFile(Matrix modelData, Matrix3D edMatrix) {
        try {
            studentAlgDataFileOutStream = new FileOutputStream(
                    studentAlgDataFile);
            studentAlgDataObjOutStream = new ObjectOutputStream(
                    studentAlgDataFileOutStream);
            studentAlgDataObjOutStream.writeObject(modelData);
            studentAlgDataObjOutStream.writeObject(edMatrix);
            studentAlgDataObjOutStream.flush(); //flushes the buffer
            studentAlgDataFileOutStream.close();
            studentAlgDataObjOutStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object[] getStudentAlgData() {
        Object[] algData = new Object[2];
        try {
            //try to read the new type of objects (from the jar)
            studentAlgDataFileInStream = new FileInputStream(studentAlgDataFile);
            studentAlgDataObjInStream = new ObjectInputStream(
                    studentAlgDataFileInStream);
            algData[MODEL_DATA] = studentAlgDataObjInStream.readObject();
            algData[ED_MATRIX] = studentAlgDataObjInStream.readObject();
            studentAlgDataFileInStream.close();
            studentAlgDataObjInStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return algData;
    }

    public boolean closeStudentAlgDataFile() {
        if (studentAlgDataFileOutStream != null) {
            try {
                studentAlgDataFileOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (studentAlgDataFileInStream != null) {
            try {
                studentAlgDataFileInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (studentAlgDataObjOutStream != null) {
            try {
                studentAlgDataObjOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (studentAlgDataObjInStream != null) {
            try {
                studentAlgDataObjInStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //saves the ed matrix as a comma delimited text file
    public void saveEdMatrix(Matrix3D matrix) {
        save.setMatrix(matrix);
        new Thread(save).start();
    }

    /*
     * GENERAL METHODS Operate on all types of file
     */
    public void setStudent(Student student) {
        exitStudent();
        GameObject.getInstance().getNumCompManager().newStudent(student);
        currentStudent = student;
        currentStudent.augmentSession(); //increment the students session number
        createStudentDataFile(currentStudent);
        setStudentAlgDataFile(currentStudent);
    }

    public boolean exitStudent() {
        //Note: always call this before calling setStudent for the new student
        if (currentStudent != null) {
            if (saveGame() && closeStudentAlgDataFile()) {
                currentStudent = null;
                GameObject.getInstance().getNumCompManager().exitStudent();
                return true;
            } else
                return false;
        } else
            return true;
    }

    public boolean saveGame() {
        if ((currentStudent != null)/* &&(!(Debugger.IN_USE&&game.debugger.debugGameScreens)) */) {
            //replace the old student record in the data file with the new one
            boolean success = replaceAllStudentListLine(currentStudent);
            return success;
        } else
            return true;
    }

    public boolean closeAllDataFiles() {
        if (GameObject.getInstance().getCurrentState() == GameStates.REGISTRATION) {
            //if we are on the registration screen
            //save any changes to the student list
            //TODO: do we really need IT ?            saveAllStudentList(game.regScreen.studentListData);
        }
        if ((currentStudent != null)/* `&&(!(Debugger.IN_USE&&game.debugger.debugGameScreens)) */) {
            boolean filesClosed = false;
            filesClosed = closeAllStudentListFile()
                    && closeStudentAlgDataFile();
            return filesClosed;
        } else
            return true;
    }
}