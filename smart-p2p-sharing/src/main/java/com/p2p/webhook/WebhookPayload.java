package com.p2p.webhook;

public class WebhookPayload {

    private final String fileName;
    private final String filePath;
    private final long fileSize;
    private final String checksum;
    private final String mimeType;
    private final String receivedFrom;
    private final long timestamp;
    private final String securityStatus;

    private WebhookPayload(Builder builder) {
        this.fileName = builder.fileName;
        this.filePath = builder.filePath;
        this.fileSize = builder.fileSize;
        this.checksum = builder.checksum;
        this.mimeType = builder.mimeType;
        this.receivedFrom = builder.receivedFrom;
        this.timestamp = builder.timestamp;
        this.securityStatus = builder.securityStatus;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getReceivedFrom() {
        return receivedFrom;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSecurityStatus() {
        return securityStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fileName;
        private String filePath;
        private long fileSize;
        private String checksum;
        private String mimeType;
        private String receivedFrom;
        private long timestamp;
        private String securityStatus;

        private Builder() {}

        public Builder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder withFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder withFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder withChecksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder withReceivedFrom(String receivedFrom) {
            this.receivedFrom = receivedFrom;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withSecurityStatus(String securityStatus) {
            this.securityStatus = securityStatus;
            return this;
        }

        public WebhookPayload build() {
            return new WebhookPayload(this);
        }
    }
}
