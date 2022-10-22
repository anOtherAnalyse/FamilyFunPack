package family_fun_pack.utils;

public class Timer {
    private long time;

    public boolean passed(double ms) {
        return getTime() >= ms;
    }

    public Timer reset() {
        time = System.currentTimeMillis();
        return this;
    }

    public long getTime() {
        return System.currentTimeMillis() - time;
    }

    public void setTime(long ns) {
        time = ns;
    }

    public void adjust(int time) {
        this.time += time;
    }

}
