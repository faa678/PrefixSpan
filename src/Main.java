/**
 * Created by mhwong on 7/30/15.
 */
public class Main {

    public static void main (String[] args) {
        Runtime runtime = Runtime.getRuntime();
        long startTime = System.currentTimeMillis();
        new PrefixSpan();
        long endTime = System.currentTimeMillis();
        System.out.println("Execution Time: " + (endTime - startTime)/1000 + "s");
        System.out.println("Execution Time: " + (endTime - startTime) + "ms");
        runtime.gc();
        long memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used Memory: " + memory/(1024L) + "KB");
    }
}
