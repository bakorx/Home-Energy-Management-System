package hems.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import hems.gui.DashboardGUI;
import java.text.SimpleDateFormat;
import java.util.*;

public class AlertAgent extends Agent {

    private DashboardGUI gui;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private int totalAlerts = 0, overloadAlerts = 0, usageReports = 0;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) gui = (DashboardGUI) args[0];
        log("Started. Listening for events.");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) handleEvent(msg);
                else block();
            }
        });
    }

    private void handleEvent(ACLMessage msg) {
        String content = msg.getContent();
        String sender  = msg.getSender().getLocalName();
        String time    = sdf.format(new Date());
        if (content == null) return;

        totalAlerts++;

        if (content.startsWith("LOAD_SHED:")) {
            overloadAlerts++;
            String[] parts = content.split(":", 3);
            String appliances = parts.length > 1 ? parts[1] : "";
            String detail     = parts.length > 2 ? parts[2] : "";
            pushAlert("[" + time + "] LOAD SHED — " + detail
                + " (deferred: " + appliances + ")", "WARNING");

        } else if (content.startsWith("RESTORE:")) {
            pushAlert("[" + time + "] RESTORED — " + content.substring(8), "INFO");

        } else if (content.startsWith("USAGE_REPORT:")) {
            usageReports++;
            double watts = Double.parseDouble(content.split(":")[1]);
            pushAlert(String.format("[%s] USAGE — Current load: %.0fW", time, watts), "INFO");

        } else if (content.startsWith("OUTAGE:")) {
            pushAlert("[" + time + "] OUTAGE — " + content.substring(7), "CRITICAL");

        } else {
            pushAlert("[" + time + "] EVENT from " + sender + ": " + content, "INFO");
        }

        if (gui != null) gui.updateAlertStats(totalAlerts, overloadAlerts, usageReports);
    }

    private void pushAlert(String message, String level) {
        System.out.println("[AlertAgent] " + level + " | " + message);
        if (gui != null) {
            gui.pushAlert(message, level);
            gui.addLog("[AlertAgent] " + message);
        }
    }

    private void log(String msg) {
        System.out.println("[AlertAgent] " + msg);
        if (gui != null) gui.addLog("[AlertAgent] " + msg);
    }
}