/** Q1 design decision - shared booking log appended by several threads: StringBuilder vs StringBuffer. */
public class ThreadSafetyDemo {

    interface Log { void add(String s); int length(); }

    /** runs the threads and returns how many of them died with an exception */
    static int run(Log log, int threads, int perThread) throws Exception {
        Thread[] ts = new Thread[threads];
        final int[] failed = {0};
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                try { for (int j = 0; j < perThread; j++) log.add("x"); }
                catch (RuntimeException e) { synchronized (failed) { failed[0]++; } }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        return failed[0];
    }

    public static void main(String[] args) throws Exception {
        int threads = 4, per = 100000, expected = threads * per, trials = 10;
        System.out.println("Each trial: " + threads + " threads x " + per + " appends -> expected length " + expected);
        for (String kind : new String[]{"StringBuilder", "StringBuffer"}) {
            int wrong = 0, crashed = 0; long minLen = Long.MAX_VALUE;
            for (int t = 0; t < trials; t++) {
                final StringBuilder sb = new StringBuilder();
                final StringBuffer sf = new StringBuffer();
                Log log = kind.equals("StringBuilder")
                    ? new Log() { public void add(String s) { sb.append(s); } public int length() { return sb.length(); } }
                    : new Log() { public void add(String s) { sf.append(s); } public int length() { return sf.length(); } };
                if (run(log, threads, per) > 0) crashed++;
                if (log.length() != expected) { wrong++; minLen = Math.min(minLen, log.length()); }
            }
            System.out.printf("%-14s trials=%d  wrong-length=%d  trials-with-crashed-thread=%d  %s%n", kind, trials, wrong, crashed,
                wrong > 0 ? "shortest length seen=" + minLen : "always correct");
        }
    }
}
