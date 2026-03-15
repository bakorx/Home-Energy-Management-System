package hems.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DashboardGUI extends JFrame {

    // Consumption panel
    private DefaultTableModel applianceTableModel;
    private JLabel totalWattsLabel, loadPctLabel;
    private JProgressBar loadBar;

    // Scheduler panel
    private JLabel tariffBadge;
    private DefaultListModel<String> deferredListModel;

    // Grid panel
    private final Map<String, JLabel> gridBulbs = new LinkedHashMap<>();
    private JLabel outageLabel;

    // Alert panel
    private JTextArea alertArea;
    private JLabel alertCountLabel, overloadCountLabel, reportCountLabel;

    // Log panel
    private JTextArea logArea;

    // Controls panel
    private JSlider speedSlider;
    private JLabel speedLabel;
    private final Map<String, JToggleButton> applianceButtons = new LinkedHashMap<>();

    // Callbacks to agents — set by ConsumptionMonitorAgent after startup
    private Consumer<String[]> onApplianceToggle;   // appliance name, "ON"/"OFF"
    private Runnable onOutageTriggered;
    private Runnable onReset;
    private Consumer<Integer> onSpeedChanged;        // interval in ms

    public DashboardGUI() {
        setTitle("HEMS — Home Energy Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setLayout(new BorderLayout(6, 6));
        getContentPane().setBackground(new Color(245, 245, 248));

        add(buildTitleBar(),   BorderLayout.NORTH);
        add(buildMainGrid(),   BorderLayout.CENTER);
        add(buildBottomArea(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Agent callback registration ──

    public void setOnApplianceToggle(Consumer<String[]> cb)  { this.onApplianceToggle = cb; }
    public void setOnOutageTriggered(Runnable cb)            { this.onOutageTriggered = cb; }
    public void setOnReset(Runnable cb)                      { this.onReset = cb; }
    public void setOnSpeedChanged(Consumer<Integer> cb)      { this.onSpeedChanged = cb; }

    // ── Build UI ──

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(40, 50, 100));
        bar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Home Energy Management System  |  DCIT 403 Agent Simulation");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        JButton outageBtn = controlButton("Simulate Outage", new Color(200, 60, 40));
        outageBtn.addActionListener(e -> {
            if (onOutageTriggered != null) onOutageTriggered.run();
        });

        JButton resetBtn = controlButton("Reset Simulation", new Color(80, 80, 160));
        resetBtn.addActionListener(e -> {
            if (onReset != null) onReset.run();
        });

        btns.add(outageBtn);
        btns.add(resetBtn);
        bar.add(btns, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMainGrid() {
        JPanel main = new JPanel(new GridLayout(2, 2, 8, 8));
        main.setBorder(new EmptyBorder(8, 8, 4, 8));
        main.setBackground(new Color(245, 245, 248));
        main.add(buildConsumptionPanel());
        main.add(buildGridPanel());
        main.add(buildSchedulerPanel());
        main.add(buildAlertPanel());
        return main;
    }

    private JPanel buildBottomArea() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBackground(new Color(245, 245, 248));
        bottom.setBorder(new EmptyBorder(0, 8, 8, 8));
        bottom.add(buildControlsPanel(), BorderLayout.NORTH);
        bottom.add(buildLogPanel(),      BorderLayout.CENTER);
        return bottom;
    }

    private JPanel buildConsumptionPanel() {
        JPanel p = card("Consumption Monitor Agent");
        p.setLayout(new BorderLayout(4, 4));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        totalWattsLabel = badge("0 W",  new Color(60, 80, 180));
        loadPctLabel    = badge("0%",   new Color(40, 130, 80));
        top.add(new JLabel("Total load:"));
        top.add(totalWattsLabel);
        top.add(new JLabel("  Capacity used:"));
        top.add(loadPctLabel);
        p.add(top, BorderLayout.NORTH);

        loadBar = new JProgressBar(0, 100);
        loadBar.setStringPainted(true);
        loadBar.setString("Load: 0%");
        loadBar.setForeground(new Color(40, 160, 80));
        loadBar.setPreferredSize(new Dimension(0, 22));
        p.add(loadBar, BorderLayout.CENTER);

        String[] cols = {"Appliance", "Watts", "Status"};
        applianceTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(applianceTableModel);
        table.setRowHeight(22);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(0, 180));
        p.add(sp, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildGridPanel() {
        JPanel p = card("Grid Agent");
        p.setLayout(new BorderLayout(4, 4));

        outageLabel = new JLabel("  Grid: NORMAL");
        outageLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        outageLabel.setForeground(new Color(40, 130, 80));
        outageLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        p.add(outageLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));
        grid.setOpaque(false);
        for (String name : Arrays.asList(
                "AC","WaterHeater","WashingMachine","Oven","TV","Fridge","Lights")) {
            JLabel lbl = new JLabel(name, SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(new Color(60, 180, 100));
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 210), 1, true),
                new EmptyBorder(8, 4, 8, 4)));
            gridBulbs.put(name, lbl);
            grid.add(lbl);
        }
        p.add(new JScrollPane(grid), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSchedulerPanel() {
        JPanel p = card("Scheduler Agent");
        p.setLayout(new BorderLayout(4, 4));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        tariffBadge = badge("OFF-PEAK", new Color(40, 130, 80));
        top.add(new JLabel("Tariff period:"));
        top.add(tariffBadge);
        p.add(top, BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(4, 4));
        mid.setOpaque(false);
        mid.setBorder(new EmptyBorder(4, 4, 4, 4));
        JLabel lbl = new JLabel("Deferred appliances:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        mid.add(lbl, BorderLayout.NORTH);
        deferredListModel = new DefaultListModel<>();
        JList<String> deferredList = new JList<>(deferredListModel);
        deferredList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        deferredList.setBackground(new Color(255, 250, 235));
        deferredList.setCellRenderer(new DeferredCellRenderer());
        mid.add(new JScrollPane(deferredList), BorderLayout.CENTER);
        p.add(mid, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAlertPanel() {
        JPanel p = card("Alert Agent");
        p.setLayout(new BorderLayout(4, 4));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        stats.setOpaque(false);
        alertCountLabel    = badge("0 total",     new Color(80, 80, 180));
        overloadCountLabel = badge("0 overloads", new Color(200, 80, 40));
        reportCountLabel   = badge("0 reports",   new Color(40, 130, 80));
        stats.add(alertCountLabel);
        stats.add(overloadCountLabel);
        stats.add(reportCountLabel);
        p.add(stats, BorderLayout.NORTH);

        alertArea = new JTextArea();
        alertArea.setEditable(false);
        alertArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        alertArea.setLineWrap(true);
        alertArea.setBackground(new Color(252, 248, 240));
        p.add(new JScrollPane(alertArea), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildControlsPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setBackground(new Color(238, 240, 255));
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(180, 185, 230), 1, true),
            new EmptyBorder(8, 10, 8, 10)));

        JLabel heading = new JLabel("Manual Controls  —  toggle any appliance or adjust simulation speed");
        heading.setFont(new Font("SansSerif", Font.BOLD, 12));
        heading.setForeground(new Color(40, 40, 100));
        p.add(heading, BorderLayout.NORTH);

        // Appliance toggle buttons
        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toggles.setOpaque(false);
        for (String name : Arrays.asList(
                "AC","WaterHeater","WashingMachine","Oven","TV","Fridge","Lights")) {
            JToggleButton btn = new JToggleButton(name, true);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.setBackground(new Color(60, 180, 100));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new EmptyBorder(4, 8, 4, 8));
            btn.setOpaque(true);
            btn.addActionListener(e -> {
                boolean on = btn.isSelected();
                btn.setBackground(on ? new Color(60, 180, 100) : new Color(200, 70, 60));
                btn.setText(name);
                if (onApplianceToggle != null)
                    onApplianceToggle.accept(new String[]{name, on ? "ON" : "OFF"});
            });
            applianceButtons.put(name, btn);
            toggles.add(btn);
        }

        // Speed slider
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        speedPanel.setOpaque(false);
        speedLabel = new JLabel("Speed: Normal (3s)");
        speedLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        speedLabel.setForeground(new Color(40, 40, 100));
        speedSlider = new JSlider(1, 5, 3);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setOpaque(false);
        speedSlider.setPreferredSize(new Dimension(140, 30));
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            int[] intervals = {500, 1000, 3000, 5000, 8000};
            String[] labels  = {"Very Fast (0.5s)", "Fast (1s)", "Normal (3s)", "Slow (5s)", "Very Slow (8s)"};
            int interval = intervals[val - 1];
            speedLabel.setText("Speed: " + labels[val - 1]);
            if (!speedSlider.getValueIsAdjusting() && onSpeedChanged != null)
                onSpeedChanged.accept(interval);
        });
        speedPanel.add(new JLabel("Sim speed:"));
        speedPanel.add(speedSlider);
        speedPanel.add(speedLabel);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(toggles,    BorderLayout.WEST);
        row.add(speedPanel, BorderLayout.EAST);
        p.add(row, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(4, 0, 0, 0),
            new TitledBorder("System Log — ACL Message Activity")));
        p.setBackground(new Color(245, 245, 248));
        p.setPreferredSize(new Dimension(0, 110));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(28, 28, 35));
        logArea.setForeground(new Color(180, 220, 180));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }

    // ── Public update methods called by agents ──

    public void updateConsumption(Map<String, Double> appliances,
            Map<String, Boolean> applianceOn, double totalWatts, double pct) {
        SwingUtilities.invokeLater(() -> {
            applianceTableModel.setRowCount(0);
            for (Map.Entry<String, Double> e : appliances.entrySet()) {
                boolean on = applianceOn.getOrDefault(e.getKey(), false);
                applianceTableModel.addRow(new Object[]{
                    e.getKey(),
                    on ? String.format("%.0f W", e.getValue()) : "—",
                    on ? "ON" : "OFF"
                });
            }
            totalWattsLabel.setText(String.format("%.0f W", totalWatts));
            loadPctLabel.setText(String.format("%.0f%%", pct * 100));
            int pctInt = (int)(pct * 100);
            loadBar.setValue(pctInt);
            loadBar.setString("Load: " + pctInt + "%");
            if (pct >= 0.80) {
                loadBar.setForeground(new Color(220, 60, 40));
                loadPctLabel.setBackground(new Color(220, 60, 40));
            } else if (pct >= 0.60) {
                loadBar.setForeground(new Color(220, 160, 20));
                loadPctLabel.setBackground(new Color(200, 130, 20));
            } else {
                loadBar.setForeground(new Color(40, 160, 80));
                loadPctLabel.setBackground(new Color(40, 130, 80));
            }
            // Sync toggle buttons with actual state
            for (Map.Entry<String, Boolean> e : applianceOn.entrySet()) {
                JToggleButton btn = applianceButtons.get(e.getKey());
                if (btn != null) {
                    btn.setSelected(e.getValue());
                    btn.setBackground(e.getValue()
                        ? new Color(60, 180, 100) : new Color(200, 70, 60));
                }
            }
        });
    }

    public void updateGridState(Map<String, Boolean> powerState, boolean outage) {
        SwingUtilities.invokeLater(() -> {
            for (Map.Entry<String, JLabel> e : gridBulbs.entrySet()) {
                boolean on = powerState.getOrDefault(e.getKey(), false);
                e.getValue().setBackground(
                    on ? new Color(60, 180, 100) : new Color(200, 70, 60));
            }
            outageLabel.setText(outage ? "  Grid: OUTAGE" : "  Grid: NORMAL");
            outageLabel.setForeground(outage
                ? new Color(200, 40, 40) : new Color(40, 130, 80));
        });
    }

    public void updateTariff(boolean isPeak, String label) {
        SwingUtilities.invokeLater(() -> {
            tariffBadge.setText(label);
            tariffBadge.setBackground(
                isPeak ? new Color(200, 80, 40) : new Color(40, 130, 80));
        });
    }

    public void updateDeferred(List<String> deferred) {
        SwingUtilities.invokeLater(() -> {
            deferredListModel.clear();
            for (String a : deferred) deferredListModel.addElement(a);
        });
    }

    public void updateSchedulerStatus(String status, double totalWatts, boolean peak) {}

    public void updateAlertStats(int total, int overloads, int reports) {
        SwingUtilities.invokeLater(() -> {
            alertCountLabel.setText(total + " total");
            overloadCountLabel.setText(overloads + " overloads");
            reportCountLabel.setText(reports + " reports");
        });
    }

    public void pushAlert(String message, String level) {
        SwingUtilities.invokeLater(() -> {
            alertArea.append(message + "\n");
            alertArea.setCaretPosition(alertArea.getDocument().getLength());
        });
    }

    public void addLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ── Helpers ──

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(210, 210, 220), 1, true),
            new TitledBorder(BorderFactory.createEmptyBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 13), new Color(60, 60, 120))));
        return p;
    }

    private JLabel badge(String text, Color bg) {
        JLabel lbl = new JLabel(text);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setBorder(new EmptyBorder(2, 8, 2, 8));
        return lbl;
    }

    private JButton controlButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setOpaque(true);
        return btn;
    }

    static class StatusCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, val, sel, foc, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            String v = val == null ? "" : val.toString();
            if ("ON".equals(v)) {
                setBackground(new Color(220, 248, 220));
                setForeground(new Color(20, 120, 20));
            } else {
                setBackground(new Color(255, 235, 235));
                setForeground(new Color(160, 30, 30));
            }
            return this;
        }
    }

    static class DeferredCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object val,
                int idx, boolean sel, boolean foc) {
            super.getListCellRendererComponent(list, val, idx, sel, foc);
            setText("  Deferred: " + val);
            setBackground(sel ? new Color(255, 200, 80) : new Color(255, 245, 210));
            setForeground(new Color(120, 60, 0));
            setBorder(new EmptyBorder(4, 6, 4, 6));
            return this;
        }
    }
}