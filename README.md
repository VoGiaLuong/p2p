# Smart P2P Sharing Blueprint

This repository demonstrates a suggested folder structure for building a Java 17 based UDP peer-to-peer
file sharing system that leverages AI-assisted classification and n8n automations.

```
smart-p2p-sharing/
├── applications/
│   ├── desktop-client/
│   │   └── src/main/java/com/example/p2p/client/DesktopClientApp.java
│   └── node-service/
│       └── src/main/java/com/example/p2p/node/NodeServiceApp.java
├── configs/
│   └── n8n/
│       ├── docker-compose.yml
│       └── workflows/
│           └── file-sorting-workflow.json
├── docs/
│   └── ARCHITECTURE.md
├── modules/
│   ├── file-classification/
│   │   └── src/main/java/com/example/p2p/classification/FileTypeClassifier.java
│   ├── n8n-integration/
│   │   └── src/main/java/com/example/p2p/n8n/N8nWebhookClient.java
│   └── p2p-transport/
│       ├── src/main/java/com/example/p2p/transport/UdpFileReceiver.java
│       └── src/main/java/com/example/p2p/transport/UdpFileSender.java
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

Each directory includes sample source files and configuration stubs that can be used as a starting
point when setting up the full system in IntelliJ IDEA.
