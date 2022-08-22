public class UpdateLogger {
    private long lastUpdateRan;

    public UpdateLogger(){
        lastUpdateRan = System.currentTimeMillis();
    }

    public long getLastUpdateRan() {
        return lastUpdateRan;
    }

    public void setLastUpdateRan(long lastUpdateRan) {
        this.lastUpdateRan = lastUpdateRan;
    }
}
