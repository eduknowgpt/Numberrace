package org.unicog.numberrace;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.threerings.media.ManagedJFrame;

public class MainFrame extends ManagedJFrame implements ControllerProvider {

    private Controller controller;

    public MainFrame(String title, Controller controller) {
        super(title);
        assert controller != null;
        this.controller = controller;
    }

    public void setController(Controller controller) {
        assert controller != null;
        this.controller = controller;
    }

    public Controller getController() {
        return controller;
    }

}
