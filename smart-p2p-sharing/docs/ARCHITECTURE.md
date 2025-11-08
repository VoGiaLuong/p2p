# Smart P2P File Sharing Architecture

This document provides a high-level overview of the sample project structure for a UDP-based P2P
file sharing application with AI-driven classification and n8n automation hooks.

## Modules

- **p2p-transport**: Contains reusable networking utilities for sending/receiving file chunks over UDP.
- **file-classification**: Hosts AI-assisted heuristics responsible for inferring the file category and
  organizing the storage layout.
- **n8n-integration**: Bridges the Java services with n8n webhooks to orchestrate downstream workflows.

## Applications

- **desktop-client**: Reference client that classifies a file, triggers an n8n workflow, and sends the
  payload to another peer via UDP.
- **node-service**: Reference headless node that listens for UDP packets, stores files according to the
  classifier label, mirrors the runtime storage tree, and notifies n8n for additional automation.

## Runtime Storage Layout

The `runtime/storage` directory is structured to support a two-machine transfer scenario:

```
runtime/storage/
├── inbox/          # Files written directly from the UDP receiver
├── processing/     # Temporary staging area during AI classification
├── classified/     # Auto-generated <label>/<YYYY-MM-DD>/ directories
└── shared/         # Optional export surface for curated assets
```

The `FileOrganizer` utility automatically creates these directories, ensuring the classification
results are mirrored in the filesystem so that n8n workflows can react to each file category.

## Automation Assets

The `configs/n8n` directory bundles Docker Compose and workflow templates to bootstrap an n8n instance
alongside example automation steps.
