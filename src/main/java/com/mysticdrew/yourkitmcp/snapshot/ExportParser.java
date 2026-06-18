package com.mysticdrew.yourkitmcp.snapshot;

import com.mysticdrew.yourkitmcp.ProfilerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Parses a YourKit `-export` output directory. Column lookup is by header name
 *  (case-insensitive substring) so it tolerates column reordering across versions. */
public final class ExportParser {

    public AnalysisResult parse(Path exportDir, int topN) {
        List<HotSpot> hotSpots = findFile(exportDir, "hot", "spot")
            .map(p -> parseHotSpots(p, topN)).orElse(List.of());
        List<MemoryClass> classes = findFile(exportDir, "class", "list")
            .map(p -> parseClasses(p, topN)).orElse(List.of());
        return new AnalysisResult(exportDir.toAbsolutePath().toString(), hotSpots, classes);
    }

    private Optional<Path> findFile(Path dir, String... needles) {
        if (!Files.isDirectory(dir)) return Optional.empty();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        for (String needle : needles) if (!n.contains(needle)) return false;
                        return n.endsWith(".csv") || n.endsWith(".txt");
                    })
                    .findFirst();
        } catch (IOException e) {
            throw new ProfilerException("Failed to scan export dir: " + e.getMessage(), e);
        }
    }

    private List<HotSpot> parseHotSpots(Path file, int topN) {
        List<HotSpot> out = new ArrayList<>();
        forEachRow(file, (header, row) -> {
            String name = cell(header, row, "name");
            if (name == null) return;
            out.add(new HotSpot(name, toDouble(cell(header, row, "time")), toLong(cell(header, row, "count"))));
        });
        return out.stream().limit(topN).toList();
    }

    private List<MemoryClass> parseClasses(Path file, int topN) {
        List<MemoryClass> out = new ArrayList<>();
        forEachRow(file, (header, row) -> {
            String name = cell(header, row, "class");
            if (name == null) return;
            out.add(new MemoryClass(name, toLong(cell(header, row, "object")),
                toLong(cell(header, row, "shallow"))));
        });
        return out.stream().limit(topN).toList();
    }

    private interface RowConsumer { void accept(Map<String, Integer> header, String[] row); }

    private void forEachRow(Path file, RowConsumer consumer) {
        try {
            List<String> lines = Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;
            String[] headerCells = splitCsv(lines.get(0));
            Map<String, Integer> header = new HashMap<>();
            for (int i = 0; i < headerCells.length; i++) {
                header.put(headerCells[i].toLowerCase(Locale.ROOT), i);
            }
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                consumer.accept(header, splitCsv(lines.get(i)));
            }
        } catch (IOException e) {
            throw new ProfilerException("Failed to read export file " + file + ": " + e.getMessage(), e);
        }
    }

    /** Resolve a cell whose header CONTAINS the given key (case-insensitive). */
    private String cell(Map<String, Integer> header, String[] row, String key) {
        String needle = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> e : header.entrySet()) {
            if (e.getKey().contains(needle) && e.getValue() < row.length) {
                return row[e.getValue()].trim();
            }
        }
        return null;
    }

    private String[] splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                cells.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cells.add(cur.toString());
        return cells.toArray(new String[0]);
    }

    private double toDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.replace(",", "").trim()); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Long.parseLong(s.replace(",", "").trim()); } catch (NumberFormatException e) { return 0; }
    }
}
