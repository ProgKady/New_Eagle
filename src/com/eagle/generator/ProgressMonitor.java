package com.eagle.generator;

public interface ProgressMonitor {
    void onPhase(String phase, double percent);
    void onLog(String message);
    void onError(String error);
    void onComplete(int totalFiles, java.util.List<String> filePaths);
    boolean isCancelled();
}
