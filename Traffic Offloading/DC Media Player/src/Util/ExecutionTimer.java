package Util;

public class ExecutionTimer {
    private long start;
    private long end;
    public ExecutionTimer() {
        reset();
        start();
    }

    public void reset() {
        start = 0;
        end = 0;
    }

    public void start() {
        start = System.nanoTime();
    }

    public ExecutionTimer end() {
        end = System.nanoTime();
        return this;
    }

    public double duration() {
        return (end - start) / 1000000.0;
    }

    public static void main(String s[]) {
        ExecutionTimer t = new ExecutionTimer();
        t.start();
        for (int i = 0; i < 8000000; i++) {
            MyColor.getColorfromYUV((byte) (i % 256), (byte) (i % 256), (byte) (i % 256));
        }
        t.end();
        System.out.println("\n" + t.duration() + " ms");
    }
}
