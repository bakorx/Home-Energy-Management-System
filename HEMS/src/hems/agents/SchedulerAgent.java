package hems.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import hems.gui.DashboardGUI;
import java.util.*;

public class SchedulerAgent extends Agent {

    private DashboardGUI gui;
    private final List<String> deferredAppliances = new ArrayList<>();
    private boolean peakHour = false;
    private static final List<String> DEFERRABLE =
        Arrays.asList("WaterHeater", "WashingMachine", "Oven");

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) gui = (DashboardGUI) args[0];
        log("Started.");

        // Listen for messages from ConsumptionMonitor
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchSender(
                    new AID("ConsumptionMonitor", AID.ISLOCALNAME)));
                if (msg != null) handleMonitorMessage(msg);
                else block();
            }
        });

        // Flip peak/off-peak every 9 seconds
        addBehaviour(new TickerBehaviour(this, 9000) {
            @Override
            protected void onTick() {
                peakHour = !peakHour;
                String label = peakHour ? "PEAK" : "OFF-PEAK";
                log("Tariff period: " + label);
                if (gui != null) gui.updateTariff(peakHour, label);

                if (!peakHour && !deferredAppliances.isEmpty()) {
                    log("Off-peak — restoring: " + deferredAppliances);
                    for (String a : new ArrayList<>(deferredAppliances))
                        sendACL("GridAgent", ACLMessage.REQUEST, "POWER_ON:" + a);
                    notifyAlert("RESTORE:Appliances restored during off-peak: " + deferredAppliances);
                    deferredAppliances.clear();
                    if (gui != null) gui.updateDeferred(deferredAppliances);
                }
            }
        });
    }

    private void handleMonitorMessage(ACLMessage msg) {
        String content = msg.getContent();
        if (content == null) return;

        if (content.startsWith("OVERLOAD:")) {
            String[] parts = content.split(":");
            double totalWatts = Double.parseDouble(parts[1]);
            log(String.format("Overload! Total: %.0fW — shedding load", totalWatts));

            String statusStr = parts.length > 2 ? parts[2] : "";
            List<String> toDefer = findActiveDeferable(statusStr);

            if (!toDefer.isEmpty()) {
                for (String a : toDefer) {
                    deferredAppliances.add(a);
                    sendACL("GridAgent", ACLMessage.REQUEST, "POWER_OFF:" + a);
                    log("Deferred: " + a);
                }
                if (gui != null) gui.updateDeferred(deferredAppliances);
                notifyAlert("LOAD_SHED:" + String.join(",", toDefer)
                    + ":High load — deferred: " + toDefer);
            }
        } else if (content.startsWith("STATUS:")) {
            String[] parts = content.split(":");
            double watts = Double.parseDouble(parts[1]);
            if (gui != null) gui.updateSchedulerStatus("Normal", watts, peakHour);
        }
    }

    private List<String> findActiveDeferable(String statusStr) {
        List<String> result = new ArrayList<>();
        for (String line : statusStr.split(";")) {
            if (line.isEmpty()) continue;
            String[] kv = line.split("=");
            if (kv.length == 2 && DEFERRABLE.contains(kv[0])
                    && "1".equals(kv[1]) && !deferredAppliances.contains(kv[0])) {
                result.add(kv[0]);
                if (result.size() >= 2) break;
            }
        }
        return result;
    }

    private void notifyAlert(String message) {
        sendACL("AlertAgent", ACLMessage.INFORM, message);
    }

    private void sendACL(String target, int performative, String content) {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(new AID(target, AID.ISLOCALNAME));
        msg.setContent(content);
        send(msg);
    }

    private void log(String msg) {
        System.out.println("[SchedulerAgent] " + msg);
        if (gui != null) gui.addLog("[SchedulerAgent] " + msg);
    }
}