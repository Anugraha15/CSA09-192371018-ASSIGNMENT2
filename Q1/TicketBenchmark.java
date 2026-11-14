import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.StringJoiner;

/** Q1 - String vs StringBuilder vs StringBuffer (plus join / StringJoiner / format as alternatives). */
public class TicketBenchmark {

    interface Builder { String build(String[] fields); }

    static String[] makeFields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "Field" + i + "=Value" + (i * 7919 % 1000);
        return f;
    }

    // ---- the three required approaches ----
    static String viaPlus(String[] f) { String s = ""; for (String x : f) s = s + x + ";"; return s; }
    static String viaBuilder(String[] f) { StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(x).append(';'); return sb.toString(); }
    static String viaBuffer(String[] f) { StringBuffer sb = new StringBuffer(); for (String x : f) sb.append(x).append(';'); return sb.toString(); }
    // ---- alternatives for the "Alternative Concept" part ----
    static String viaJoin(String[] f) { return String.join(";", f); }
    static String viaJoiner(String[] f) { StringJoiner j = new StringJoiner(";"); for (String x : f) j.add(x); return j.toString(); }
    static String viaFormat(String[] f) { StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(String.format("%s;", x)); return sb.toString(); }

    static final com.sun.management.ThreadMXBean MX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long allocated() { return MX.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    static long sink;

    /** returns {avg ms per run, avg allocated KB per run} */
    static double[] measure(Builder b, String[] f, int tickets, int warm, int runs) {
        for (int w = 0; w < warm; w++) for (int t = 0; t < tickets; t++) sink += b.build(f).length();
        double totalMs = 0; long totalAlloc = 0;
        for (int r = 0; r < runs; r++) {
            long a0 = allocated(); long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += b.build(f).length();
            totalMs += (System.nanoTime() - t0) / 1e6; totalAlloc += allocated() - a0;
        }
        return new double[]{totalMs / runs, totalAlloc / (double) runs / 1024.0};
    }

    /** characters copied by repeated '+' : sum of the length of every intermediate String. */
    static long plusCharsCopied(String[] f) { long cum = 0, total = 0; for (String x : f) { cum += x.length() + 1; total += cum; } return total; }

    /** number of times a StringBuilder (default capacity 16) must grow its internal array for these fields. */
    static int builderResizes(String[] f) {
        StringBuilder sb = new StringBuilder(); int cap = sb.capacity(), resizes = 0;
        for (String x : f) { sb.append(x).append(';'); if (sb.capacity() != cap) { resizes++; cap = sb.capacity(); } }
        return resizes;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version") + " | " + System.getProperty("os.name") + " | cores=" + Runtime.getRuntime().availableProcessors());
        Object[][] methods = {
            {"String +", (Builder) TicketBenchmark::viaPlus}, {"StringBuilder", (Builder) TicketBenchmark::viaBuilder},
            {"StringBuffer", (Builder) TicketBenchmark::viaBuffer}, {"String.join", (Builder) TicketBenchmark::viaJoin},
            {"StringJoiner", (Builder) TicketBenchmark::viaJoiner}, {"String.format", (Builder) TicketBenchmark::viaFormat}};
        // workload name, fields per ticket, number of tickets
        Object[][] workloads = {
            {"W1 short ticket (10 fields) x 50000 tickets", 10, 50000},
            {"W2 normal ticket (50 fields) x 10000 tickets", 50, 10000},
            {"W3a giant ticket 5000 fields x 1", 5000, 1}, {"W3b giant ticket 10000 fields x 1", 10000, 1},
            {"W3c giant ticket 20000 fields x 1", 20000, 1}, {"W3d giant ticket 40000 fields x 1", 40000, 1}};
        try (PrintWriter csv = new PrintWriter("q1_results.csv")) {
            csv.println("workload,fields,tickets,method,avg_ms_per_run,avg_alloc_KB_per_run");
            for (Object[] w : workloads) {
                int n = (Integer) w[1], tickets = (Integer) w[2];
                String[] f = makeFields(n);
                System.out.println("\n== " + w[0] + " ==");
                for (Object[] m : methods) {
                    double[] r = measure((Builder) m[1], f, tickets, 2, 5);
                    System.out.printf("%-14s %12.3f ms/run %14.1f KB allocated/run%n", m[0], r[0], r[1]);
                    csv.printf("\"%s\",%d,%d,%s,%.4f,%.1f%n", w[0], n, tickets, m[0], r[0], r[1]);
                }
            }
        }
        System.out.println("\n== Analytical counts per ticket ==");
        try (PrintWriter csv = new PrintWriter("q1_counts.csv")) {
            csv.println("fields,intermediate_strings_plus,chars_copied_plus,final_length,builder_resizes");
            for (int n : new int[]{10, 50, 5000, 10000, 20000, 40000}) {
                String[] f = makeFields(n);
                long cp = plusCharsCopied(f); int len = viaBuilder(f).length(); int rs = builderResizes(f);
                System.out.printf("n=%6d  '+' creates %6d intermediate Strings, copies %,15d chars | final length %,9d | StringBuilder resizes: %d%n", n, n, cp, len, rs);
                csv.printf("%d,%d,%d,%d,%d%n", n, n, cp, len, rs);
            }
        }
        // memory before/after using Runtime (as the question asks) for the giant ticket
        String[] f = makeFields(20000);
        for (String name : new String[]{"String +", "StringBuilder"}) {
            System.gc(); Thread.sleep(200);
            Runtime rt = Runtime.getRuntime(); long before = rt.totalMemory() - rt.freeMemory();
            String s = name.equals("String +") ? viaPlus(f) : viaBuilder(f);
            long after = rt.totalMemory() - rt.freeMemory();
            System.out.printf("Runtime memory (20000 fields) %-14s before=%,d B after=%,d B delta=%,d B (result length %d)%n", name, before, after, after - before, s.length());
        }
    }
}
