# Secret Messenger

A secure, peer-to-peer LAN chat application with end-to-end encryption written in Java.

## Features

- **End-to-end encryption:** Messages are securely encrypted using AES with PBKDF2 key derivation (SHA-256)
- **Multimedia support:** Send both text messages and images to your peers
- **Automatic peer discovery:** Automatically finds other instances on your local network
- **Personal notes:** Keep a personal chat for your own notes and reminders
- **Chat management:** Rename chats for easier identification of peers
- **Clean GUI:** Intuitive interface with a peer list, chat window, and status updates
- **Connection status:** Real-time feedback on connection attempts and message delivery
- **Configurable settings:** Adjust encryption parameters and network settings via config file

## How It Works

SecretMessenger establishes secure connections between peers on a local area network. Each message is encrypted using AES with a key derived from a shared password using PBKDF2. The application features automatic peer discovery, connection management, and a simple but effective GUI built with Java Swing.

## Setup

### Requirements

- Java 17 or higher

### Configuration

The application will look for a configuration file at `config/config.properties`. If not found, default values will be used.

Example configuration file:
```properties
PASSWORD=your_secure_password_here
SALT=random numbers separated by ',' like 3,14,15,9 and so on
LISTEN_PORT=9000 # raccomended
```

### Compilation

Compile all Java source files with the bin directory as the output path:

```bash
javac -d secretMessanger/bin secretMessanger/src/msg/*.java secretMessanger/src/msg/*/*.java
```

### Running

From the project root directory, run the application with:

```bash

java -cp secretMessanger/bin msg.Main
```

## Usage Guide

1. **Start the application** on each computer you wish to connect
2. **Add peers** by entering their IP addresses or wait for automatic discovery
3. **Send messages** by selecting a peer from the list and typing in the message field
4. **Send images** using the special image sharing functionality
5. **Rename chats** for easier identification of your peers
6. **Check connection status** in the status bar at the bottom of the window

### The Interface

- **Peer List:** Shows all connected peers with their names/IPs
- **Chat Area:** Displays your conversation with the selected peer
- **Input Field:** Type messages or use special commands here
- **Status Bar:** Shows connection status and message confirmation

## Security Notes

- **Encryption:** Uses AES with PBKDF2 key derivation function (SHA-256)
- **Shared Password:** All peers must use the same password for successful communication
- **No message persistence:** Messages are stored in memory only during the session
- **Local network only:** Designed for use within trusted local networks

## Advanced Usage

- Use the personal chat (labeled "Me") for keeping notes to yourself
- Configure the application by editing the config.properties file
- Monitor connections through the status updates in the application window

## License

This project is available under the MIT License.

---

### Disclaimer

This application is designed for educational purposes. While it implements strong encryption, it should not be relied upon for transmitting highly sensitive information in security-critical environments.
