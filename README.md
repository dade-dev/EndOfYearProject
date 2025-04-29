# Secure LAN Messenger

A simple, secure, peer-to-peer LAN chat application written in Java.

## Features

- **End-to-end encryption:** Messages are encrypted using AES-128, with the key derived securely from a shared password using PBKDF2.
- **Automatic peer discovery:** Connect to other users in your LAN by entering their IP address.
- **Multiple chats:** Start and maintain a chat with each peer individually.
- **Simple GUI:** Clean, intuitive graphical interface with peer list and chat window.
- **Automatic chat switching:** When a new message is received, the chat view switches to the sender automatically.

## How it works

Each client runs on a local machine. Connections are established over the LAN using a user-chosen port. Messages are encrypted with AES-128, using a key derived from a shared password (the same password and salt must be used on all peers). The GUI is written using Swing.

## Setup

### Requirements

- Java 17 or higher

### Compilation

Compile all `.java` files:

```bash
javac src/msg/**/*.java
```

### Running

From the root project directory:

```bash
java -cp src msg.Main
```

## Usage

1. **Start the application** on each computer in your LAN.
2. **Set the same password** for all peers (see `Model.java`, or modify to request password at startup).
3. **Enter the IP address** of another peer to connect and start chatting.
4. **Type your message** and send. Chats are encrypted and visible only to the participants.

### GUI Overview

- **Peer List:** Shows all available chats and connected peers.
- **Chat Area:** Displays the message history with the selected peer.
- **Input Field:** Type your message here.
- **Status Bar:** Shows connection and message status.

When a message arrives from another peer, the chat view will switch automatically to that peer.

## Security

- **Encryption:** Uses AES-128 in ECB mode, with the key derived from PBKDF2 (SHA-256) using a shared password and salt.
- **Key Management:** All peers must use the same password and salt for communication.
- **No message storage:** All messages are kept in-memory only for the session duration.

**Note:** For production use, consider using a random IV and a stronger cipher mode (such as CBC or GCM), and allow password entry at runtime.

## Customization

- **Change the port:** Edit the listening port in `NetworkService.java`.
- **Change the password and salt:** Edit the relevant fields in `Model.java`.
- **Password prompt:** For better usability, modify the app to prompt for the password at startup.

## License

This project is released under the MIT License.

---

### Disclaimer

This project is intended for educational purposes. It is not intended for use in security-critical environments.
