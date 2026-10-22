import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Detects modified, new, and deleted files using SHA-256 hashes.
 * Usage:
 *   java FileIntegrityMonitor baseline <folder>   (saves baseline.txt)
 *   java FileIntegrityMonitor check <folder>      (compares to baseline.txt)
 */
public class FileIntegrityMonitor {
    private static final Path BASELINE = Paths.get("baseline.txt");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: baseline|check <folder>");
            return;
        }
        Map<String, String> current = hashFolder(Paths.get(args[1]));
        if (args[0].equals("baseline")) {
            List<String> lines = current.entrySet().stream()
                    .map(e -> e.getKey() + "|" + e.getValue()).collect(Collectors.toList());
            Files.write(BASELINE, lines);
            System.out.println("Baseline saved for " + current.size() + " file(s).");
        } else if (args[0].equals("check")) {
            check(current);
        } else {
            System.out.println("Unknown mode: " + args[0]);
        }
    }

    private static void check(Map<String, String> current) throws IOException {
        Map<String, String> saved = new HashMap<>();
        for (String line : Files.readAllLines(BASELINE)) {
            String[] parts = line.split("\\|");
            saved.put(parts[0], parts[1]);
        }
        int issues = 0;
        for (Map.Entry<String, String> e : current.entrySet()) {
            if (!saved.containsKey(e.getKey())) {
                System.out.println("NEW:      " + e.getKey()); issues++;
            } else if (!saved.get(e.getKey()).equals(e.getValue())) {
                System.out.println("MODIFIED: " + e.getKey()); issues++;
            }
        }
        for (String path : saved.keySet()) {
            if (!current.containsKey(path)) {
                System.out.println("DELETED:  " + path); issues++;
            }
        }
        System.out.println(issues == 0 ? "No changes detected." : issues + " change(s) detected.");
    }

    private static Map<String, String> hashFolder(Path folder) throws Exception {
        Map<String, String> hashes = new TreeMap<>();
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path p : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                hashes.put(p.toString(), sha256(p));
            }
        }
        return hashes;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(Files.readAllBytes(file));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
