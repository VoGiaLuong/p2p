# Architecture Overview

This document explains the main components that make up the Smart P2P File Sharing solution and how they collaborate to move files securely across a local network before delegating automation tasks to n8n.

## Runtime pipeline

1. **Peer discovery** (`com.p2p.network.PeerDiscoveryService`): each node broadcasts heartbeat packets via UDP on port `9875` and listens for replies to populate an in-memory peer catalogue.
2. **Transfer negotiation** (`com.p2p.transfer.FileSender` ↔ `com.p2p.transfer.FileReceiver`): the sender transmits a metadata packet containing file statistics, SHA-256 checksum, and the sender identity. The receiver acknowledges metadata and allocates temporary storage for the upcoming chunks.
3. **Chunk streaming**: the sender reads the file via `FileChunker`, encapsulates each chunk within a `Packet` (type `DATA`), and waits for per-chunk acknowledgements before advancing. Retries are triggered when acknowledgements do not arrive within the configured timeout.
4. **Assembly and validation**: once all chunks are present, the receiver reassembles the payload, verifies the checksum via `ChecksumUtil`, and invokes `SecurityChecker` to inspect the MIME signature using `MimeDetector`.
5. **Post-processing**:
   - Safe files are moved into `shared-storage/incoming` and a webhook payload is submitted to n8n through `N8nClient`.
   - Suspicious files are relocated into `shared-storage/quarantine` with full audit logging.
6. **Automation**: the supplied n8n workflow organises files under `shared-storage/organized/<category>` and creates folders on demand before emitting a notification.

## Module responsibilities

| Package | Description |
| --- | --- |
| `com.p2p.network` | UDP server/client primitives, packet serialisation, and peer discovery logic. |
| `com.p2p.transfer` | File chunking, sending, receiving, checksum verification, and state machines for transfer sessions. |
| `com.p2p.security` | MIME detection and policy enforcement. |
| `com.p2p.storage` | Directory initialisation, session workspaces, and persistence helpers. |
| `com.p2p.webhook` | n8n webhook client and payload composition. |
| `com.p2p.ui` | JavaFX bootstrap hooks and controllers (stubbed for expansion). |

## Key design choices

- **Reliable UDP**: the application treats UDP as an unreliable transport and layers acknowledgements plus retries over the top. The `Packet` class serialises metadata, chunk state, and acknowledgement information into a fixed header followed by payload bytes.
- **Back-pressure**: the sender blocks on acknowledgements, ensuring receivers are not overwhelmed. Timeouts and retry counts are configurable through `application.properties`.
- **Pluggable security**: MIME policies are supplied as configuration, enabling administrators to extend or restrict allowable types without recompiling the code.
- **Automation isolation**: n8n runs via Docker Compose to keep workflow automation decoupled from the JVM runtime, allowing teams to iterate on workflows without redeploying the Java component.

## Directory conventions

```
shared-storage/
├── incoming/     # Files that passed validation, waiting for n8n processing
├── organized/    # n8n output, sorted by category
├── quarantine/   # Suspicious artefacts moved here for manual review
└── temp/         # Session-specific chunk staging directories
```

The `StorageManager` guarantees these directories exist at startup and tracks session workspaces for partial transfers.

## Extension points

- Replace the JavaFX stub with a full-featured UI that surfaces peer discovery, transfer queues, and audit history.
- Integrate an antivirus engine in `SecurityChecker` for deep scanning before releasing files to end users.
- Extend the webhook payload with correlation identifiers and deliver them to SIEM/SOAR tooling.
