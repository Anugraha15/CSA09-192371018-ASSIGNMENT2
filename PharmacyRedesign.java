import java.io.PrintWriter;
import java.util.*;

/**
 * Q7 - Diagnose, Redesign, Validate  (based on Q4: pharmacy stock lookup + evening code-order report).
 *
 * ORIGINAL   : HashMap<String,Medicine>. Evening report = copy all keys + full sort  -> O(n log n) every evening.
 * REDESIGN   : same HashMap (O(1) lookups kept) + a cached sorted array + a small 'pending' list of codes added since the
 *              last report. Evening report = sort ONLY the k new codes (k log k) and MERGE with the cached array (n + k).
 * NAIVE SWAP : TreeMap<String,Medicine> (report is free, but every lookup / insert pays O(log n)).
 */
public class PharmacyRedesign {

    static final class Medicine { final String code; int stock; Medicine(String c, int s) { code = c; stock = s; } }

    // ------------------------------------------------------------------ ORIGINAL
    static final class OriginalPharmacy {
        final Map<String, Medicine> map = new HashMap<>();
        void add(Medicine m) { map.put(m.code, m); }
        Medicine get(String code) { return map.get(code); }
        String[] codeOrderReport(Comparator<String> cmp) {
            String[] k = map.keySet().toArray(new String[0]);
            Arrays.sort(k, cmp);                                   // full re-sort EVERY time
            return k;
        }
    }

    // ------------------------------------------------------------------ REDESIGN
    static final class IncrementalPharmacy {
        final Map<String, Medicine> map = new HashMap<>();
        private String[] sorted = new String[0];                   // cached code-ordered index (valid up to the last report)
        private final ArrayList<String> pending = new ArrayList<>();// codes added since the last report
        private boolean removedSince = false;

        void add(Medicine m) { if (map.put(m.code, m) == null) pending.add(m.code); }   // only NEW codes become pending
        Medicine get(String code) { return map.get(code); }
        void remove(String code) { if (map.remove(code) != null) removedSince = true; }

        String[] codeOrderReport(Comparator<String> cmp) {
            if (pending.isEmpty() && !removedSince) return sorted;          // nothing changed: O(1)
            String[] add = pending.toArray(new String[0]);
            Arrays.sort(add, cmp);                                          // k log k
            String[] out = new String[map.size()]; int o = 0, i = 0, j = 0;
            String last = null;
            while (i < sorted.length || j < add.length) {                   // linear merge: n + k comparisons
                String next;
                if (j >= add.length || (i < sorted.length && cmp.compare(sorted[i], add[j]) <= 0)) next = sorted[i++]; else next = add[j++];
                if (!map.containsKey(next)) continue;                       // dropped (removed) code
                if (next.equals(last)) continue;                            // removed-then-re-added duplicate
                out[o++] = next; last = next;
            }
            sorted = (o == out.length) ? out : Arrays.copyOf(out, o);
            pending.clear(); removedSince = false;
            return sorted;
        }
    }

    // ------------------------------------------------------------------ helpers
    static long sink;
    static final Comparator<String> PLAIN = String::compareTo;
    static final class Counting implements Comparator<String> { long n; public int compare(String a, String b) { n++; return a.compareTo(b); } }

    static String code(int x) { return String.format("MED%07d", x); }

    /** generates the codes of day d: 'k' fresh unique codes, in random order */
    static List<String> fresh(int start, int k, Random r) { List<String> l = new ArrayList<>(); for (int i = 0; i < k; i++) l.add(code((start + i) * 3 + 11)); Collections.shuffle(l, r); return l; }

    interface Task { void run(); }
    static double ms(Task t) { long t0 = System.nanoTime(); t.run(); return (System.nanoTime() - t0) / 1e6; }

    /** one scenario: n0 medicines initially, k new per day, 'days' evenings. Returns {orig ms, redesign ms, treemap ms, origCmp, redCmp} */
    static double[] scenario(int n0, int k, int days, boolean verify) {
        Random r = new Random(11);
        List<String> initial = fresh(0, n0, r);
        List<List<String>> dayCodes = new ArrayList<>();
        for (int d = 0; d < days; d++) dayCodes.add(fresh(n0 + d * k, k, r));

        OriginalPharmacy o = new OriginalPharmacy(); IncrementalPharmacy g = new IncrementalPharmacy(); TreeMap<String, Medicine> t = new TreeMap<>();
        for (String c : initial) { Medicine m = new Medicine(c, 10); o.add(m); g.add(m); t.put(c, m); }
        // the first report (before the measured days) builds the cached index of the redesign: one-off cost, same as the original's cost
        o.codeOrderReport(PLAIN); g.codeOrderReport(PLAIN);

        double oMs = 0, gMs = 0, tMs = 0;
        for (int d = 0; d < days; d++) {
            List<String> codes = dayCodes.get(d);
            for (String c : codes) { Medicine m = new Medicine(c, 5); o.add(m); g.add(m); }
            double tIns = ms(() -> { for (String c : codes) t.put(c, new Medicine(c, 5)); });
            final String[][] ro = new String[1][], rg = new String[1][];
            oMs += ms(() -> ro[0] = o.codeOrderReport(PLAIN));
            gMs += ms(() -> rg[0] = g.codeOrderReport(PLAIN));
            tMs += tIns + ms(() -> sink += t.keySet().toArray(new String[0]).length);
            if (verify && !Arrays.equals(ro[0], rg[0])) throw new IllegalStateException("reports differ on day " + d);
        }
        // count comparisons on a fresh replay (separate run, so the counting comparator does not disturb the timings)
        Counting co = new Counting(), cg = new Counting();
        OriginalPharmacy o2 = new OriginalPharmacy(); IncrementalPharmacy g2 = new IncrementalPharmacy();
        for (String c : initial) { Medicine m = new Medicine(c, 10); o2.add(m); g2.add(m); }
        o2.codeOrderReport(new Counting()); g2.codeOrderReport(new Counting());
        for (int d = 0; d < days; d++) { for (String c : dayCodes.get(d)) { Medicine m = new Medicine(c, 5); o2.add(m); g2.add(m); } o2.codeOrderReport(co); g2.codeOrderReport(cg); }
        return new double[]{oMs, gMs, tMs, co.n, cg.n};
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version"));

        // ---------- correctness: add, update, remove, re-add, report twice ----------
        System.out.println("\n== CORRECTNESS OF THE REDESIGN (compared with the original after every step) ==");
        OriginalPharmacy o = new OriginalPharmacy(); IncrementalPharmacy g = new IncrementalPharmacy(); Random r = new Random(2);
        boolean ok = true; int checks = 0;
        for (int round = 0; round < 200; round++) {
            for (int i = 0; i < 1 + r.nextInt(30); i++) { String c = code(r.nextInt(500)); Medicine m = new Medicine(c, round); o.add(m); g.add(m); }       // adds + updates of existing codes
            if (r.nextInt(3) == 0) { String c = code(r.nextInt(500)); o.map.remove(c); g.remove(c); }                                                       // removals (then possible re-add later)
            if (r.nextInt(5) != 0) { ok &= Arrays.equals(o.codeOrderReport(PLAIN), g.codeOrderReport(PLAIN)); checks++; ok &= Arrays.equals(o.codeOrderReport(PLAIN), g.codeOrderReport(PLAIN)); checks++; }
        }
        System.out.println("  " + checks + " report comparisons (random adds / updates / removals / re-adds): all identical = " + ok);

        // ---------- A. diagnosis: where does the time go in the ORIGINAL? ----------
        System.out.println("\n== A. DIAGNOSIS: cost of ONE evening report in the original design (HashMap + full sort) ==");
        System.out.printf("%-10s %-22s %-22s %-18s%n", "n", "100k lookups (ms)", "evening report (ms)", "report / lookups");
        for (int n : new int[]{50_000, 200_000, 500_000}) {
            OriginalPharmacy p = new OriginalPharmacy(); List<String> codes = fresh(0, n, new Random(1));
            for (String c : codes) p.add(new Medicine(c, 1));
            double look = 0, rep = 0;
            for (int run = 0; run < 7; run++) {
                double l = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += p.get(codes.get(i % n)).stock; sink += s; });
                double q = ms(() -> sink += p.codeOrderReport(PLAIN).length);
                if (run >= 2) { look += l; rep += q; }
            }
            System.out.printf("%-10d %-22.2f %-22.2f %-18.1fx%n", n, look / 5, rep / 5, rep / look);
        }

        // ---------- E. validation ----------
        System.out.println("\n== E. VALIDATION: total report cost over 10 evenings (k new medicines each evening); same data for every design ==");
        System.out.printf("%-9s %-8s | %-14s %-14s %-8s | %-14s | %-16s %-16s%n", "n0", "k/day", "ORIGINAL ms", "REDESIGN ms", "speed-up", "TreeMap ms*", "comparisons orig", "comparisons new");
        try (PrintWriter csv = new PrintWriter("q7_results.csv")) {
            csv.println("n0,k_per_day,original_ms,redesign_ms,speedup,treemap_insert_plus_report_ms,original_comparisons,redesign_comparisons");
            int[][] cfg = {{50_000, 500}, {200_000, 100}, {200_000, 500}, {200_000, 5_000}, {200_000, 50_000}, {200_000, 200_000}, {500_000, 500}};
            for (int[] c : cfg) {
                double[] acc = new double[5]; int reps = 3;
                scenario(c[0], c[1], 3, true);   // warm-up (+ equality check)
                for (int i = 0; i < reps; i++) { double[] s = scenario(c[0], c[1], 10, i == 0); for (int j = 0; j < 5; j++) acc[j] += s[j] / reps; }
                System.out.printf("%-9d %-8d | %-14.1f %-14.1f %-7.1fx | %-14.1f | %,16.0f %,16.0f%n", c[0], c[1], acc[0], acc[1], acc[0] / acc[1], acc[2], acc[3], acc[4]);
                csv.printf("%d,%d,%.2f,%.2f,%.2f,%.2f,%.0f,%.0f%n", c[0], c[1], acc[0], acc[1], acc[0] / acc[1], acc[2], acc[3], acc[4]);
            }
        }
        System.out.println("  * TreeMap column = time of the k inserts + one in-order walk per evening, summed over 10 evenings.");

        // ---------- lookups unchanged? and the cost of the naive swap ----------
        System.out.println("\n== LOOKUPS: 100,000 lookups (ms). The redesign keeps the HashMap, the naive swap does not ==");
        System.out.printf("%-10s %-14s %-14s %-14s%n", "n", "original", "redesign", "TreeMap");
        for (int n : new int[]{50_000, 200_000, 500_000}) {
            List<String> codes = fresh(0, n, new Random(1));
            OriginalPharmacy p = new OriginalPharmacy(); IncrementalPharmacy q = new IncrementalPharmacy(); TreeMap<String, Medicine> t = new TreeMap<>();
            for (String c : codes) { Medicine m = new Medicine(c, 1); p.add(m); q.add(m); t.put(c, m); }
            q.codeOrderReport(PLAIN);
            double a = 0, b = 0, d = 0;
            for (int run = 0; run < 7; run++) {
                double x = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += p.get(codes.get((i * 7) % n)).stock; sink += s; });
                double y = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += q.get(codes.get((i * 7) % n)).stock; sink += s; });
                double z = ms(() -> { long s = 0; for (int i = 0; i < 100_000; i++) s += t.get(codes.get((i * 7) % n)).stock; sink += s; });
                if (run >= 2) { a += x; b += y; d += z; }
            }
            System.out.printf("%-10d %-14.2f %-14.2f %-14.2f%n", n, a / 5, b / 5, d / 5);
        }

        // ---------- memory ----------
        System.out.println("\n== MEMORY: extra memory held by the redesign (n = 500,000) ==");
        List<String> codes = fresh(0, 500_000, new Random(1)); List<Medicine> meds = new ArrayList<>(); for (String c : codes) meds.add(new Medicine(c, 1));
        System.gc(); Thread.sleep(150); Runtime rt = Runtime.getRuntime(); long b0 = rt.totalMemory() - rt.freeMemory();
        OriginalPharmacy p = new OriginalPharmacy(); for (Medicine m : meds) p.add(m);
        System.gc(); Thread.sleep(150); long b1 = rt.totalMemory() - rt.freeMemory();
        IncrementalPharmacy q = new IncrementalPharmacy(); for (Medicine m : meds) q.add(m); q.codeOrderReport(PLAIN);
        System.gc(); Thread.sleep(150); long b2 = rt.totalMemory() - rt.freeMemory();
        System.out.printf("  original (HashMap only)            : %.1f MB%n  redesign (HashMap + sorted index)   : %.1f MB  -> extra %.1f MB (one reference per medicine)%n", (b1 - b0) / 1048576.0, (b2 - b1) / 1048576.0, ((b2 - b1) - (b1 - b0)) / 1048576.0);
        sink += p.map.size() + q.map.size();
        System.out.println("(sink=" + sink + ")");
    }
}
