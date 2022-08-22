public class RedditPost {
    String url;
    String title;
    String selftext;
    Long date;
    String permaLink;
    String subreddit;
    boolean nsfw;

    public RedditPost(String url,String subreddit, String title, String selftext,Long date,String permaLink,boolean nsfw){
        this.selftext = selftext;
        this.title = title;
        this.url = url;
        this.date = date;
        this.permaLink = permaLink;
        this.nsfw = nsfw;
        this.subreddit = subreddit;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSelftext() {
        return selftext;
    }

    public Long getDate() {
        return date;
    }

    public String getPermaLink() {
        return permaLink;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }
}
