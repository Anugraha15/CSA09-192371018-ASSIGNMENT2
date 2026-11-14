# CSA09 – Programming in Java – Assignment 2 (CO2)

| | |
|---|---|
| **Name** | Anugraha |
| **Register number** | 192371018 |
| **Institute** | Saveetha Institute of Medical and Technical Sciences |
| **Java version used** | 21.0.12 (OpenJDK 21), Linux, 1 CPU core, 4 GB RAM — *update this line if you re-run on your own machine* |
| **Repository** | `CSA09-192371018-Assignment2` |

## Folder layout
Each task has its own folder with the source code, the raw console output (`*_output.txt`), the raw measurements (`*.csv`) and a `screenshots/` folder for the output screenshots.

| Folder | Task | Main programs |
|---|---|---|
| `Q1` | String vs StringBuilder vs StringBuffer | `TicketBenchmark`, `ThreadSafetyDemo`, `PresizeTest` |
| `Q2` | ArrayList vs LinkedList vs ArrayDeque | `ListBenchmark` |
| `Q3` | hashCode–equals contract | `before/Attendance`, `after/Attendance`, `tests/ContractTests`, `tests/DedupBenchmark` |
| `Q4` | HashMap / TreeMap / LinkedHashMap | `PharmacyMaps` |
| `Q5` | Generic ranking component | `LeaderboardDemo`, `ComparatorTests`, `GenericExtras`, `AlternativeSorts` |
| `Q6` | PriorityQueue triage | `Triage`, `TriageStudy` |
| `Q7` | Diagnose, redesign, validate (based on Q4) | `PharmacyRedesign` |

## How to compile and run
Requires a JDK (11 or newer; `javac` and `java` on the PATH).

**Everything at once**
* Linux / macOS / Git-Bash: `./run_all.sh` (or e.g. `./run_all.sh Q4 Q6` for some tasks)
* Windows: double-click or run `run_all.bat`

**One task at a time (example for Q4)**
```
cd Q4
javac -d out PharmacyMaps.java
java -Xmx2g -cp out PharmacyMaps
```

| Task | Compile | Run |
|---|---|---|
| Q1 | `cd Q1` · `javac -d out *.java` | `java -cp out TicketBenchmark` · `java -cp out ThreadSafetyDemo` · `java -cp out PresizeTest` |
| Q2 | `cd Q2` · `javac -d out *.java` | `java -cp out ListBenchmark` |
| Q3 | `cd Q3` · `javac -d out_b before/Attendance.java` · `javac -d out_a after/Attendance.java` · `javac -d out_t tests/*.java` | `java -cp out_b Attendance` · `java -cp out_a Attendance` · `java -cp out_t ContractTests` · `java -cp out_t DedupBenchmark` |
| Q4 | `cd Q4` · `javac -d out *.java` | `java -Xmx2g -cp out PharmacyMaps` |
| Q5 | `cd Q5` · `javac -d out *.java` | `java -cp out LeaderboardDemo` · `ComparatorTests` · `GenericExtras` · `AlternativeSorts` |
| Q6 | `cd Q6` · `javac -d out *.java` | `java -cp out Triage` · `java -cp out TriageStudy` |
| Q7 | `cd Q7` · `javac -d out *.java` | `java -Xmx3g -cp out PharmacyRedesign` |

Timing results change a little from run to run and from machine to machine. Each benchmark discards warm-up runs and reports the average of several runs, and the conclusions rely on growth trends and ratios, not on one number.

## Result of each task (short note)
* **Q1** – Repeated `+` copies O(n²) characters: 40,000 fields took ~1,476 ms against ~0.5 ms for StringBuilder (≈3,000×). For a short ticket the gap is small and a pre-sized StringBuilder is 2–3× faster than `+`. In a multithreaded server `StringBuilder` lost appends / crashed threads in 3 of 10 trials; `StringBuffer` was always correct.
* **Q2** – ArrayList is excellent for get-by-index and append; at 300,000 songs LinkedList's middle insert is ~18× slower and `get(k)` ~2,000× slower than ArrayList (but LinkedList's insert at the front is ~700× faster). For the "Up Next" queue ArrayDeque is the right structure (10,000 mixed operations: 0.015 ms vs 178 ms for ArrayList at n = 300,000).
* **Q3** – Without `hashCode()` the program prints `Unique students: 3`; with `hashCode()` it prints `Unique students: 2`.
* **Q4** – 100,000 lookups at 500,000 medicines: HashMap 7 ms, LinkedHashMap 15 ms, TreeMap 179 ms. TreeMap gives the code-order report for free; LinkedHashMap gives the added-order report for free.
* **Q5** – Output `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]`. A comparator using subtraction breaks transitivity and made `List.sort` throw "Comparison method violates its general contract!" in 3 of 20 random runs.
* **Q6** – Output `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)`. 5,000 admits + 5,000 serves take ~2 ms. An aging score (`severity × W + arrival tick`) stops starvation (S5 patient served at tick 40 for W = 10, never under strict priority).
* **Q7** – Redesign of the Q4 evening report (HashMap + cached sorted index + merge of new codes): ~7× fewer comparisons and ~2.4–3.7× less time than re-sorting every evening, with identical lookup speed; costs ~8 MB extra at 500,000 medicines.
