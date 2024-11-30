# <div align="center">G04 : Algorithm Alchemists</div>

<img src="https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExZ3VtdGk5MDN2d3EzeTd5a2F1dWRwb3Y3dzQ1czg1dzJhdTkwYW5nZyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/6fbksneAJLITC/giphy.webp" height=12 width=100%/>

### Overview

We are developing **DynoDroid** which  is an advanced mobile security solution designed to identify and analyze malicious applications on Android devices. By leveraging both **dynamic** and **static analysis**, DynoDroid ensures comprehensive malware detection and delivers detailed insights to users.  

Our system not only identifies harmful behavior but also provides a detailed report marking the malicious syscalls , permisiions , intents etc which make given app malicious

### Key Features

1. **Dynamic Analysis:**
   - The app extracts the downloaded APK and uploads it to the server.
   - The server runs the APK in a simulated environment using user interaction patterns:
     - **Loading:** Observing behavior during the initialization phase.
     - **Background Execution:** Monitoring activities when the app is idle.
     - **Active Interaction:** Simulating real-world user interactions with the app.
   - Forces malware to execute by simulating real device usage, capturing malicious behavior.

2. **Static Analysis:**
   - Inspects the app's source code for malicious patterns.
   - Analyzes permissions, intents, and other app components for suspicious activity.

3. **Detailed Reporting:**
   - Generates a comprehensive report rating the app’s risk level.
   - Highlights the **syscalls**, **permissions**, and **intents** contributing to the malicious rating.
   - Provides actionable insights for users to make informed decisions.

<img src="https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExZ3VtdGk5MDN2d3EzeTd5a2F1dWRwb3Y3dzQ1czg1dzJhdTkwYW5nZyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/6fbksneAJLITC/giphy.webp" height=12 width=100%/>

### Workflow

1. **App Scan:**
   - Users initiate a scan for downloaded apps on their device.
   
2. **Analysis Workflow:**
   - **Dynamic Analysis:** The app is sent to the server, where the dynamic analysis explained above  is performed in a controlled environment.
   - **Static Analysis:** The APK is inspected locally for potential threats.

3. **Report Generation:**
   - The system consolidates findings from both analyses into an intuitive report.

4. **Actionable Feedback:**
   - Users can review malicious indicators and take appropriate actions.
  
### Development Status : the current stage of deveopment can be monitored through [Activity_log](https://github.com/Recurse-Official/G04/blob/main/Activity_log.md)

