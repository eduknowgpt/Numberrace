package org.unicog.numberrace.swing;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;

public class SafeComboBoxUI extends MetalComboBoxUI {

    public SafeComboBoxUI() {
        super();
    }

    @Override
    protected ComboPopup createPopup() {
        return new SafeComboPopup(comboBox);
    }

    public static ComponentUI createUI(JComponent c) {
        return new SafeComboBoxUI();
    }
}
