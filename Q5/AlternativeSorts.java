import java.util.*;

/** Q5 alternative concept: Comparator chain (1 sort) vs Comparable on Team (1 sort) vs three stable sorts (3 sorts). */
public class AlternativeSorts {
    static final class RTeam implements Comparable<RTeam> {
        final String name; final int points, goalDiff;
        RTeam(String n, int p, int g) { name = n; points = p; goalDiff = g; }
        @Override public int compareTo(RTeam o) {                   // natural order = "rank order"
            int c = Integer.compare(o.points, points); if (c != 0) return c;
            c = Integer.compare(o.goalDiff, goalDiff); if (c != 0) return c;
            return name.compareTo(o.name);
        }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }
    static long sink;

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version") + " | avg of 5 runs after 2 warm-ups");
        System.out.printf("%-10s %-34s %10s%n", "n", "approach", "avg ms");
        Comparator<RTeam> chain = Comparator.comparingInt((RTeam t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((RTeam t) -> t.goalDiff).reversed()).thenComparing(t -> t.name);
        for (int n : new int[]{10_000, 100_000, 500_000}) {
            Random r = new Random(3); List<RTeam> data = new ArrayList<>();
            for (int i = 0; i < n; i++) data.add(new RTeam("Team" + r.nextInt(n), r.nextInt(30), r.nextInt(20) - 10));
            List<RTeam> ref = new ArrayList<>(data); ref.sort(chain);
            Object[][] tests = {{"Comparator chain (1 sort)", 1}, {"Comparable.compareTo (1 sort)", 2}, {"3 stable sorts name>gd>points", 3}};
            for (Object[] t : tests) {
                double tot = 0; boolean same = true;
                for (int run = 0; run < 7; run++) {
                    List<RTeam> c = new ArrayList<>(data);
                    long t0 = System.nanoTime();
                    switch ((Integer) t[1]) {
                        case 1 -> c.sort(chain);
                        case 2 -> Collections.sort(c);
                        default -> {
                            c.sort(Comparator.comparing((RTeam x) -> x.name));
                            c.sort(Comparator.comparingInt((RTeam x) -> x.goalDiff).reversed());
                            c.sort(Comparator.comparingInt((RTeam x) -> x.points).reversed());
                        }
                    }
                    long dt = System.nanoTime() - t0;
                    if (run >= 2) tot += dt / 1e6;
                    if (run == 6) same = c.equals(ref) || sameOrder(c, ref);
                    sink += c.size();
                }
                System.out.printf("%-10d %-34s %10.2f   same result as chain: %b%n", n, t[0], tot / 5, same);
            }
        }
    }
    static boolean sameOrder(List<RTeam> a, List<RTeam> b) { for (int i = 0; i < a.size(); i++) if (a.get(i) != b.get(i)) return false; return true; }
}
