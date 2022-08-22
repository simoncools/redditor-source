public class Subreddit {
    long lastChecked;
    String name;

    public Subreddit(String name,long lastChecked){
        this.lastChecked = lastChecked;
        this.name = name;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
