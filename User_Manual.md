# Talkie: User Manual

## 1. Introduction

**Project Title:** Talkie - Secure P2P Walkie-Talkie & Dispatch Admin System

**Objectives:** 
To provide a secure, low-latency Push-To-Talk (PTT) communication platform with robust oversight capabilities for administrative and dispatch personnel.

**Purpose of the System:** 
Talkie is designed to facilitate real-time voice communication across a localized peer-to-peer network while ensuring accountability. The system enforces secure access and provides administrative oversight through network telemetry tracking, incident flagging (e.g., stuck microphones, excessive airtime), and comprehensive PDF audit report generation.

**Target Users:** 
- **Field Workers:** Personnel on the ground requiring instant, reliable voice communication with their peers and supervisors.
- **Dispatchers / Administrators:** Supervisors tasked with monitoring network traffic, ensuring channel compliance, and generating audit logs.

**Scope of the Application:** 
The application covers secure user registration with hashed authentication, role-based access control (Field Worker vs. Dispatcher), real-time audio transmission with voice-altering capabilities, continuous dispatcher telemetry monitoring, transmission ledger logging, and automated PDF compliance report extraction.

---

## 2. System Walkthrough & Application Interfaces

This section provides a step-by-step guide on how the Talkie system works, from initial registration to report generation.

### 2.1. Registration & Authentication

Before accessing the system, users must create an account and authenticate. The system enforces role-based access, directing users to different interfaces based on their assigned designation.

#### Registration Interface
![Screenshot: Registration Screen](placeholder_registration.png)
*Caption: The Registration Interface capturing user credentials and role selection.*
**Description:** New users must provide their Full Name, Username, Password, and Designation. Importantly, the user must select their **Role** (`Field Worker` or `Dispatcher`) which will dictate their permissions within the app. Users must also agree to the Terms & Conditions before signing up.

#### Login Interface
![Screenshot: Login Screen](placeholder_login.png)
*Caption: The secure Login Interface.*
**Description:** Returning users enter their Username and Password. The authentication system securely verifies credentials. A "Remember For 30 Days" checkbox is available for convenience. Upon successful login, the system redirects the user to their respective dashboard based on their role.

---

### 2.2. Field Worker Experience

Users registered as "Field Workers" are directed to the primary communication interfaces.

#### Home Screen
![Screenshot: Home Screen](placeholder_home.png)
*Caption: The Field Worker Home Dashboard showing paired and available devices.*
**Description:** The Home Screen welcomes the user with a dynamic greeting based on the time of day. It displays a list of **Paired Devices** (contacts the user can currently talk to) and **Available Devices** (contacts available for synchronization). Users can tap the "TALK" button to open a direct channel with a paired contact.

#### Push-To-Talk (PTT) Interface
![Screenshot: PTT Screen](placeholder_ptt.png)
*Caption: The Push-To-Talk active communication interface.*
**Description:** This is the core walkie-talkie interface. It features a large central avatar of the contact and a prominent circular **Mic Button**. 
- **Operation:** The user presses and holds the Mic Button to speak, which triggers a glowing waveform animation indicating active transmission. Releasing the button ends the transmission.
- **Voice Changer:** Tapping the settings icon opens a Voice Changer dialog, allowing the user to disguise their voice using different audio profiles (e.g., Male, Robot, Female).

---

### 2.3. Dispatcher Oversight

Users registered as "Dispatchers" bypass the standard PTT screen and are instead directed to the Dispatch Controller Dashboard.

#### Dispatcher Dashboard
![Screenshot: Dispatcher Dashboard](placeholder_dashboard.png)
*Caption: The Dispatch Controller Dashboard displaying real-time telemetry and logs.*
**Description:** The dashboard provides supervisors with a bird's-eye view of network traffic. 
- **Network Telemetry:** Displays aggregated metrics including *Total Transmissions*, *Total Airtime* (in seconds), and *Flagged Incidents*.
- **Transmission Ledger:** A real-time, scrolling log of all communications across the network. Each entry shows the operator's name, time, channel, and duration.
- **Incident Flagging:** If an operator exceeds a safe transmission duration (e.g., a "stuck mic"), the ledger highlights the entry in red with an explicit `[ALERT: STUCK MIC / EXCESSIVE AIRTIME]` warning tag.

---

### 2.4. Report Generation

To maintain operational compliance, dispatchers can export communication logs.

#### PDF Audit Report
![Screenshot: PDF Report Dialog](placeholder_pdf_dialog.png)
*Caption: The confirmation dialog showing successful PDF export.*
**Description:** From the Dispatcher Dashboard, tapping the **"Export Audit PDF"** button commands the native PDF engine to compile all transmission logs into a formalized document. 
- The generated document, titled *Talkie P2P Network Traffic & Fleet Incident Report*, organizes the data into a clean, professional table format (Time, Operator Name, Channel, Duration, Compliance Status). 
- Incident flags are highlighted in red within the document. The report is automatically saved to the device's Downloads folder for easy sharing and auditing.
