package org.unicog.numberrace.swing;

import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

public class SafeComboBox extends JComboBox {

    public static final String uiClassID = "SafeComboBoxUI";

    public SafeComboBox() {
        super();
    }

    public SafeComboBox(ComboBoxModel model) {
        super(model);
    }

    public SafeComboBox(Object[] items) {
        super(items);
    }

    public SafeComboBox(Vector<?> items) {
        super(items);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.JTableHeader#getUIClassID()
     */
    public String getUIClassID() {
        return uiClassID;
    }

}
