package model;
import java.nio.file.Path;

public record ImageEntry(String id, Path path, long hash, String hasher) {}
