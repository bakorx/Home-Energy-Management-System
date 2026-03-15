package hems.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import hems.gui.DashboardGUI;
import java.util.*;

public class GridAgent extends Agent {

    private DashboardGUI gui;
    private final Map<String, Boolean> powerState = new LinkedHashMap<>();
    private boolean gridOutage = false;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) gui = (DashboardGUI) args[0];

        for (String a : Arrays.asList(
                "AC","WaterHeater","WashingMachine","Oven","TV","Fridge","Lights"))
            powerState.put(a, true);

        log("Started. Controlling " + powerState.size() + " circuits.");
        if (gui != null) gui.updateGridState(powerState, gridOutage);

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) handleCommand(msg);
                else block();
            }
        });
    }

    private void handleCommand(ACLMessage msg) {
        String content = msg.getContent();
        String sender  = msg.getSender().getLocalName();
        if (content == null) return;

        if (content.startsWith("POWER_OFF:")) {
            String appliance = content.substring(10);
            setPower(appliance, false);
            sendACL(sender, ACLMessage.CONFIRM, "ACK_OFF:" + appliance);
            sendACL("ConsumptionMonitor", ACLMessage.INFORM,
                "CIRCUIT_CHANGE:" + appliance + ":OFF");

        } else if (content.startsWith("POWER_ON:")) {
            String appliance = content.substring(9);
            setPower(appliance, true);
            sendACL(sender, ACLMessage.CONFIRM, "ACK_ON:" + appliance);
            sendACL("ConsumptionMonitor", ACLMessage.INFORM,
                "CIRCUIT_CHANGE:" + appliance + ":ON");

        } else if (content.startsWith("OUTAGE:")) {
            gridOutage = true;
            for (String a : Arrays.asList("AC","WaterHeater","WashingMachine","Oven"))
                setPower(a, false);
            sendACL("AlertAgent", ACLMessage.INFORM,
                "OUTAGE:Grid outage — non-essential circuits cut");
        }
    }

    private void setPower(String appliance, boolean on) {
        if (powerState.containsKey(appliance)) {
            powerState.put(appliance, on);
            log("Power " + (on ? "ON " : "OFF") + "-> " + appliance);
            if (gui != null) gui.updateGridState(powerState, gridOutage);
        }
    }

    private void sendACL(String target, int performative, String content) {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(new AID(target, AID.ISLOCALNAME));
        msg.setContent(content);
        send(msg);
    }

    private void log(String msg) {
        System.out.println("[GridAgent] " + msg);
        if (gui != null) gui.addLog("[GridAgent] " + msg);
    }
}