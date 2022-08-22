import net.dv8tion.jda.api.entities.TextChannel;

public class SubQueue {
    private String subreddit;
    private TextChannel channel;
    private TextChannel authorChannel;

    public SubQueue(String subreddit, TextChannel channel,TextChannel authorChannel){
        this.channel = channel;
        this.authorChannel = authorChannel;
        this.subreddit = subreddit;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public TextChannel getAuthorChannel() {
        return authorChannel;
    }
}
