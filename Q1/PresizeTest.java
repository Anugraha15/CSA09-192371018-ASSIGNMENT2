/** Q1 extra: does pre-sizing the StringBuilder explain why '+' looked competitive on the SHORT ticket (10 fields)? */
public class PresizeTest {
    static long sink;
    interface B { String build(String[] f); }
    public static void main(String[] args) {
        String[] f = TicketBenchmark.makeFields(10);
        int tickets = 50000;
        Object[][] m = {
            {"String +", (B) TicketBenchmark::viaPlus},
            {"StringBuilder (default capacity 16)", (B) TicketBenchmark::viaBuilder},
            {"StringBuilder (capacity 256)", (B) x -> { StringBuilder sb = new StringBuilder(256); for (String s : x) sb.append(s).append(';'); return sb.toString(); }},
            {"StringBuffer (capacity 256)", (B) x -> { StringBuffer sb = new StringBuffer(256); for (String s : x) sb.append(s).append(';'); return sb.toString(); }}};
        System.out.println("Short ticket: 10 fields x " + tickets + " tickets, avg of 7 runs after 3 warm-ups");
        for (Object[] o : m) {
            B b = (B) o[1]; double tot = 0;
            for (int r = 0; r < 10; r++) {
                long t0 = System.nanoTime(); for (int t = 0; t < tickets; t++) sink += b.build(f).length(); long dt = System.nanoTime() - t0;
                if (r >= 3) tot += dt / 1e6;
            }
            System.out.printf("%-38s %8.3f ms/run%n", o[0], tot / 7);
        }
    }
}
