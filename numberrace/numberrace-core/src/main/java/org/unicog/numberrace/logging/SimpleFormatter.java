package org.unicog.numberrace.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SimpleFormatter extends Formatter {

    Date dat = new Date();
    //  private final static String format = "{0,date} {0,time}";
    private final static String format = "{0,time,HH:mm:ss}";
    private MessageFormat formatter;

    private Object args[] = new Object[1];

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private String lineSeparator;

    public SimpleFormatter() {
        lineSeparator = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction(
                "line.separator"));
    }

    //  private String lineSeparator = System.getProperty("line.separator");

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");
        sb.append(record.getLevel().getLocalizedName());
        sb.append(" [");
        String className = record.getSourceClassName();
        if (className != null) {
            int dotInd = className.lastIndexOf(".") + 1;
            sb.append(className.substring(dotInd));
        } else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append(".");
            sb.append(record.getSourceMethodName());
        }
        sb.append("]#").append(Thread.currentThread().getName()).append(" ");

        String message = formatMessage(record);
        sb.append(message);

        sb.append(lineSeparator);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }

}
