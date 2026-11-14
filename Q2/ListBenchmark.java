import java.io.PrintWriter;
import java.util.*;
import java.util.function.Supplier;

/** Q2 - ArrayList vs LinkedList vs ArrayDeque vs plain array with manual shifting (playlist editor). */
public class ListBenchmark {

    record Song(int id) { }

    static final int K = 1000;          // number of positional operations per measurement
    static final int WARM = 2, RUNS = 5;
    static long sink;

    interface Op { void run(); }
    interface Prep<T> { T make(); }

    /** time of op.run() on a freshly prepared structure; prep time is NOT counted. Returns avg ms. */
    static <T> double time(Prep<T> prep, java.util.function.Consumer<T> op) {
        double total = 0;
        for (int i = 0; i < WARM + RUNS; i++) {
            T s = prep.make();
            long t0 = System.nanoTime();
            op.accept(s);
            long dt = System.nanoTime() - t0;
            if (i >= WARM) total += dt / 1e6;
        }
        return total / RUNS;
    }

    static <L extends List<Song>> L filled(Supplier<L> sup, Song[] songs) {
        L l = sup.get();
        for (Song s : songs) l.add(s);
        return l;
    }

    // ---------- plain array with manual shifting ----------
    static final class ManualArray {
        Song[] a; int size;
        ManualArray(int capacity) { a = new Song[capacity]; }
        void addEnd(Song s) { a[size++] = s; }
        void insert(int idx, Song s) { for (int i = size; i > idx; i--) a[i] = a[i - 1]; a[idx] = s; size++; }
        void removeAt(int idx) { for (int i = idx; i < size - 1; i++) a[i] = a[i + 1]; a[--size] = null; }
        Song get(int i) { return a[i]; }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version") + " | K=" + K + " positional ops per measurement | avg of " + RUNS + " runs after " + WARM + " warm-ups");
        int[] sizes = {10_000, 100_000, 300_000};
        try (PrintWriter csv = new PrintWriter("q2_results.csv")) {
            csv.println("structure,operation,n,avg_ms");
            for (int n : sizes) {
                Song[] songs = new Song[n + K * 4];
                for (int i = 0; i < songs.length; i++) songs[i] = new Song(i);
                Song[] base = Arrays.copyOf(songs, n);
                Song extra = new Song(-1);
                Random rnd = new Random(42);
                int[] idx = new int[K];
                for (int i = 0; i < K; i++) idx[i] = rnd.nextInt(n);
                System.out.println("\n===== playlist size n = " + n + " =====");
                System.out.printf("%-12s %-22s %12s%n", "structure", "operation", "avg ms");

                // ----- ArrayList & LinkedList -----
                for (String name : new String[]{"ArrayList", "LinkedList"}) {
                    Supplier<List<Song>> sup = name.equals("ArrayList") ? ArrayList::new : LinkedList::new;
                    Prep<List<Song>> full = () -> filled(sup, base);
                    Object[][] ops = {
                        {"add at end (n adds)", (Prep<List<Song>>) sup::get, (java.util.function.Consumer<List<Song>>) l -> { for (Song s : base) l.add(s); }},
                        {"insert at front xK", full, (java.util.function.Consumer<List<Song>>) l -> { for (int i = 0; i < K; i++) l.add(0, extra); }},
                        {"remove front xK", full, (java.util.function.Consumer<List<Song>>) l -> { for (int i = 0; i < K; i++) l.remove(0); }},
                        {"insert middle xK", full, (java.util.function.Consumer<List<Song>>) l -> { for (int i = 0; i < K; i++) l.add(l.size() / 2, extra); }},
                        {"remove middle xK", full, (java.util.function.Consumer<List<Song>>) l -> { for (int i = 0; i < K; i++) l.remove(l.size() / 2); }},
                        {"get k-th (random) xK", full, (java.util.function.Consumer<List<Song>>) l -> { long s = 0; for (int i = 0; i < K; i++) s += l.get(idx[i]).id(); sink += s; }},
                        {"iterate whole list", full, (java.util.function.Consumer<List<Song>>) l -> { long s = 0; for (Song x : l) s += x.id(); sink += s; }}};
                    for (Object[] o : ops) {
                        @SuppressWarnings("unchecked") double ms = time((Prep<List<Song>>) o[1], (java.util.function.Consumer<List<Song>>) o[2]);
                        System.out.printf("%-12s %-22s %12.3f%n", name, o[0], ms);
                        csv.printf("%s,%s,%d,%.4f%n", name, o[0], n, ms);
                    }
                }

                // ----- ArrayDeque -----
                Prep<ArrayDeque<Song>> dfull = () -> { ArrayDeque<Song> d = new ArrayDeque<>(); for (Song s : base) d.addLast(s); return d; };
                Object[][] dops = {
                    {"add at end (n adds)", (Prep<ArrayDeque<Song>>) ArrayDeque::new, (java.util.function.Consumer<ArrayDeque<Song>>) d -> { for (Song s : base) d.addLast(s); }},
                    {"insert at front xK", dfull, (java.util.function.Consumer<ArrayDeque<Song>>) d -> { for (int i = 0; i < K; i++) d.addFirst(extra); }},
                    {"remove front xK", dfull, (java.util.function.Consumer<ArrayDeque<Song>>) d -> { for (int i = 0; i < K; i++) d.pollFirst(); }},
                    {"iterate whole list", dfull, (java.util.function.Consumer<ArrayDeque<Song>>) d -> { long s = 0; for (Song x : d) s += x.id(); sink += s; }}};
                for (Object[] o : dops) {
                    @SuppressWarnings("unchecked") double ms = time((Prep<ArrayDeque<Song>>) o[1], (java.util.function.Consumer<ArrayDeque<Song>>) o[2]);
                    System.out.printf("%-12s %-22s %12.3f%n", "ArrayDeque", o[0], ms);
                    csv.printf("ArrayDeque,%s,%d,%.4f%n", o[0], n, ms);
                }

                // ----- plain array, manual shifting -----
                Prep<ManualArray> afull = () -> { ManualArray m = new ManualArray(n + K * 4); for (Song s : base) m.addEnd(s); return m; };
                Object[][] aops = {
                    {"add at end (n adds)", (Prep<ManualArray>) () -> new ManualArray(n + K * 4), (java.util.function.Consumer<ManualArray>) m -> { for (Song s : base) m.addEnd(s); }},
                    {"insert at front xK", afull, (java.util.function.Consumer<ManualArray>) m -> { for (int i = 0; i < K; i++) m.insert(0, extra); }},
                    {"remove front xK", afull, (java.util.function.Consumer<ManualArray>) m -> { for (int i = 0; i < K; i++) m.removeAt(0); }},
                    {"insert middle xK", afull, (java.util.function.Consumer<ManualArray>) m -> { for (int i = 0; i < K; i++) m.insert(m.size / 2, extra); }},
                    {"get k-th (random) xK", afull, (java.util.function.Consumer<ManualArray>) m -> { long s = 0; for (int i = 0; i < K; i++) s += m.get(idx[i]).id(); sink += s; }},
                    {"iterate whole list", afull, (java.util.function.Consumer<ManualArray>) m -> { long s = 0; for (int i = 0; i < m.size; i++) s += m.a[i].id(); sink += s; }}};
                for (Object[] o : aops) {
                    @SuppressWarnings("unchecked") double ms = time((Prep<ManualArray>) o[1], (java.util.function.Consumer<ManualArray>) o[2]);
                    System.out.printf("%-12s %-22s %12.3f%n", "PlainArray", o[0], ms);
                    csv.printf("PlainArray,%s,%d,%.4f%n", o[0], n, ms);
                }
            }

            // ----- "Up Next" queue: mixed workload, addFirst / addLast / removeFirst -----
            System.out.println("\n===== CHANGED CONDITION: 'Up Next' queue, 10000 mixed operations (addLast, addFirst, removeFirst in 1:1:2 ratio) =====");
            System.out.printf("%-12s %12s %12s%n", "structure", "queue size n", "avg ms");
            final int OPS = 10_000;
            for (int n : sizes) {
                Song[] base = new Song[n];
                for (int i = 0; i < n; i++) base[i] = new Song(i);
                Song extra = new Song(-1);
                // ArrayDeque
                double d = time(() -> { ArrayDeque<Song> q = new ArrayDeque<>(); for (Song s : base) q.addLast(s); return q; },
                    q -> { for (int i = 0; i < OPS; i++) { switch (i & 3) { case 0 -> q.addLast(extra); case 1 -> q.addFirst(extra); default -> q.pollFirst(); } } });
                // LinkedList used as a deque
                double ll = time(() -> { LinkedList<Song> q = new LinkedList<>(); for (Song s : base) q.addLast(s); return q; },
                    q -> { for (int i = 0; i < OPS; i++) { switch (i & 3) { case 0 -> q.addLast(extra); case 1 -> q.addFirst(extra); default -> q.pollFirst(); } } });
                // ArrayList used as a deque (add(0,x) / remove(0))
                double al = time(() -> { ArrayList<Song> q = new ArrayList<>(); for (Song s : base) q.add(s); return q; },
                    q -> { for (int i = 0; i < OPS; i++) { switch (i & 3) { case 0 -> q.add(extra); case 1 -> q.add(0, extra); default -> q.remove(0); } } });
                System.out.printf("%-12s %12d %12.3f%n", "ArrayDeque", n, d);
                System.out.printf("%-12s %12d %12.3f%n", "LinkedList", n, ll);
                System.out.printf("%-12s %12d %12.3f%n", "ArrayList", n, al);
                csv.printf("UpNext-ArrayDeque,mixed ops x10000,%d,%.4f%nUpNext-LinkedList,mixed ops x10000,%d,%.4f%nUpNext-ArrayList,mixed ops x10000,%d,%.4f%n", n, d, n, ll, n, al);
            }
        }
        System.out.println("\n(sink=" + sink + " prevents dead-code elimination)");
    }
}
