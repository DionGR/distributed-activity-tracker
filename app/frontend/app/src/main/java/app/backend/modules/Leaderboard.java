package app.frontend.app.src.main.java.app.backend.modules;

import app.backend.modules.IntermediateChunk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Leaderboard implements Serializable {

    private String fileName;
    private List<String> userNames;
    private List<IntermediateChunk> userTimes;

    public Leaderboard(String fileName) {
        this.fileName = fileName;
        this.userNames = null;
        this.userTimes = null;
    }

    public  void setLeaderboard(HashMap<String, IntermediateChunk> leaderboard) {
        this.userNames = new ArrayList<>(leaderboard.keySet());
        this.userTimes = new ArrayList<>(leaderboard.values());
    }

    public void setUserNames(List<String> userNames) {
        this.userNames = userNames;
    }

    public void setUserTimes(List<IntermediateChunk> userTimes) {
        this.userTimes = userTimes;
    }

    public String getFileName() {
        return fileName;
    }
    public List<String> getUserNames() {
        return userNames;
    }

    public List<IntermediateChunk> getUserTimes() {
        return userTimes;
    }

    public int size() {
        return userNames.size();
    }
}
