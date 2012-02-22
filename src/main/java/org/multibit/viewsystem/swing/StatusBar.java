/**
 * Copyright 2011 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.multibit.viewsystem.swing;

/**
 * L2FProd.com Common Components 7.3 License.
 *
 * Copyright 2005-2007 L2FProd.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.multibit.controller.MultiBitController;
import org.multibit.viewsystem.swing.view.components.BlinkLabel;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.MultiBitLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

/**
 * StatusBar. <BR>
 * A status bar is made of multiple zones. A zone can be any JComponent.
 * 
 * In MultiBit the StatusBar is responsible for :
 * 
 * 1) Online/ Connecting status label 2) Status messages - multiple status
 * messages are remembered and can be accessed by the user using a JCombo drop
 * down
 */
public class StatusBar extends JComponent {

    private static final long serialVersionUID = 7824115980324911080L;

    private static final int A_SMALL_NUMBER_OF_PIXELS = 100;
    private static final int A_LARGE_NUMBER_OF_PIXELS = 1000000;
    private static final int STATUSBAR_HEIGHT = 30;

    public static final int ONLINE_LABEL_DELTA = 10;

    private MultiBitLabel onlineLabel;
    private MultiBitLabel statusLabel;
    private boolean isOnline;

    private static final int MINIMUM_STATUS_LABEL_DISPLAY_TIME = 800; // millisecond

    private long lastStatusUpdateTime = 0;

    /**
     * The key used to identified the default zone
     */
    public final static String DEFAULT_ZONE = "default";

    private HashMap<String, Component> idToZones;
    private Border zoneBorder;

    private MultiBitController controller;
    private MultiBitFrame mainFrame;

    /**
     * Construct a new StatusBar
     * 
     */
    public StatusBar(MultiBitController controller, MultiBitFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;

        setLayout(LookAndFeelTweaks.createHorizontalPercentLayout(controller.getLocaliser().getLocale()));
        idToZones = new HashMap<String, Component>();
        setZoneBorder(BorderFactory.createEmptyBorder());
        setBackground(ColorAndFontConstants.BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        setMaximumSize(new Dimension(A_LARGE_NUMBER_OF_PIXELS, STATUSBAR_HEIGHT));
        setMaximumSize(new Dimension(A_SMALL_NUMBER_OF_PIXELS, STATUSBAR_HEIGHT));

        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        final MultiBitController finalController = controller;
        onlineLabel = new MultiBitLabel("", controller);
        onlineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        onlineLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent arg0) {
                int blockHeight = -1;
                if (finalController.getMultiBitService() != null) {
                    if (finalController.getMultiBitService().getChain() != null) {
                        if (finalController.getMultiBitService().getChain().getChainHead() != null) {
                            blockHeight = finalController.getMultiBitService().getChain().getChainHead().getHeight();
                            onlineLabel.setToolTipText(finalController.getLocaliser().getString("multiBitFrame.numberOfBlocks",
                                    new Object[] { "" + blockHeight }));
                        }
                    }
                }
            }
        });

        statusLabel = new MultiBitLabel("", controller);

        int onlineWidth = Math.max(
                getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).stringWidth(
                        controller.getLocaliser().getString("multiBitFrame.onlineText")),
                getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont()).stringWidth(
                        controller.getLocaliser().getString("multiBitFrame.offlineText")))
                + ONLINE_LABEL_DELTA;

        addZone("online", onlineLabel, "" + onlineWidth, "left");
        addZone("network", statusLabel, "*", "");
        addZone("filler2", new JPanel(), "0", "right");
    }

    /**
     * initialise the statusbar;
     */
    public void initialise() {
        updateOnlineStatusText(false);
        updateStatusLabel("");
    }

    /**
     * refresh online status text with existing value
     */
    public void refreshOnlineStatusText() {
        updateOnlineStatusText(isOnline);
    }

    /**
     * update online status text with new value
     * 
     * @param isOnline
     *            True if online, false if offline
     */
    public void updateOnlineStatusText(boolean isOnline) {
        this.isOnline = isOnline;
        final boolean finalIsOnline = isOnline;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String onlineStatus = finalIsOnline ? controller.getLocaliser().getString("multiBitFrame.onlineText") : controller
                        .getLocaliser().getString("multiBitFrame.offlineText");
                if (finalIsOnline) {
                    onlineLabel.setForeground(new Color(0, 100, 0));
                    if (mainFrame != null) {
                        BlinkLabel estimatedBalanceTextLabel = mainFrame.getEstimatedBalanceTextLabel();
                        if (estimatedBalanceTextLabel != null) {
                            estimatedBalanceTextLabel.setBlinkEnabled(true);
                        }
                    }
                } else {
                    onlineLabel.setForeground(new Color(180, 0, 0));
                }
                onlineLabel.setText(onlineStatus);
            }
        });
    }

    public void updateStatusLabel(String newStatusLabel) {
        final String finalNewStatusLabel = newStatusLabel;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Date now = new Date();
                if (now.getTime() - lastStatusUpdateTime < MINIMUM_STATUS_LABEL_DISPLAY_TIME) {
                    try {
                        Thread.sleep(MINIMUM_STATUS_LABEL_DISPLAY_TIME - (now.getTime() - lastStatusUpdateTime));
                    } catch (InterruptedException e) {
                        // no problem if interrupted
                    }
                }
                lastStatusUpdateTime = now.getTime();

                if (statusLabel != null) {
                    statusLabel.setText(finalNewStatusLabel);
                }
            }
        });
    }

    private void setZoneBorder(Border border) {
        zoneBorder = border;
    }

    /**
     * Adds a new zone in the StatusBar
     * 
     * @param id
     * @param zone
     * @param constraints
     *            one of the constraint support by the
     *            com.l2fprod.common.swing.PercentLayout
     */
    private void addZone(String id, Component zone, String constraints, String tweak) {
        // is there already a zone with this id?
        Component previousZone = getZone(id);
        if (previousZone != null) {
            remove(previousZone);
            idToZones.remove(id);
        }

        if (zone instanceof JComponent) {
            JComponent jc = (JComponent) zone;
            if (jc.getBorder() == null || jc.getBorder() instanceof UIResource) {
                if (jc instanceof JLabel) {
                    if ("left".equals(tweak)) {
                        jc.setBorder(new CompoundBorder(new EmptyBorder(0, 3, 0, 2), BorderFactory
                                .createLineBorder(Color.lightGray)));
                    } else {
                        if ("right".equals(tweak)) {
                            jc.setBorder(new CompoundBorder(zoneBorder, new EmptyBorder(0, 2, 0, 1)));
                        } else {
                            jc.setBorder(new CompoundBorder(zoneBorder, new EmptyBorder(0, 2, 0, 2)));
                        }
                    }
                    ((JLabel) jc).setText(" ");
                } else {
                    if (jc instanceof JPanel) {
                        // no border
                    } else {
                        jc.setBorder(zoneBorder);
                    }
                }
            }
        }

        add(zone, constraints);
        idToZones.put(id, zone);
    }

    public Component getZone(String id) {
        return (Component) idToZones.get(id);
    }

    /**
     * For example:
     * 
     * <code>
     *  setZones(new String[]{"A","B"},
     *           new JComponent[]{new JLabel(), new JLabel()},
     *           new String[]{"33%","*"});
     * </code>
     * 
     * would construct a new status bar with two zones (two JLabels) named A and
     * B, the first zone A will occupy 33 percents of the overall size of the
     * status bar and B the left space.
     * 
     * @param ids
     *            a value of type 'String[]'
     * @param zones
     *            a value of type 'JComponent[]'
     * @param constraints
     *            a value of type 'String[]'
     */
    public void setZones(String[] ids, Component[] zones, String[] constraints) {
        removeAll();
        idToZones.clear();
        for (int i = 0, c = zones.length; i < c; i++) {
            addZone(ids[i], zones[i], constraints[i], "");
        }
    }

}

/**
 * PercentLayout. <BR>
 * Constraint based layout which allow the space to be splitted using
 * percentages. The following are allowed when adding components to container:
 * <ul>
 * <li>container.add(component); <br>
 * in this case, the component will be sized to its preferred size
 * <li>container.add(component, "100"); <br>
 * in this case, the component will have a width (or height) of 100
 * <li>container.add(component, "25%"); <br>
 * in this case, the component will have a width (or height) of 25 % of the
 * container width (or height) <br>
 * <li>container.add(component, "*"); <br>
 * in this case, the component will take the remaining space. if several
 * components use the "*" constraint the space will be divided among the
 * components.
 * </ul>
 * 
 * @javabean.class name="PercentLayout"
 *                 shortDescription="A layout supports constraints expressed in percent."
 */
class PercentLayout implements LayoutManager2 {

    /**
     * Useful constant to layout the components horizontally (from top to
     * bottom).
     */
    public final static int HORIZONTAL = 0;

    /**
     * Useful constant to layout the components vertically (from left to right).
     */
    public final static int VERTICAL = 1;

    static class Constraint {
        protected Object value;

        private Constraint(Object value) {
            this.value = value;
        }
    }

    static class NumberConstraint extends Constraint {
        public NumberConstraint(int d) {
            this(Integer.valueOf(d));
        }

        public NumberConstraint(Integer d) {
            super(d);
        }

        public int intValue() {
            return (Integer) value;
        }
    }

    static class PercentConstraint extends Constraint {
        public PercentConstraint(float d) {
            super(d);
        }

        public float floatValue() {
            return (Float) value;
        }
    }

    private final static Constraint REMAINING_SPACE = new Constraint("*");

    private final static Constraint PREFERRED_SIZE = new Constraint("");

    private int orientation;
    private int gap;

    private Locale locale;

    // Consider using HashMap
    private Hashtable<Component, Constraint> m_ComponentToConstraint;

    /**
     * Creates a new HORIZONTAL PercentLayout with a gap of 0.
     */
    public PercentLayout(Locale locale) {
        this(HORIZONTAL, 0, locale);
    }

    public PercentLayout(int orientation, int gap, Locale locale) {
        setOrientation(orientation);
        this.gap = gap;
        this.locale = locale;

        m_ComponentToConstraint = new Hashtable<Component, Constraint>();
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    /**
     * @javabean.property bound="true" preferred="true"
     */
    public int getGap() {
        return gap;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("Orientation must be one of HORIZONTAL or VERTICAL");
        }
        this.orientation = orientation;
    }

    /**
     * @javabean.property bound="true" preferred="true"
     */
    public int getOrientation() {
        return orientation;
    }

    public Constraint getConstraint(Component component) {
        return m_ComponentToConstraint.get(component);
    }

    public void setConstraint(Component component, Object constraints) {
        if (constraints instanceof Constraint) {
            m_ComponentToConstraint.put(component, (Constraint) constraints);
        } else if (constraints instanceof Number) {
            setConstraint(component, new NumberConstraint(((Number) constraints).intValue()));
        } else if ("*".equals(constraints)) {
            setConstraint(component, REMAINING_SPACE);
        } else if ("".equals(constraints)) {
            setConstraint(component, PREFERRED_SIZE);
        } else if (constraints instanceof String) {
            String s = (String) constraints;
            if (s.endsWith("%")) {
                float value = Float.valueOf(s.substring(0, s.length() - 1)) / 100;
                if (value > 1 || value < 0) {
                    throw new IllegalArgumentException("percent value must be >= 0 and <= 100");
                }
                setConstraint(component, new PercentConstraint(value));
            } else {
                setConstraint(component, new NumberConstraint(Integer.valueOf(s)));
            }
        } else if (constraints == null) {
            // null constraint means preferred size
            setConstraint(component, PREFERRED_SIZE);
        } else {
            throw new IllegalArgumentException("Invalid Constraint");
        }
    }

    public void addLayoutComponent(Component component, Object constraints) {
        setConstraint(component, constraints);
    }

    /**
     * Returns the alignment along the x axis. This specifies how the component
     * would like to be aligned relative to other components. The value should
     * be a number between 0 and 1 where 0 represents alignment along the
     * origin, 1 is aligned the furthest away from the origin, 0.5 is centered,
     * etc.
     */
    public float getLayoutAlignmentX(Container target) {
        return 1.0f / 2.0f;
    }

    /**
     * Returns the alignment along the y axis. This specifies how the component
     * would like to be aligned relative to other components. The value should
     * be a number between 0 and 1 where 0 represents alignment along the
     * origin, 1 is aligned the furthest away from the origin, 0.5 is centered,
     * etc.
     */
    public float getLayoutAlignmentY(Container target) {
        return 1.0f / 2.0f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager has cached
     * information it should be discarded.
     */
    public void invalidateLayout(Container target) {
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * 
     * @param name
     *            the component name
     * @param comp
     *            the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * 
     * @param comp
     *            the component ot be removed
     */
    public void removeLayoutComponent(Component comp) {
        m_ComponentToConstraint.remove(comp);
    }

    /**
     * Calculates the minimum size dimensions for the specified panel given the
     * components in the specified parent container.
     * 
     * @param parent
     *            the component to be laid out
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    /**
     * Returns the maximum size of this component.
     * 
     * @see java.awt.Component#getMinimumSize()
     * @see java.awt.Component#getPreferredSize()
     * @see java.awt.LayoutManager
     */
    public Dimension maximumLayoutSize(Container parent) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public Dimension preferredLayoutSize(Container parent) {
        Component[] components = parent.getComponents();
        Insets insets = parent.getInsets();
        int width = 0;
        int height = 0;
        Dimension componentPreferredSize;
        boolean firstVisibleComponent = true;
        for (int i = 0, c = components.length; i < c; i++) {
            if (components[i].isVisible()) {
                componentPreferredSize = components[i].getPreferredSize();
                if (orientation == HORIZONTAL) {
                    height = Math.max(height, componentPreferredSize.height);
                    width += componentPreferredSize.width;
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        width += gap;
                    }
                } else {
                    height += componentPreferredSize.height;
                    width = Math.max(width, componentPreferredSize.width);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        height += gap;
                    }
                }
            }
        }
        return new Dimension(width + insets.right + insets.left, height + insets.top + insets.bottom);
    }

    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        Dimension d = parent.getSize();

        // calculate the available sizes
        d.width = d.width - insets.left - insets.right;
        d.height = d.height - insets.top - insets.bottom;

        // pre-calculate the size of each components
        Component[] components = parent.getComponents();
        int[] sizes = new int[components.length];

        // calculate the available size
        int availableSize = (HORIZONTAL == orientation ? d.width : d.height) - (components.length - 1) * gap;

        // PENDING(fred): the following code iterates 4 times on the component
        // array, need to find something more efficient!

        // give priority to components who want to use their preferred size or
        // who
        // have a predefined size
        for (int i = 0, c = components.length; i < c; i++) {
            if (components[i].isVisible()) {
                Constraint constraint = m_ComponentToConstraint.get(components[i]);
                if (constraint == null || constraint == PREFERRED_SIZE) {
                    sizes[i] = (HORIZONTAL == orientation ? components[i].getPreferredSize().width : components[i]
                            .getPreferredSize().height);
                    availableSize -= sizes[i];
                } else if (constraint instanceof NumberConstraint) {
                    sizes[i] = ((NumberConstraint) constraint).intValue();
                    availableSize -= sizes[i];
                }
            }
        }

        // then work with the components who want a percentage of the remaining
        // space
        int remainingSize = availableSize;
        for (int i = 0, c = components.length; i < c; i++) {
            if (components[i].isVisible()) {
                Constraint constraint = m_ComponentToConstraint.get(components[i]);
                if (constraint instanceof PercentConstraint) {
                    sizes[i] = (int) (remainingSize * ((PercentConstraint) constraint).floatValue());
                    availableSize -= sizes[i];
                }
            }
        }

        // finally share the remaining space between the other components
        ArrayList<Integer> remaining = new ArrayList<Integer>();
        for (int i = 0, c = components.length; i < c; i++) {
            if (components[i].isVisible()) {
                Constraint constraint = m_ComponentToConstraint.get(components[i]);
                if (constraint == REMAINING_SPACE) {
                    remaining.add(i);
                    sizes[i] = 0;
                }
            }
        }

        if (remaining.size() > 0) {
            int rest = availableSize / remaining.size();
            for (Integer aRemaining : remaining) {
                sizes[aRemaining] = rest;
            }
        }

        // all calculations are done, apply the sizes
        int currentOffset = (HORIZONTAL == orientation ? insets.left : insets.top);
        if (!ComponentOrientation.getOrientation(locale).isLeftToRight()) {
            currentOffset = (HORIZONTAL == orientation ? d.width - insets.right : insets.top);
        }

        for (int i = 0, c = components.length; i < c; i++) {
            if (components[i].isVisible()) {
                if (HORIZONTAL == orientation) {
                    if (ComponentOrientation.getOrientation(locale).isLeftToRight()) {
                        components[i].setBounds(currentOffset, insets.top, sizes[i], d.height);
                    } else {
                        components[i].setBounds(currentOffset - sizes[i], insets.top, sizes[i], d.height);
                    }
                } else {
                    components[i].setBounds(insets.left, currentOffset, d.width, sizes[i]);
                }
                if (ComponentOrientation.getOrientation(locale).isLeftToRight()) {
                    currentOffset += gap + sizes[i];
                } else {
                    currentOffset = currentOffset - gap - sizes[i];
                }
            }
        }
    }

}

/**
 * LookAndFeelTweaks. <br>
 * 
 */
class LookAndFeelTweaks {

    private static final Logger log = LoggerFactory.getLogger(LookAndFeelTweaks.class);

    public final static Border PANEL_BORDER = BorderFactory.createEmptyBorder(3, 3, 3, 3);

    // TODO These are never used
    public final static Border WINDOW_BORDER = BorderFactory.createEmptyBorder(4, 10, 10, 10);
    public final static Border EMPTY_BORDER = BorderFactory.createEmptyBorder();

    public static void tweak() {
        Object listFont = UIManager.get("List.font");
        UIManager.put("Table.font", listFont);
        UIManager.put("ToolTip.font", listFont);
        UIManager.put("TextField.font", listFont);
        UIManager.put("FormattedTextField.font", listFont);
        UIManager.put("Viewport.background", "Table.background");
    }

    public static PercentLayout createVerticalPercentLayout(Locale locale) {
        return new PercentLayout(PercentLayout.VERTICAL, 8, locale);
    }

    public static PercentLayout createHorizontalPercentLayout(Locale locale) {
        return new PercentLayout(PercentLayout.HORIZONTAL, 4, locale);
    }

    public static BorderLayout createBorderLayout() {
        return new BorderLayout(8, 8);
    }

    public static void setBorder(JComponent component) {
        if (component instanceof JPanel) {
            component.setBorder(PANEL_BORDER);
        }
    }

    public static void setBorderLayout(Container container) {
        container.setLayout(new BorderLayout(3, 3));
    }

    public static void makeBold(JComponent component) {
        component.setFont(component.getFont().deriveFont(Font.BOLD));
    }

    public static void makeMultilineLabel(JTextComponent area) {
        area.setFont(UIManager.getFont("Label.font"));
        area.setEditable(false);
        area.setOpaque(false);
        if (area instanceof JTextArea) {
            ((JTextArea) area).setWrapStyleWord(true);
            ((JTextArea) area).setLineWrap(true);
        }
    }

    public static void htmlize(JComponent component) {
        htmlize(component, UIManager.getFont("Button.font"));
    }

    public static void htmlize(JComponent component, Font font) {
        String stylesheet = "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: "
                + font.getName() + "; font-size: " + font.getSize() + "pt; }"
                + "a, p, li { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0; font-family: " + font.getName()
                + "; font-size: " + font.getSize() + "pt; }";

        try {
            HTMLDocument doc = null;
            if (component instanceof JEditorPane) {
                if (((JEditorPane) component).getDocument() instanceof HTMLDocument) {
                    doc = (HTMLDocument) ((JEditorPane) component).getDocument();
                }
            } else {
                View v = (View) component.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
                if (v != null && v.getDocument() instanceof HTMLDocument) {
                    doc = (HTMLDocument) v.getDocument();
                }
            }
            if (doc != null) {
                doc.getStyleSheet().loadRules(new java.io.StringReader(stylesheet), null);
            } // end of if (doc != null)
        } catch (Exception e) {
            log.error(e.getMessage(), e);

        }
    }

    public static Border addMargin(Border border) {
        return new CompoundBorder(border, PANEL_BORDER);
    }
}