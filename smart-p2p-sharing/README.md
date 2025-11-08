# Smart P2P File Sharing with MIME Detection and n8n Automation

A production-ready Java 17 reference implementation that demonstrates how to build a LAN-focused peer-to-peer (P2P) file sharing solution over UDP. The application validates file integrity with SHA-256, guards against spoofed files by using Apache Tika MIME detection, and automates post-processing through an n8n workflow.

## ✨ Features
- **Peer discovery** via UDP broadcast on port `9875`.
- **Reliable UDP transfer** with metadata handshakes, chunk acknowledgements, retries, and checksum validation.
- **Security-first pipeline** powered by Apache Tika to verify MIME signatures and quarantine suspicious files.
- **n8n automation** that classifies files by extension, creates folders on demand, moves organised assets, and posts notifications.
- **JavaFX desktop shell** (stub) ready to present peer lists and transfer progress.

## 🗂️ Project layout
```
smart-p2p-sharing/
├── pom.xml
├── README.md
├── docs/
│   └── ARCHITECTURE.md
├── n8n/
│   ├── docker-compose.yml
│   └── workflows/
│       └── file-organizer-workflow.json
├── shared-storage/ (created at runtime)
└── src/
    └── main/
        ├── java/
        │   └── com/p2p/
        │       ├── network/
        │       ├── security/
        │       ├── storage/
        │       ├── transfer/
        │       ├── ui/
        │       └── webhook/
        └── resources/
            ├── application.properties
            └── log4j2.xml
```

## 🚀 Getting started

### 1. Provision n8n
```bash
cd n8n
cp docker-compose.yml.sample docker-compose.yml # (if you need customisation)
docker-compose up -d
```
- Browse to http://localhost:5678 and import `n8n/workflows/file-organizer-workflow.json`.
- Adjust credentials via environment variables if you enable basic auth.

### 2. Build the Java application
```bash
mvn -f pom.xml clean package
```
The fat JAR lands in `target/smart-p2p-sharing-1.0-SNAPSHOT-jar-with-dependencies.jar`.

### 3. Configure peers
- Update `src/main/resources/application.properties` or override via JVM system properties, e.g. `-Dudp.server.port=9876`.
- Ensure UDP ports `9875` and `9876` are reachable across the LAN.

### 4. Run on each machine
```bash
java -jar target/smart-p2p-sharing-1.0-SNAPSHOT-jar-with-dependencies.jar
```
- The application bootstraps directories under `shared-storage/`.
- Peers auto-discover each other and appear in the UI stub.
- File transfers trigger MIME validation and, if safe, fire the n8n webhook.

## 🧪 Testing the workflow
1. Send a file from Machine A to Machine B through the JavaFX UI (or by invoking the `FileSender` programmatically).
2. Machine B assembles chunks in `shared-storage/temp/<session-id>`, validates the checksum, and moves the file to `shared-storage/incoming`.
3. The security checker inspects the MIME signature; suspicious artefacts are quarantined.
4. n8n receives metadata and orchestrates the folder structure in `shared-storage/organized/<category>`.

## ⚙️ Configuration reference
All configuration keys live in `application.properties` and can be overridden via JVM system properties or environment variables. Key options include:

| Key | Description |
| --- | --- |
| `udp.server.port` | UDP listener port for inbound packets. |
| `udp.discovery.port` | Broadcast port for peer discovery heartbeats. |
| `udp.chunk.size` | Chunk size in bytes for UDP data packets (default 8192). |
| `udp.max.retries` | Number of retries before aborting a transfer. |
| `security.allowedMimeMappings` | Extension-to-MIME whitelist used to validate files. |
| `security.enableQuarantine` | Whether suspicious files are moved to the quarantine directory. |
| `n8n.webhookUrl` | n8n webhook endpoint that consumes transfer metadata. |

## 📄 License
This repository is provided as a reference implementation for educational purposes.
