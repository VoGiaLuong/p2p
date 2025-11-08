package com.p2p.ui;

import com.p2p.network.PeerDiscoveryService;
import com.p2p.network.PeerInfo;
import com.p2p.transfer.FileSender;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MainController {

    private static final Logger LOGGER = LogManager.getLogger(MainController.class);

    private final String peerId;
    private final PeerDiscoveryService peerDiscoveryService;
    private final FileSender fileSender;
    private final TransferController transferController;
    private final ObservableList<PeerInfo> peers = FXCollections.observableArrayList();
    private Timeline timeline;

    public MainController(String peerId,
                          PeerDiscoveryService peerDiscoveryService,
                          FileSender fileSender,
                          TransferController transferController) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.peerDiscoveryService = Objects.requireNonNull(peerDiscoveryService, "peerDiscoveryService");
        this.fileSender = Objects.requireNonNull(fileSender, "fileSender");
        this.transferController = Objects.requireNonNull(transferController, "transferController");
    }

    public Parent build(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        Label header = new Label("Smart P2P File Sharing - Peer: " + peerId);
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ListView<PeerInfo> peerList = new ListView<>(peers);
        peerList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        peerList.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PeerInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getPeerId() + " - " + item.getAddress().getHostAddress() + ":" + item.getPort());
                }
            }
        });

        Button sendButton = new Button("Send File");
        sendButton.setDisable(true);
        sendButton.setOnAction(event -> handleSend(stage, peerList.getSelectionModel().getSelectedItem()));

        peerList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> sendButton.setDisable(newValue == null));

        VBox center = new VBox(10, new Label("Discovered peers:"), peerList);
        center.setPadding(new Insets(10, 0, 10, 0));

        ToolBar toolBar = new ToolBar(sendButton);

        root.setTop(header);
        root.setCenter(center);
        root.setBottom(new VBox(toolBar, new Label("Transfer log:"), transferController.getView()));

        startRefreshing();
        stage.setOnCloseRequest(event -> stop());
        return root;
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void startRefreshing() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshPeers()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        refreshPeers();
    }

    private void refreshPeers() {
        Collection<PeerInfo> discovered = peerDiscoveryService.getPeers();
        Platform.runLater(() -> {
            peers.setAll(discovered);
        });
    }

    private void handleSend(Stage stage, PeerInfo peerInfo) {
        if (peerInfo == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select file to send");
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        transferController.addLog("Queued transfer to " + peerInfo.getPeerId() + ": " + file.getName());
        CompletableFuture.runAsync(() -> {
            try {
                fileSender.sendFile(Path.of(file.getAbsolutePath()), new InetSocketAddress(peerInfo.getAddress(), peerInfo.getPort()));
                transferController.addLog("✅ Transfer completed: " + file.getName());
            } catch (Exception ex) {
                LOGGER.error("Transfer failed", ex);
                transferController.addLog("❌ Transfer failed: " + ex.getMessage());
            }
        });
    }
}
