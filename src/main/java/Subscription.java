public class Subscription {

    String subreddit;
    String channel;
    String type;
    String topType;

    public Subscription(String subreddit, String channel, String type, String topType){
        subreddit = this.subreddit;
        channel = this.channel;
        type = this.type;
        topType = this.topType;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public String getChannel() {
        return channel;
    }

    public String getType() {
        return type;
    }

    public String getTopType() {
        return topType;
    }
}
