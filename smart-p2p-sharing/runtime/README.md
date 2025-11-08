# Runtime Storage Layout

This directory mirrors the folders that the node service uses at runtime when receiving files from
remote peers. The folders are committed with `.gitkeep` placeholders so the structure is visible in
version control.

- `inbox/`: raw UDP payloads are written here before processing.
- `processing/`: transient workspace while AI classification and metadata enrichment occur.
- `classified/`: final resting place for files, partitioned by classifier label and calendar date.
- `shared/`: optional area for exposing curated or approved artifacts to other peers or services.
