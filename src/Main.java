/**
 * Created by mhwong on 7/30/15.
 */
public class Main {

    public static void main (String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java " + Main.class.getCanonicalName() + " input_file minsup");
        } else {
            Runtime runtime = Runtime.getRuntime();
            long startTime = System.currentTimeMillis();
            System.out.println("File: " + args[0]);
            System.out.println("MinSup: " + args[1]);
            new PrefixSpan(args[0], Double.valueOf(args[1]));
            long endTime = System.currentTimeMillis();
            System.out.println("Execution Time: " + (endTime - startTime) / 1000 + "s");
            System.out.println("Execution Time: " + (endTime - startTime) + "ms");
            runtime.gc();
            long memory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Used Memory: " + memory + "b");
            System.out.println("Used Memory: " + memory / (1024L * 1024L) + "MB");
            System.out.println();
            System.out.println();
        }
    }
}
