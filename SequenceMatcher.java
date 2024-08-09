import java.util.*;

public class SequenceMatcher {

    private String a;
    private String b;
    private Map<Character, List<Integer>> b2j;
    private Set<Character> bjunk;
    private boolean autojunk;

    public SequenceMatcher(String a, String b) {
        this(a, b, true);
    }

    public SequenceMatcher(String a, String b, boolean autojunk) {
        this.a = a;
        this.b = b;
        this.autojunk = autojunk;
        this.b2j = new HashMap<>();
        this.bjunk = new HashSet<>();
        chainB();
    }

    private void chainB() {
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            b2j.computeIfAbsent(c, k -> new ArrayList<>()).add(i);
        }

        if (autojunk && b.length() >= 200) {
            int ntest = b.length() / 100 + 1;
            for (Map.Entry<Character, List<Integer>> entry : b2j.entrySet()) {
                if (entry.getValue().size() > ntest) {
                    bjunk.add(entry.getKey());
                }
            }
            for (char j : bjunk) {
                b2j.remove(j);
            }
        }
    }

    public Match findLongestMatch(int alo, int ahi, int blo, int bhi) {
        int besti = alo;
        int bestj = blo;
        int bestsize = 0;

        Map<Integer, Integer> j2len = new HashMap<>();

        for (int i = alo; i < ahi; i++) {
            Map<Integer, Integer> newj2len = new HashMap<>();
            for (int j : b2j.getOrDefault(a.charAt(i), Collections.emptyList())) {
                if (j < blo) continue;
                if (j >= bhi) break;
                Map<Integer, Integer> finalJ2len = j2len;
                int k = newj2len.merge(j, 1, (old, v) -> finalJ2len.getOrDefault(j - 1, 0) + 1);
                if (k > bestsize) {
                    besti = i - k + 1;
                    bestj = j - k + 1;
                    bestsize = k;
                }
            }
            j2len = newj2len;
        }

        while (besti > alo && bestj > blo && a.charAt(besti - 1) == b.charAt(bestj - 1)) {
            besti--;
            bestj--;
            bestsize++;
        }

        while (besti + bestsize < ahi && bestj + bestsize < bhi &&
                a.charAt(besti + bestsize) == b.charAt(bestj + bestsize)) {
            bestsize++;
        }

        return new Match(besti, bestj, bestsize);
    }

    public List<Match> getMatchingBlocks() {
        List<Match> matching_blocks = new ArrayList<>();
        findMatchingBlocks(0, a.length(), 0, b.length(), matching_blocks);
        matching_blocks.add(new Match(a.length(), b.length(), 0));
        return matching_blocks;
    }

    private void findMatchingBlocks(int alo, int ahi, int blo, int bhi, List<Match> matching_blocks) {
        Match match = findLongestMatch(alo, ahi, blo, bhi);
        if (match.size > 0) {
            if (alo < match.a && blo < match.b) {
                findMatchingBlocks(alo, match.a, blo, match.b, matching_blocks);
            }
            matching_blocks.add(match);
            if (match.a + match.size < ahi && match.b + match.size < bhi) {
                findMatchingBlocks(match.a + match.size, ahi, match.b + match.size, bhi, matching_blocks);
            }
        }
    }

    public List<Object[]> getOpcodes() {
        // Return list of 5-tuples describing how to turn a into b.

        // Each tuple is of the form (tag, i1, i2, j1, j2).  The first tuple
        // has i1 == j1 == 0, and remaining tuples have i1 == the i2 from the
        // tuple preceding it, and likewise for j1 == the previous j2.

        // The tags are strings, with these meanings:

        // 'replace':  a[i1:i2] should be replaced by b[j1:j2]
        // 'delete':   a[i1:i2] should be deleted.
        //             Note that j1==j2 in this case.
        // 'insert':   b[j1:j2] should be inserted at a[i1:i1].
        //             Note that i1==i2 in this case.
        // 'equal':    a[i1:i2] == b[j1:j2]

        // >>> a = "qabxcd"
        // >>> b = "abycdf"
        // >>> s = SequenceMatcher(None, a, b)
        // >>> for tag, i1, i2, j1, j2 in s.get_opcodes():
        // ...    print(("%7s a[%d:%d] (%s) b[%d:%d] (%s)" %
        // ...           (tag, i1, i2, a[i1:i2], j1, j2, b[j1:j2])))
        //  delete a[0:1] (q) b[0:0] ()
        //  equal a[1:3] (ab) b[0:2] (ab)
        // replace a[3:4] (x) b[2:3] (y)
        //  equal a[4:6] (cd) b[3:5] (cd)
        //  insert a[6:6] () b[5:6] (f)

        List<Object[]> opcodes = new ArrayList<>();
        int i = 0;
        int j = 0;

        for (Match match : getMatchingBlocks()) {
            int ai = match.a;
            int bj = match.b;
            int size = match.size;

            String tag = "";
            if (i < ai && j < bj) {
                tag = "replace";
            } else if (i < ai) {
                tag = "delete";
            } else if (j < bj) {
                tag = "insert";
            }

            if (!tag.isEmpty()) {
                opcodes.add(new Object[]{tag, i, ai, j, bj});
            }

            i = ai + size;
            j = bj + size;

            if (size > 0) {
                opcodes.add(new Object[]{"equal", ai, i, bj, j});
            }
        }

        return opcodes;
    }

    public List<List<Object[]>> getGroupedOpcodes(int n) {
        // Isolate change clusters by eliminating ranges with no changes.

        // Return a generator of groups with up to n lines of context.
        // Each group is in the same format as returned by get_opcodes().

        // >>> from pprint import pprint
        // >>> a = list(map(str, range(1,40)))
        // >>> b = a[:]
        // >>> b[8:8] = ['i']     # Make an insertion
        // >>> b[20] += 'x'       # Make a replacement
        // >>> b[23:28] = []      # Make a deletion
        // >>> b[30] += 'y'       # Make another replacement
        // >>> pprint(list(SequenceMatcher(None,a,b).get_grouped_opcodes()))
        // [[('equal', 5, 8, 5, 8), ('insert', 8, 8, 8, 9), ('equal', 8, 11, 9, 12)],
        //  [('equal', 16, 19, 17, 20),
        // ('replace', 19, 20, 20, 21),
        // ('equal', 20, 22, 21, 23),
        // ('delete', 22, 27, 23, 23),
        // ('equal', 27, 30, 23, 26)],
        //  [('equal', 31, 34, 27, 30),
        // ('replace', 34, 35, 30, 31),
        // ('equal', 35, 38, 31, 34)]]

        List<List<Object[]>> result = new ArrayList<>();
        List<Object[]> codes = getOpcodes();

        if (codes.isEmpty()) {
            codes.add(new Object[]{"equal", 0, 1, 0, 1});
        }

        if (codes.get(0)[0].equals("equal")) {
            Object[] first = codes.get(0);
            codes.set(0, new Object[]{first[0], Math.max((int) first[1], (int) first[2] - n), first[2],
                    Math.max((int) first[3], (int) first[4] - n), first[4]});
        }

        if (codes.get(codes.size() - 1)[0].equals("equal")) {
            Object[] last = codes.get(codes.size() - 1);
            codes.set(codes.size() - 1, new Object[]{last[0], last[1], Math.min((int) last[2], (int) last[1] + n),
                    last[3], Math.min((int) last[4], (int) last[3] + n)});
        }

        int nn = n + n;
        List<Object[]> group = new ArrayList<>();

        for (Object[] code : codes) {
            String tag = (String) code[0];
            int i1 = (int) code[1];
            int i2 = (int) code[2];
            int j1 = (int) code[3];
            int j2 = (int) code[4];

            if (tag.equals("equal") && i2 - i1 > nn) {
                group.add(new Object[]{tag, i1, Math.min(i2, i1 + n), j1, Math.min(j2, j1 + n)});
                result.add(new ArrayList<>(group));
                group.clear();
                i1 = Math.max(i1, i2 - n);
                j1 = Math.max(j1, j2 - n);
            }

            group.add(new Object[]{tag, i1, i2, j1, j2});
        }

        if (!group.isEmpty() && !(group.size() == 1 && group.get(0)[0].equals("equal"))) {
            result.add(group);
        }

        return result;
    }

    public double Ratio() {
        // Return a measure of the sequences' similarity (float in [0,1]).

        // Where T is the total number of elements in both sequences, and
        // M is the number of matches, this is 2.0*M / T.
        // Note that this is 1 if the sequences are identical, and 0 if
        // they have nothing in common.

        // .ratio() is expensive to compute if you haven't already computed
        // .get_matching_blocks() or .get_opcodes(), in which case you may
        // want to try .quick_ratio() or .real_quick_ratio() first to get an
        // upper bound.

        // >>> s = SequenceMatcher(None, "abcd", "bcde")
        // >>> s.ratio()
        // 0.75
        // >>> s.quick_ratio()
        // 0.75
        // >>> s.real_quick_ratio()
        // 1.0
        int matches = 0;
        for (Match block : getMatchingBlocks()) {
            matches += block.size;
        }
        return calculateRatio(matches, a.length() + b.length());
    }

    public double quickRatio() {
        // Return an upper bound on ratio() relatively quickly.
        // This isn't defined beyond that it is an upper bound on .ratio(), and
        // is faster to compute.
        int al = a.length();
        int bl = b.length();
        Map<Character, Integer> fullbCount = new HashMap<>();
        for (int i = 0; i < bl; i++) {
            fullbCount.put(b.charAt(i), fullbCount.getOrDefault(b.charAt(i), 0) + 1);
        }

        Map<Character, Integer> fullaCount = new HashMap<>();
        int matches = 0;
        for (int j = 0; j < al; j++) {
            int numb;
            if (fullaCount.containsKey(a.charAt(j))){
                numb = fullaCount.get(a.charAt(j));
            }else{
                numb = fullbCount.getOrDefault(a.charAt(j), 0);
            }
            fullaCount.put(a.charAt(j), numb -1);
            if (numb > 0){
                matches = matches + 1;
            }
        }

        return calculateRatio(matches, al + bl);
    }

    public double realQuickRatio() {
        // Return an upper bound on ratio() very quickly.
        // This isn't defined beyond that it is an upper bound on .ratio(), and
        // is faster to compute than either .ratio() or .quick_ratio().
        int al = a.length();
        int bl = b.length();
        return calculateRatio(Integer.compare(al, bl), al + bl);
    }

    public static class Match {
        public final int a;
        public final int b;
        public final int size;

        public Match(int a, int b, int size) {
            this.a = a;
            this.b = b;
            this.size = size;
        }

        @Override
        public String toString() {
            return String.format("Match(a=%d, b=%d, size=%d)", a, b, size);
        }
    }

    private double calculateRatio(int matches, int length) {
        return length == 0 ? 1.0 : 2.0 * matches / length;
    }
}
