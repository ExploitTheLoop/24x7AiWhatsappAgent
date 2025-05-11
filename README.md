# 24x7 AI WhatsApp Agent

## Overview
The 24x7 AI WhatsApp Agent is an Android application that integrates with WhatsApp to provide an AI-powered chatbot. The app allows users to control a WhatsApp bot that can operate autonomously, interact with users, and log interactions. It uses APIs like Gemini, Deepgram, and ElevenLabs for AI functionalities, and supports features like internet detection, shadow mode, and Google Sheets logging.

The project consists of two main repositories:
- **[Control Panel APK](https://github.com/ExploitTheLoop/24x7AiWhatsappAgent)**: An Android app built using Java and Android Studio to manage the WhatsApp bot.
- **[Server APIs](https://github.com/ExploitTheLoop/Server-Whatsappbot)**: Backend APIs that the control panel interacts with to manage bot operations.

A demo of the project can be viewed on [YouTube](https://www.youtube.com/watch?v=lvwuYuEHyA8).

## Features
- **Signup and Configuration**:
  - Sign up using API keys for Gemini, Deepgram, and ElevenLabs.
  - Configure ElevenLabs Voice ID, Google Sheet ID, and a personality prompt for the bot.
  - After configuration, a QR code is generated for WhatsApp login.
  - Scan the QR code to log in and start controlling the bot.

- **Shadow Mode**:
  - The bot activates automatically when the phone screen is off and locked, allowing it to operate in the background.

- **Internet Detection Mode**:
  - The bot activates when the device disconnects from the internet, ensuring it can respond to messages even during network disruptions.

- **Google Sheets Logging**:
  - Important messages exchanged by the AI with users are logged into a specified Google Sheet.
  - Notifications are sent to the user for important messages.

- **Assistant Controls**:
  - Monitor bot activity (e.g., Active Bots, Total Views, Messages).
  - Toggle features like Internet Detection Mode, Shadow Mode, and notifications for important messages.

## Screenshots
The control panel interface displays:
- **Bot Statistics**: Active Bots (e.g., 136), Total Views (e.g., 5.4K), Messages (e.g., 51.6K).
- **Assistant Controls**: Options to enable/disable Internet Detection Mode, Shadow Mode, and notifications.
- Below is a screenshot of the control panel:

![Control Panel Screenshot](images/screenshot.png)

## Requirements
- Android device with WhatsApp installed.
- API keys for:
  - Gemini API
  - Deepgram API
  - ElevenLabs API
- A Google Sheet ID for logging messages.
- Java and Android Studio for building the APK.
- Node.js or a compatible environment for running the server APIs.

## Setup Instructions
1. **Clone the Repositories**:
   ```bash
   git clone https://github.com/ExploitTheLoop/24x7AiWhatsappAgent.git
   git clone https://github.com/ExploitTheLoop/Server-Whatsappbot.git
   ```

2. **Set Up the Server**:
   - Navigate to the `Server-Whatsappbot` directory.
   - Install dependencies (e.g., `npm install` if using Node.js).
   - Configure the server with necessary environment variables (e.g., API keys).
   - Run the server (e.g., `npm start`).

3. **Build the APK**:
   - Open the `24x7AiWhatsappAgent` project in Android Studio.
   - Build the APK using Android Studio.
   - Install the APK on your Android device.

4. **Configure the App**:
   - Open the app on your device.
   - Sign up by entering the required API keys (Gemini, Deepgram, ElevenLabs), ElevenLabs Voice ID, Google Sheet ID, and personality prompt.
   - A QR code will be generated for WhatsApp login.

5. **Log In to WhatsApp**:
   - Scan the QR code using WhatsApp to link the bot.
   - The bot will now be active and controllable via the app.

## Usage
- **Control the Bot**:
  - Use the control panel to monitor bot activity (e.g., active bots, messages, views).
  - Enable/disable features like Shadow Mode and Internet Detection Mode.
  - Receive notifications for important messages logged in the Google Sheet.

- **Shadow Mode**:
  - The bot will automatically activate when the phone is locked and the screen is off.

- **Internet Detection**:
  - The bot will activate if the device loses internet connectivity, ensuring uninterrupted operation.

- **Logging**:
  - Check the configured Google Sheet for logged messages.
  - Review notifications for important interactions.

## Visit the Server Repository
The server-side code for this project, which provides the APIs used by the control panel, is available at the following repository:
- **[Server-Whatsappbot Repository](https://github.com/ExploitTheLoop/Server-Whatsappbot)**

## Contributing
Contributions are welcome! Please fork the repository, make your changes, and submit a pull request.

## License
This project is licensed under the MIT License. See the LICENSE file in the repository for more details.
