import java.util.*;

/** Q5 - test data with ties at every level, and data designed to break faulty comparators. */
public class ComparatorTests {

    static final Comparator<Team> CORRECT = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    /** FAULT 1: compares by SUBTRACTION - overflows for large values, breaking transitivity. */
    static final Comparator<Team> SUBTRACTION = (a, b) -> {
        if (b.points != a.points) return b.points - a.points;
        if (b.goalDiff != a.goalDiff) return b.goalDiff - a.goalDiff;
        return a.name.compareTo(b.name);
    };
    /** FAULT 2: forgets the last tie-breaker (name) - teams that tie on both numbers compare as equal although they are different. */
    static final Comparator<Team> NO_NAME = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
    /** FAULT 3: goal difference sorted ascending by mistake. */
    static final Comparator<Team> WRONG_DIRECTION = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparingInt(t -> t.goalDiff).thenComparing(t -> t.name);

    /** brute-force check of the principle on a sample: antisymmetry, transitivity, and 'equal items are interchangeable'. */
    static String check(Comparator<Team> c, List<Team> s) {
        int anti = 0, trans = 0, subst = 0; String firstTrans = "";
        for (Team a : s) for (Team b : s) if (Integer.signum(c.compare(a, b)) != -Integer.signum(c.compare(b, a))) anti++;
        for (Team a : s) for (Team b : s) for (Team d : s) {
            if (c.compare(a, b) < 0 && c.compare(b, d) < 0 && c.compare(a, d) >= 0) { trans++; if (firstTrans.isEmpty()) firstTrans = a + " < " + b + " < " + d + " but compare(a,c)=" + c.compare(a, d); }
            if (c.compare(a, b) == 0 && Integer.signum(c.compare(a, d)) != Integer.signum(c.compare(b, d))) subst++;
        }
        return "antisymmetry violations=" + anti + ", transitivity violations=" + trans + ", equal-substitution violations=" + subst + (trans > 0 ? "\n      e.g. " + firstTrans : "");
    }

    public static void main(String[] args) {
        // ---------- DATA SET 1: several ties at every level ----------
        List<Team> ties = List.of(new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1), new Team("Bisons", 9, 4),
            new Team("Dragons", 9, 6), new Team("Falcons", 6, 1), new Team("Aces", 6, 3), new Team("Zebras", 3, 0), new Team("Wolves", 3, 0), new Team("Hawks", 9, 6));
        System.out.println("DATA SET 1 (ties at points, goal difference and name level)");
        Leaderboard<Team> b = new Leaderboard<>(CORRECT); ties.forEach(b::add);
        System.out.println("  correct comparator : " + b.ranking());
        System.out.println("  hand-derived order : [Dragons(9,6), Eagles(9,6), Hawks(9,6), Bisons(9,4), Panthers(9,4), Aces(6,3), Cobras(6,1), Falcons(6,1), Wolves(3,0), Zebras(3,0)]");
        List<Team> r1 = b.ranking(); r1.clear();                 // mutate the returned copy ...
        System.out.println("  ranking() returns an independent copy (clearing it leaves the board intact): " + (b.ranking().size() == ties.size()));
        Leaderboard<Team> bn = new Leaderboard<>(NO_NAME); ties.forEach(bn::add);
        System.out.println("  FAULT 2 (no name tie-break): " + bn.ranking() + "   <- Panthers before Bisons, only insertion order decides");
        Leaderboard<Team> bw = new Leaderboard<>(WRONG_DIRECTION); ties.forEach(bw::add);
        System.out.println("  FAULT 3 (goal diff ascending): " + bw.ranking());

        // ---------- DATA SET 2: designed to break the subtraction comparator ----------
        List<Team> extreme = List.of(new Team("A", 0, 2_000_000_000), new Team("B", 0, -2_000_000_000), new Team("C", 0, 0),
                                     new Team("D", 2_000_000_000, 1), new Team("E", -2_000_000_000, 1), new Team("F", 0, 1));
        System.out.println("\nDATA SET 2 (large values: goal difference +-2,000,000,000 and points +-2,000,000,000)");
        System.out.println("  CORRECT      : " + check(CORRECT, extreme));
        System.out.println("  SUBTRACTION  : " + check(SUBTRACTION, extreme));
        System.out.println("  NO_NAME      : " + check(NO_NAME, ties) + "  (consistent but INCOMPLETE: teams tied on both numbers compare as equal, so their order depends on insertion order)");
        System.out.println("  WRONG_DIR    : " + check(WRONG_DIRECTION, ties) + "  (consistent, but ranks the wrong way - consistency alone is not correctness)");
        int thrown = 0; int trials = 20;
        for (int seed = 0; seed < trials; seed++) {
            Random r = new Random(seed); List<Team> big = new ArrayList<>();
            int[] vals = {2_000_000_000, -2_000_000_000, 0, 1_500_000_000, -1_500_000_000};
            for (int i = 0; i < 500; i++) big.add(new Team("T" + i, vals[r.nextInt(5)], vals[r.nextInt(5)]));
            try { new ArrayList<>(big).sort(SUBTRACTION); } catch (IllegalArgumentException e) { thrown++; if (thrown == 1) System.out.println("  sorting 500 teams with SUBTRACTION threw: " + e.getMessage()); }
            new ArrayList<>(big).sort(CORRECT);   // never throws
        }
        System.out.println("  SUBTRACTION threw IllegalArgumentException in " + thrown + " of " + trials + " random 500-team sorts; CORRECT threw in 0.");
    }
}
