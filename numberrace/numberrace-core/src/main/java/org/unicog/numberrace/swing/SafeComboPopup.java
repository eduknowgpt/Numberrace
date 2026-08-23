package org.unicog.numberrace.swing;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicComboPopup;

import com.threerings.media.SafeScrollPane;

public class SafeComboPopup extends BasicComboPopup {

    public SafeComboPopup(JComboBox combo) {
        super(combo);
    }

    @Override
    protected JScrollPane createScroller() {
        JScrollPane sp = new SafeScrollPane(list);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setHorizontalScrollBar(null);
        return sp;
    }
}
