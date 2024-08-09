# SequenceMatcher
Java reimplementation of Python difflib.SequenceMatcher

<details open>
<summary>Usage</summary>

### Code
You may also be used directly in a Java environment
```java
class Main {
    public static void main(String[] args) {
        String str1 = "变桨电机3温度异常";
        String str2 = "变桨电机超温解决";
        String str3 = "变桨驱动器更换";


        SequenceMatcher sequenceMatcher = new SequenceMatcher(str1, str2);
        double r1 = sequenceMatcher.Ratio();
        System.out.println("Matching ratio:"+ r1);

        SequenceMatcher sequenceMatcher2 = new SequenceMatcher(str1, str3);
        double r2 = sequenceMatcher2.Ratio();
        System.out.println("Matching ratio:"+ r2);

        double r3 = sequenceMatcher.quickRatio();
        System.out.println("Matching quickRatio:"+ r3);

        double r4 = sequenceMatcher2.quickRatio();
        System.out.println("Matching quickRatio:"+ r4);

        double r5 = sequenceMatcher.realQuickRatio();
        System.out.println("Matching realQuickRatio:"+ r5);

        double r6 = sequenceMatcher2.realQuickRatio();
        System.out.println("Matching realQuickRatio:"+ r6);

        // 打印 get_opcodes 的结果
        System.out.println("get_opcodes 结果:");
        for (Object[] opcode : sequenceMatcher.getOpcodes()) {
            String tag = (String) opcode[0];
            int i1 = (int) opcode[1];
            int i2 = (int) opcode[2];
            int j1 = (int) opcode[3];
            int j2 = (int) opcode[4];
            System.out.printf("%7s a[%d:%d] (%s) b[%d:%d] (%s)%n",
                    tag, i1, i2, str1.substring(i1, i2), j1, j2, str2.substring(j1, j2));
        }

        System.out.println();

        // 打印 get_grouped_opcodes 的结果
        System.out.println("get_grouped_opcodes 结果:");
        for (List<Object[]> group : sequenceMatcher.getGroupedOpcodes(3)) {
            System.out.println("[");
            for (Object[] opcode : group) {
                String tag = (String) opcode[0];
                int i1 = (int) opcode[1];
                int i2 = (int) opcode[2];
                int j1 = (int) opcode[3];
                int j2 = (int) opcode[4];
                System.out.printf("  (%s, %d, %d, %d, %d),%n", tag, i1, i2, j1, j2);
            }
            System.out.println("]");
        }
    }
}
```

Console Output
```bash
Matching ratio:0.5882352941176471
Matching ratio:0.25

Matching quickRatio:0.5882352941176471
Matching quickRatio:0.25

Matching realQuickRatio:0.11764705882352941
Matching realQuickRatio:0.125

get_opcodes 结果:
  equal a[0:4] (变桨电机) b[0:4] (变桨电机)
replace a[4:5] (3) b[4:5] (超)
  equal a[5:6] (温) b[5:6] (温)
replace a[6:9] (度异常) b[6:8] (解决)

get_grouped_opcodes 结果:
[
  (equal, 1, 4, 1, 4),
  (replace, 4, 5, 4, 5),
  (equal, 5, 6, 5, 6),
  (replace, 6, 9, 6, 8),
]
```

</details>