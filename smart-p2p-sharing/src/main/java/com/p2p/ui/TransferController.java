package com.p2p.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;

public class TransferController {

    private final ObservableList<String> transferLogs = FXCollections.observableArrayList();
    private final ListView<String> listView = new ListView<>(transferLogs);

    public ListView<String> getView() {
        return listView;
    }

    public void addLog(String log) {
        Platform.runLater(() -> {
            transferLogs.add(log);
            listView.scrollTo(transferLogs.size() - 1);
        });
    }
}
