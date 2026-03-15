# Home Energy Management System (HEMS)
## DCIT 403 — Intelligent Agent Systems | Individual Project

---

## What This Project Is

The Home Energy Management System is a multi-agent simulation built in Java using the JADE (Java Agent DEvelopment Framework). It demonstrates how four autonomous agents can cooperate to monitor household electricity consumption, detect overload conditions, defer non-critical appliances, handle grid outages, and notify a homeowner — all automatically, in real time, using FIPA-compliant ACL messages.

The system was designed using the **Prometheus methodology** across all 5 phases:
- Phase 1 — System Specification
- Phase 2 — Architectural Design
- Phase 3 — Interaction Design
- Phase 4 — Detailed Design
- Phase 5 — Implementation (this project)

---

## The Four Agents

| Agent | Role | JADE Behaviour |
|---|---|---|
| `ConsumptionMonitorAgent` | Reads appliance wattage every 3s, detects overload, sends ACL messages | TickerBehaviour |
| `SchedulerAgent` | Receives load alerts, defers appliances, manages peak/off-peak tariff | CyclicBehaviour + TickerBehaviour |
| `GridAgent` | Executes POWER_ON / POWER_OFF commands on individual circuits | CyclicBehaviour |
| `AlertAgent` | Receives all system events, generates timestamped notifications | CyclicBehaviour |

### The Perceive → Decide → Act Loop

```
ConsumptionMonitor   →   perceives appliance wattage every 3 seconds
        |
        |  ACLMessage: OVERLOAD / STATUS
        ↓
SchedulerAgent       →   decides which appliances to defer
        |
        |  ACLMessage: POWER_OFF / POWER_ON
        ↓
GridAgent            →   acts by cutting or restoring circuits
        |
        |  ACLMessage: LOAD_SHED / RESTORE / OUTAGE
        ↓
AlertAgent           →   notifies user and logs all events
```

---

## Project Structure

```
Home-Energy-Management-System/
├── HEMS/
│   ├── src/
│   │   └── hems/
│   │       ├── HEMSLauncher.java              ← main entry point
│   │       ├── agents/
│   │       │   ├── ConsumptionMonitorAgent.java
│   │       │   ├── SchedulerAgent.java
│   │       │   ├── GridAgent.java
│   │       │   └── AlertAgent.java
│   │       └── gui/
│   │           └── DashboardGUI.java          ← Swing dashboard
│   ├── lib/
│   │   └── jade.jar                           ← JADE 4.6 library
│   └── out/                                   ← compiled .class files go here
└── README.md
```

---

## Prerequisites

### 1. Java JDK 8 or higher

Check if Java is installed:
```powershell
java -version
javac -version
```

If not installed, download from: https://adoptium.net
- Choose: **Temurin JDK 17** (or any version 8 and above)
- Run the installer and restart your terminal

### 2. JADE 4.6

1. Go to: https://jade.tilab.com
2. Click **Download**
3. Register a free account and log in
4. Download **jadeBin** (the pre-compiled version)
5. Extract the zip file
6. Find `jade.jar` inside the extracted folder
7. Copy `jade.jar` into the `HEMS/lib/` folder

---

## How to Compile

Open the VS Code terminal (`Ctrl + backtick`) and make sure you are inside the project folder:

```powershell
cd C:\path\to\Home-Energy-Management-System\HEMS
```

Create the output folder if it doesn't exist:
```powershell
mkdir out
```

Compile all Java files:
```powershell
javac -cp lib\jade.jar -d out src\hems\gui\DashboardGUI.java src\hems\agents\ConsumptionMonitorAgent.java src\hems\agents\SchedulerAgent.java src\hems\agents\GridAgent.java src\hems\agents\AlertAgent.java src\hems\HEMSLauncher.java
```

If successful you will see no output and the `out/` folder will contain compiled `.class` files.

---

## How to Run

```powershell
java -cp "out;lib\jade.jar" hems.HEMSLauncher
```

The dashboard GUI will open automatically. All 4 agents start within 1 second of each other.

---

## Understanding the Dashboard

When the simulation starts you will see a window with 4 panels and a log bar at the bottom.

### Top-Left — Consumption Monitor Agent
Shows a live table of all 7 appliances (AC, WaterHeater, WashingMachine, Oven, TV, Fridge, Lights) with their wattage and ON/OFF status. The progress bar below the table shows total household load as a percentage of the 3000W capacity. It turns:
- **Green** — safe (below 60%)
- **Amber** — getting high (60–80%)
- **Red** — overload detected (above 80%)

### Top-Right — Grid Agent
Shows 7 coloured circuit tiles, one per appliance:
- **Green tile** — appliance circuit is ON (powered)
- **Red tile** — appliance circuit has been cut (no power)

Tiles flip automatically when SchedulerAgent sends POWER_OFF or POWER_ON commands.

### Bottom-Left — Scheduler Agent
Shows the current tariff period badge (**OFF-PEAK** in green or **PEAK** in red) and a list of any appliances that have been deferred (paused) due to overload. The list clears automatically when tariff switches to off-peak and appliances are restored.

### Bottom-Right — Alert Agent
Shows a timestamped event log of every notification — load sheds, restorations, usage reports, and outages. Three counters at the top track total alerts, overload events, and usage reports seen.

### Manual Controls Bar (middle, blue area)
- **Toggle buttons** — click any appliance name to manually turn it ON or OFF. The agents will react immediately.
- **Simulate Outage** (red button, top right) — triggers a grid outage, cutting all non-essential circuits instantly.
- **Reset Simulation** (blue button, top right) — restores all appliances to ON and resets the tick counter to 0.
- **Speed slider** — drag left to speed up the simulation (0.5s ticks) or right to slow it down (8s ticks). Useful for demo presentations.

### System Log (dark bar at bottom)
Shows every ACL message passing between agents in real time. This is the live evidence that JADE FIPA ACL communication is happening. Each line shows which agent sent what message.

---

## What Happens When You Run It

The simulation follows a scripted sequence to demonstrate the full agent loop:

| Tick | Event |
|---|---|
| Start | All 7 appliances ON. Total load ~3310W. Already above threshold. |
| Tick 2 | WashingMachine turns ON (was already on — no change) |
| Tick 5 | Oven turns ON — load spikes further |
| Immediately | ConsumptionMonitor detects OVERLOAD, sends message to Scheduler |
| Immediately | Scheduler defers WaterHeater + WashingMachine, sends POWER_OFF to Grid |
| Immediately | Grid cuts those circuits — tiles go red |
| Immediately | AlertAgent logs LOAD SHED notification |
| Every 9s | Tariff flips PEAK ↔ OFF-PEAK |
| On OFF-PEAK | Scheduler restores deferred appliances — tiles go green |
| Tick 8 | AC turns OFF (simulated) |
| Tick 11 | AC turns back ON |
| Tick 14 | Oven turns OFF |
| Every 3 ticks | ConsumptionMonitor sends USAGE_REPORT to Alert |

---

## ACL Message Reference

All messages use JADE's FIPA ACL format. The content field uses colon-separated strings.

| Message | Performative | Sender | Receiver | Content Format |
|---|---|---|---|---|
| OVERLOAD | REQUEST | ConsumptionMonitor | SchedulerAgent | `OVERLOAD:<watts>:<applianceStatus>` |
| STATUS | INFORM | ConsumptionMonitor | SchedulerAgent | `STATUS:<watts>:<applianceStatus>` |
| USAGE_REPORT | INFORM | ConsumptionMonitor | AlertAgent | `USAGE_REPORT:<watts>:<tick>` |
| POWER_OFF | REQUEST | SchedulerAgent | GridAgent | `POWER_OFF:<applianceName>` |
| POWER_ON | REQUEST | SchedulerAgent | GridAgent | `POWER_ON:<applianceName>` |
| LOAD_SHED | INFORM | SchedulerAgent | AlertAgent | `LOAD_SHED:<appliances>:<detail>` |
| RESTORE | INFORM | SchedulerAgent | AlertAgent | `RESTORE:<detail>` |
| ACK_OFF | CONFIRM | GridAgent | SchedulerAgent | `ACK_OFF:<applianceName>` |
| ACK_ON | CONFIRM | GridAgent | SchedulerAgent | `ACK_ON:<applianceName>` |
| CIRCUIT_CHANGE | INFORM | GridAgent | ConsumptionMonitor | `CIRCUIT_CHANGE:<appliance>:ON/OFF` |
| OUTAGE | INFORM | GridAgent | AlertAgent | `OUTAGE:<detail>` |

---

## Common Errors and Fixes

**Error: `javac` is not recognized**
Java JDK is not installed or not in your PATH. Download from adoptium.net, install, then restart VS Code and try again.

**Error: `Unresolved compilation problems` mentioning JADE classes**
The compile command was run without `-cp lib\jade.jar` or jade.jar is in the wrong location. Make sure jade.jar is inside the `HEMS\lib\` folder and the compile command includes `-cp lib\jade.jar`.

**Error: `ClassNotFoundException: hems.HEMSLauncher`**
The compile step did not complete successfully or you are running from the wrong directory. Make sure you are inside the `HEMS\` folder and the `out\hems\` subfolder exists and contains `.class` files.

**Dashboard opens but nothing happens**
One or more agents failed to start. Check the terminal output for any Java exceptions. The most common cause is JADE not finding a local host. Make sure your machine has a working network adapter (even if not connected to the internet).

**Agents start but messages are not received**
Agent startup order issue. This is already handled in `HEMSLauncher.java` with `Thread.sleep(300)` delays. If it still happens, increase the delay to 500ms.

---

## Simulation Configuration

You can change these values directly in the source files before compiling:

| Setting | File | Variable | Default |
|---|---|---|---|
| Tick interval (ms) | `ConsumptionMonitorAgent.java` | `tickInterval` | `3000` |
| Overload threshold | `ConsumptionMonitorAgent.java` | `overloadThresholdPct` | `0.80` |
| Max household capacity | `ConsumptionMonitorAgent.java` | `totalCapacityWatts` | `3000.0` |
| Tariff flip interval (ms) | `SchedulerAgent.java` | TickerBehaviour period | `9000` |
| Max appliances to defer | `SchedulerAgent.java` | `result.size() >= 2` | `2` |

---

## Technology Justification

**Java** was chosen because JADE is a Java-native framework and provides the most complete implementation of the FIPA agent standard. Java's threading model works naturally with JADE's behaviour system, and Swing integrates cleanly with JADE's agent lifecycle for real-time GUI updates.

**JADE 4.6** provides agent containers, lifecycle management, the FIPA ACL message transport service, and behaviour scheduling (TickerBehaviour, CyclicBehaviour) out of the box. No other framework provides this level of FIPA compliance in Java.

**Java Swing** was used for the GUI because it is part of the Java standard library (no extra dependencies), integrates naturally with JADE agents via `SwingUtilities.invokeLater()`, and produces a desktop application that runs on any OS.

---

## Course Information

- **Course:** DCIT 403 — Intelligent Agent Systems
- **Project type:** Individual Project
- **Methodology:** Prometheus Agent-Oriented Software Engineering
- **Framework:** JADE 4.6 (Java Agent DEvelopment Framework)
- **Language:** Java JDK 8+
