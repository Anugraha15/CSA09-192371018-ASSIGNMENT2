import java.io.PrintWriter;
import java.util.*;

/** Q4 - HashMap vs TreeMap vs LinkedHashMap (vs Hashtable, vs sorted array + binarySearch) for a pharmacy stock lookup. */
public class PharmacyMaps {

    static final class Medicine {
        final String code; final String name; int stock; final int seq;   // seq = order in which the medicine was added
        Medicine(String code, String name, int stock, int seq) { this.code = code; this.name = name; this.stock = stock; this.seq = seq; }
    }
    static long sink;
    static final int WARM = 2, RUNS = 5, LOOKUPS = 100_000;

    interface Task { void run(); }
    static double avgMs(Task t) {
        double tot = 0;
        for (int i = 0; i < WARM + RUNS; i++) { long t0 = System.nanoTime(); t.run(); long dt = System.nanoTime() - t0; if (i >= WARM) tot += dt / 1e6; }
        return tot / RUNS;
    }

    static Map<String, Medicine> build(String type, Medicine[] meds) {
        Map<String, Medicine> m = switch (type) {
            case "HashMap" -> new HashMap<>(); case "TreeMap" -> new TreeMap<>();
            case "LinkedHashMap" -> new LinkedHashMap<>(); default -> new Hashtable<>(); };
        for (Medicine x : meds) m.put(x.code, x);
        return m;
    }

    /** report of codes in ascending code order. Natural for TreeMap; others must sort. */
    static String[] codeOrderReport(String type, Map<String, Medicine> m) {
        if (type.equals("TreeMap")) return m.keySet().toArray(new String[0]);        // already sorted: just walk it
        String[] k = m.keySet().toArray(new String[0]); Arrays.sort(k); return k;      // extra work: O(n log n)
    }
    /** report of codes in insertion order. Natural for LinkedHashMap; others must sort by the 'seq' field. */
    static String[] addedOrderReport(String type, Map<String, Medicine> m) {
        if (type.equals("LinkedHashMap")) return m.keySet().toArray(new String[0]);
        Medicine[] a = m.values().toArray(new Medicine[0]);
        Arrays.sort(a, Comparator.comparingInt(x -> x.seq));                           // extra work: O(n log n)
        String[] k = new String[a.length]; for (int i = 0; i < a.length; i++) k[i] = a[i].code; return k;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version") + " | " + LOOKUPS + " lookups per measurement | avg of " + RUNS + " runs after " + WARM + " warm-ups");
        try (PrintWriter csv = new PrintWriter("q4_results.csv")) {
            csv.println("structure,n,put_all_ms,lookups_100k_ms,report_code_order_ms,report_added_order_ms,code_order_ok,added_order_ok,retained_MB");
            for (int n : new int[]{10_000, 100_000, 500_000}) {
                // unique codes "MED" + 7 digits, added in random (not sorted) order
                List<Integer> nums = new ArrayList<>(); for (int i = 0; i < n; i++) nums.add(i * 3 + 11);
                Collections.shuffle(nums, new Random(1));
                Medicine[] meds = new Medicine[n]; String[] added = new String[n];
                for (int i = 0; i < n; i++) { String c = String.format("MED%07d", nums.get(i)); meds[i] = new Medicine(c, "Drug-" + i, 100 + i % 50, i); added[i] = c; }
                String[] sortedCodes = added.clone(); Arrays.sort(sortedCodes);
                Random r = new Random(5); String[] queries = new String[LOOKUPS];
                for (int i = 0; i < LOOKUPS; i++) queries[i] = added[r.nextInt(n)];

                System.out.println("\n===== n = " + n + " medicines =====");
                System.out.printf("%-14s %10s %12s %14s %14s %8s %8s %10s%n", "structure", "put all", "100k lookups", "code-order rpt", "added-order rpt", "code ok", "add ok", "retained MB");
                for (String type : new String[]{"HashMap", "TreeMap", "LinkedHashMap", "Hashtable"}) {
                    double put = avgMs(() -> sink += build(type, meds).size());
                    Map<String, Medicine> m = build(type, meds);
                    double look = avgMs(() -> { long s = 0; for (String q : queries) s += m.get(q).stock; sink += s; });
                    double rc = avgMs(() -> sink += codeOrderReport(type, m).length);
                    double ra = avgMs(() -> sink += addedOrderReport(type, m).length);
                    boolean okC = Arrays.equals(codeOrderReport(type, m), sortedCodes);
                    boolean okA = Arrays.equals(addedOrderReport(type, m), added);
                    // retained memory of the map structure only (medicines and keys already exist)
                    System.gc(); Thread.sleep(100);
                    Runtime rt = Runtime.getRuntime(); long b = rt.totalMemory() - rt.freeMemory();
                    Map<String, Medicine> m2 = build(type, meds);
                    System.gc(); Thread.sleep(100);
                    long a = rt.totalMemory() - rt.freeMemory(); sink += m2.size();
                    double mb = (a - b) / 1048576.0;
                    System.out.printf("%-14s %10.2f %12.2f %14.2f %14.2f %8s %8s %10.1f%n", type, put, look, rc, ra, okC, okA, mb);
                    csv.printf("%s,%d,%.3f,%.3f,%.3f,%.3f,%b,%b,%.1f%n", type, n, put, look, rc, ra, okC, okA, mb);
                }
                // ----- alternative concept: sorted array of medicines + Arrays.binarySearch -----
                Medicine[] byCode = meds.clone(); Arrays.sort(byCode, Comparator.comparing(x -> x.code));
                String[] keys = new String[n]; for (int i = 0; i < n; i++) keys[i] = byCode[i].code;
                double look = avgMs(() -> { long s = 0; for (String q : queries) s += byCode[Arrays.binarySearch(keys, q)].stock; sink += s; });
                // insertion: 1000 new medicines, each found by binary search and inserted with arraycopy (O(n) shift)
                final int INS = 1000;
                String[] newCodes = new String[INS]; for (int i = 0; i < INS; i++) newCodes[i] = String.format("NEW%07d", i * 7);
                double ins = avgMs(() -> {
                    String[] k = Arrays.copyOf(keys, n + INS); Medicine[] v = Arrays.copyOf(byCode, n + INS); int size = n;
                    for (String c : newCodes) {
                        int p = -Arrays.binarySearch(k, 0, size, c) - 1;
                        System.arraycopy(k, p, k, p + 1, size - p); System.arraycopy(v, p, v, p + 1, size - p);
                        k[p] = c; v[p] = null; size++;
                    }
                    sink += size;
                });
                double rep = avgMs(() -> sink += keys.clone().length);   // report = walk the already sorted array
                // map equivalent: 1000 inserts into HashMap / TreeMap
                Map<String, Medicine> hm = build("HashMap", meds), tm = build("TreeMap", meds);
                double insH = avgMs(() -> { for (String c : newCodes) hm.put(c, null); for (String c : newCodes) hm.remove(c); });
                double insT = avgMs(() -> { for (String c : newCodes) tm.put(c, null); for (String c : newCodes) tm.remove(c); });
                System.out.printf("%-14s lookups(100k)=%.2f ms | 1000 inserts=%.2f ms | code-order report=%.2f ms   (HashMap 1000 put+remove=%.2f ms, TreeMap=%.2f ms)%n",
                    "SortedArray", look, ins, rep, insH, insT);
                csv.printf("SortedArray,%d,,%.3f,%.3f,,true,,%n", n, look, rep);
                csv.printf("SortedArray_1000_inserts,%d,%.3f,,,,,,%nHashMap_1000_put_remove,%d,%.3f,,,,,,%nTreeMap_1000_put_remove,%d,%.3f,,,,,,%n", n, ins, n, insH, n, insT);
            }
        }
        System.out.println("\n(sink=" + sink + ")");
    }
}
