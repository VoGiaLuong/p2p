# Smart P2P Sharing Blueprint

This repository demonstrates a suggested folder structure for building a Java 17 based UDP peer-to-peer
file sharing system that leverages AI-assisted classification and n8n automations. The layout is ready to
be imported into IntelliJ IDEA and pairs with an open-source n8n instance running in Docker.

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
│   │   ├── src/main/java/com/example/p2p/classification/FileOrganizer.java
│   │   └── src/main/java/com/example/p2p/classification/FileTypeClassifier.java
│   ├── n8n-integration/
│   │   └── src/main/java/com/example/p2p/n8n/N8nWebhookClient.java
│   └── p2p-transport/
│       ├── src/main/java/com/example/p2p/transport/UdpFileReceiver.java
│       └── src/main/java/com/example/p2p/transport/UdpFileSender.java
├── runtime/
│   └── storage/
│       ├── inbox/
│       ├── processing/
│       ├── classified/
│       └── shared/
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

Key runtime directories:

- `runtime/storage/inbox`: initial landing zone for files received over UDP.
- `runtime/storage/processing`: working directory where files are staged for AI classification.
- `runtime/storage/classified`: automatically generated per-label folders (partitioned by date).
- `runtime/storage/shared`: optional area to expose curated content to other peers or services.

Each directory includes sample source files and configuration stubs that can be used as a starting
point when setting up the full system in IntelliJ IDEA.
