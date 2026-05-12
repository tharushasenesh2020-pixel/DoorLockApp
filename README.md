# DoorLockApp
# 🔐 DoorLockApp

A Bluetooth-based Smart Door Lock system built as an embedded systems project. This Android app communicates with an ATmega328P microcontroller over Bluetooth (HC-06) to control a physical door lock — with role-based access, PIN authentication, and Firebase-backed audit logging.

---

## 📱 Screenshots

| Home (Admin) | PIN Entry | Lock Status |
|---|---|---|
| ![Home](screenshots/home_admin.png) | ![PIN](screenshots/pin_entry.png) | ![Status](screenshots/lock_status.png) |

| Manage Users | Settings | Change PIN |
|---|---|---|
| ![Users](screenshots/manage_users.png) | ![Settings](screenshots/settings.png) | ![Change PIN](screenshots/change_pin.png) |

---

## ✨ Features

### 🔑 Role-Based Access
- **Admin** — Full control: change PIN, set auto-lock delay, manage users, enable 1-touch locking, view logs, delete lock
- **User** — Limited access: unlock the door and check lock status only

### 📡 Bluetooth Connectivity
- Connects to the door lock via **HC-06 Bluetooth module**
- Scans and lists nearby paired Bluetooth devices
- Real-time connection status display

### 🔢 PIN Authentication
- Custom PIN keypad UI (4–8 digit PIN support)
- Secure PIN change with confirmation step
- PIN change history logged to Firebase

### 🏠 Smart Lock Control
- Unlock door remotely via Bluetooth
- **Lock Door** button for instant re-locking
- Configurable **auto-lock delay** (default: 5 minutes)
- **1-Touch Locking** toggle
- **KeyPress Beep** toggle

### ☁️ Firebase Integration
- User authentication (email/password login)
- Real-time database for lock state sync
- Access history, PIN change history & app usage logs
- Multi-user management from the admin panel

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Mobile App | Android Studio (Java) |
| Backend | Firebase (Auth + Realtime Database) |
| Microcontroller | ATmega328P |
| Communication | HC-06 Bluetooth Module |
| Protocol | UART Serial over Bluetooth |

---

## 🔧 How It Works

1. The Android app connects to the **HC-06 Bluetooth module** wired to the ATmega328P
2. When the user enters the correct PIN, the app sends an unlock command via Bluetooth serial
3. The ATmega328P receives the command and actuates the door lock mechanism
4. After the configured delay (default 5 min), the lock automatically re-engages
5. All access events are logged in real time to **Firebase**

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Arctic Fox or later)
- Android device with Bluetooth (API 21+)
- Firebase project with Authentication and Realtime Database enabled
- ATmega328P hardware with HC-06 module

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/tharushasenesh2020-pixel/DoorLockApp.git
   cd DoorLockApp
   ```

2. **Open in Android Studio**
   - Open the `doorlock` folder in Android Studio

3. **Connect Firebase**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project and add an Android app
   - Download `google-services.json` and place it in the `app/` directory
   - Enable **Email/Password Authentication** and **Realtime Database**

4. **Build & Run**
   - Connect your Android device
   - Click **Run** in Android Studio

5. **Pair HC-06**
   - Pair HC-06 (`98:D3:31:B3:9A:0B`) in your phone's Bluetooth settings
   - Open the app → Connect Bluetooth → Select HC-06

---

## 📁 Project Structure

```
DoorLockApp/
├── doorlock/
│   ├── app/
│   │   ├── src/main/java/        # Java source files
│   │   └── src/main/res/         # Layouts, drawables, strings
│   └── build.gradle
├── README.md
└── LICENSE
```

---

## 🔒 Security Notes

- PINs are stored and validated via Firebase — not hardcoded
- All login sessions are authenticated through Firebase Auth
- Access and PIN change events are timestamped and logged per user

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Tharusha Senesh**  
[GitHub](https://github.com/tharushasenesh2020-pixel) 

---

> Built as part of an Embedded Systems project — bridging hardware control with a cloud-connected Android application.
