package hems;

import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import hems.gui.DashboardGUI;
import javax.swing.SwingUtilities;

public class HEMSLauncher {
    public static void main(String[] args) throws Exception {

        final DashboardGUI[] guiHolder = new DashboardGUI[1];
        SwingUtilities.invokeAndWait(() -> guiHolder[0] = new DashboardGUI());
        DashboardGUI gui = guiHolder[0];

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(ProfileImpl.MAIN_HOST, "localhost");
        profile.setParameter(ProfileImpl.GUI, "false");

        AgentContainer container = rt.createMainContainer(profile);
        Object[] agentArgs = new Object[]{ gui };

        AgentController alertAgent = container.createNewAgent(
            "AlertAgent", "hems.agents.AlertAgent", agentArgs);
        AgentController gridAgent = container.createNewAgent(
            "GridAgent", "hems.agents.GridAgent", agentArgs);
        AgentController schedulerAgent = container.createNewAgent(
            "SchedulerAgent", "hems.agents.SchedulerAgent", agentArgs);

        alertAgent.start();
        Thread.sleep(300);
        gridAgent.start();
        Thread.sleep(300);
        schedulerAgent.start();
        Thread.sleep(300);

        AgentController monitorAgent = container.createNewAgent(
            "ConsumptionMonitor", "hems.agents.ConsumptionMonitorAgent", agentArgs);
        monitorAgent.start();

        System.out.println("=== HEMS Simulation Running ===");
    }
}