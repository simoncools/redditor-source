import java.util.ArrayList;

public class PostList {
    private ArrayList<RedditPost> postList;
    private String subreddit;
    private long lastChecked;

    public PostList(String subreddit,ArrayList<RedditPost> postList){
        this.postList = postList;
        this.subreddit = subreddit;
        this.lastChecked = System.currentTimeMillis();
    }

    public ArrayList<RedditPost> getPostList() {
        return postList;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void addPost(RedditPost post){
        postList.add(post);
        return;
    }
}
