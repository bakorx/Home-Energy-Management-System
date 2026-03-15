package hems.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import hems.gui.DashboardGUI;
import java.util.*;

public class ConsumptionMonitorAgent extends Agent {

    public static final String NAME = "ConsumptionMonitor";

    private final Map<String, Double> appliances = new LinkedHashMap<>();
    private final Map<String, Boolean> applianceOn = new LinkedHashMap<>();
    private double totalCapacityWatts = 3000.0;
    private double overloadThresholdPct = 0.80;
    private DashboardGUI gui;
    private int tick = 0;
    private int tickInterval = 3000; // ms — controlled by speed slider
    private TickerBehaviour tickerBehaviour;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) gui = (DashboardGUI) args[0];

        appliances.put("AC",             1200.0);
        appliances.put("WaterHeater",     800.0);
        appliances.put("WashingMachine",  500.0);
        appliances.put("Oven",            600.0);
        appliances.put("TV",              150.0);
        appliances.put("Fridge",          180.0);
        appliances.put("Lights",           80.0);

        for (String a : appliances.keySet()) applianceOn.put(a, true);

        // Wire GUI callbacks
        if (gui != null) {
            // Manual appliance toggle from button
            gui.setOnApplianceToggle(args2 -> {
                String name = args2[0];
                boolean on  = "ON".equals(args2[1]);
                setAppliance(name, on);
                addLog("Manual override: " + name + " -> " + (on ? "ON" : "OFF"));
            });

            // Simulate outage button
            gui.setOnOutageTriggered(() -> {
                addLog("MANUAL OUTAGE triggered from dashboard");
                sendACL("GridAgent", ACLMessage.REQUEST, "OUTAGE:manual");
                sendACL("AlertAgent", ACLMessage.INFORM,
                    "OUTAGE:Manual outage triggered from dashboard");
            });

            // Reset button — restart simulation state
            gui.setOnReset(() -> {
                tick = 0;
                for (String a : appliances.keySet()) applianceOn.put(a, true);
                double total = getTotalConsumption();
                gui.updateConsumption(appliances, applianceOn, total,
                    total / totalCapacityWatts);
                sendACL("AlertAgent", ACLMessage.INFORM,
                    "RESET:Simulation reset by user");
                addLog("Simulation reset — all appliances restored");
            });

            // Speed slider — change tick interval
            gui.setOnSpeedChanged(ms -> {
                tickInterval = ms;
                // Remove old behaviour and add new one with updated period
                removeBehaviour(tickerBehaviour);
                tickerBehaviour = buildTickerBehaviour();
                addBehaviour(tickerBehaviour);
                addLog("Simulation speed changed to " + ms + "ms per tick");
            });
        }

        log("Started. Monitoring " + appliances.size() + " appliances.");
        tickerBehaviour = buildTickerBehaviour();
        addBehaviour(tickerBehaviour);
    }

    private TickerBehaviour buildTickerBehaviour() {
        return new TickerBehaviour(this, tickInterval) {
            @Override
            protected void onTick() {
                tick++;
                simulateUsageChanges();
                double total = getTotalConsumption();
                double pct   = total / totalCapacityWatts;

                if (gui != null) gui.updateConsumption(appliances, applianceOn, total, pct);
                log(String.format("Tick %d | Total: %.0fW (%.0f%%)", tick, total, pct * 100));

                if (pct >= overloadThresholdPct) {
                    log("OVERLOAD — notifying SchedulerAgent");
                    sendACL("SchedulerAgent", ACLMessage.REQUEST,
                        "OVERLOAD:" + total + ":" + buildStatus());
                } else {
                    sendACL("SchedulerAgent", ACLMessage.INFORM,
                        "STATUS:" + total + ":" + buildStatus());
                }

                if (tick % 3 == 0) {
                    sendACL("AlertAgent", ACLMessage.INFORM,
                        "USAGE_REPORT:" + total + ":" + tick);
                }
            }
        };
    }

    private void simulateUsageChanges() {
        if (tick == 2)  setAppliance("WashingMachine", true);
        if (tick == 5)  setAppliance("Oven", true);
        if (tick == 8)  setAppliance("AC", false);
        if (tick == 11) setAppliance("AC", true);
        if (tick == 14) setAppliance("Oven", false);
    }

    public void setAppliance(String name, boolean on) {
        if (applianceOn.containsKey(name)) {
            applianceOn.put(name, on);
            log((on ? "ON" : "OFF") + " -> " + name);
            if (gui != null) gui.updateConsumption(appliances, applianceOn,
                getTotalConsumption(), getTotalConsumption() / totalCapacityWatts);
        }
    }

    public double getTotalConsumption() {
        double total = 0;
        for (Map.Entry<String, Double> e : appliances.entrySet())
            if (applianceOn.get(e.getKey())) total += e.getValue();
        return total;
    }

    private String buildStatus() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : applianceOn.entrySet())
            sb.append(e.getKey()).append("=").append(e.getValue() ? "1" : "0").append(";");
        return sb.toString();
    }

    private void sendACL(String target, int performative, String content) {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(new AID(target, AID.ISLOCALNAME));
        msg.setContent(content);
        send(msg);
    }

    private void addLog(String msg) {
        if (gui != null) gui.addLog("[ConsumptionMonitor] " + msg);
    }

    private void log(String msg) {
        System.out.println("[ConsumptionMonitor] " + msg);
        if (gui != null) gui.addLog("[ConsumptionMonitor] " + msg);
    }
}