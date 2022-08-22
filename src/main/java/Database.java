

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.sql.*;

public class Database {
    // JDBC driver name and database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/redditor";

    //  Database credentials
    static final String USER = "XXX";
    static final String PASS = "XXX";

    Connection conn = null;


    public Database(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
        }catch(ClassNotFoundException cnfe){
            cnfe.printStackTrace();
        }catch(SQLException sqe){
            sqe.printStackTrace();
        }
    }

    public ArrayList<String> getAllRedditsWUpdate() throws SQLException{

        ArrayList<String> redditList = new ArrayList<>();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT subreddit,LastChecked FROM reddits";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {
                String nextSub = rs.getString("subreddit");
                String lastChecked = rs.getString("LastChecked");
                redditList.add(nextSub+","+lastChecked);
            }
            rs.close();
            return redditList;
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        return null;
    }

    public void removeReddit(String subreddit) throws SQLException{
        subreddit = subreddit.toLowerCase();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "DELETE FROM reddits WHERE subreddit='"+subreddit+"'";
            stmt.execute(sql);
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

    public void subscribe(String subreddit, TextChannel channel, TextChannel authorChannel) throws SQLException{
        subreddit = subreddit.toLowerCase();
        boolean nsfwSub = isSubredditNSFW(subreddit);
        if(!channel.isNSFW() && nsfwSub){
            try {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(16743936);
                String myTitle = "NSFW";
                eb.setTitle(myTitle);
                eb.setDescription("Subreddit is NSFW. Please mark the '"+channel.getName()+"' text channel as NSFW in the text channel settings.");
                eb.setFooter(FooterMessage.footerMessage);
                authorChannel.sendMessageEmbeds(eb.build()).queue();

            }catch(InsufficientPermissionException e){
                System.out.println("Cannot write to channel");
            }

        }else {
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                boolean alreadyAdded = false;
                checkGuildAddIfFalse(channel.getGuild());
                String[] currentSubscriptions = getGuildSubscriptionList(channel.getGuild());
                String[] newSubscriptions = new String[currentSubscriptions.length + 1];
                for (int i = 0; i < currentSubscriptions.length; i++) {
                    if (currentSubscriptions[i].toLowerCase().startsWith(subreddit + "/")) {
                        alreadyAdded = true;

                        try {
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(16743936);
                            String myTitle = "Already subscribed";
                            eb.setTitle(myTitle);
                            eb.setDescription("You're already subscribed to this subreddit!");
                            eb.setFooter(FooterMessage.footerMessage);
                            authorChannel.sendMessageEmbeds(eb.build()).queue();
                        }catch(InsufficientPermissionException e){
                            System.out.println("Cannot write to channel");
                        }

                    }
                    newSubscriptions[i] = currentSubscriptions[i];
                }
                if (!alreadyAdded) {
                   /* try {
                        channel.sendMessage("This channel is now subscribed to " + subreddit).queue();
                    }catch(InsufficientPermissionException e){
                        System.out.println("Cannot write to channel");
                        }*/

                    try {

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(16743936);
                        String myTitle = "Successfully subscribed";
                        eb.setTitle(myTitle);
                        eb.setDescription("Successfully subscribed " + channel.getName() + " to " + subreddit);
                        eb.setFooter(FooterMessage.footerMessage);
                        authorChannel.sendMessageEmbeds(eb.build()).queue();
                    }catch(InsufficientPermissionException e){
                        System.out.println("Cannot write to channel");
                    }

                }
                if (!alreadyAdded) {
                    newSubscriptions[currentSubscriptions.length] = subreddit + "/" + channel.getId();
                    String newString = "";
                    for (int i = 0; i < newSubscriptions.length; i++) {
                        if (i == 0) {
                            newString = newSubscriptions[i];
                        } else {
                            newString = newString + " " + newSubscriptions[i];
                        }
                    }

                    String sql;
                    sql = "UPDATE guilds SET subreddits='" + newString + "' WHERE Guild=" + channel.getGuild().getId();
                    stmt.execute(sql);
                    sql = "INSERT INTO reddits (subreddit,LastChecked) VALUES('" + subreddit + "'," + System.currentTimeMillis() / 1000 + ")";
                    stmt.execute(sql);
                }
            } catch (SQLException e) {
                System.out.println("SQL error when subscribing, subreddit is probably already in global list");
                //e.printStackTrace();
            } finally {
                if (stmt != null) { stmt.close(); }
            }
        }
    }

    public void unsubscribe(String subreddit, TextChannel channel) throws SQLException{
        subreddit = subreddit.toLowerCase();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            checkGuildAddIfFalse(channel.getGuild());
            String[] currentSubscriptions = getGuildSubscriptionList(channel.getGuild());
            List<String> newList = new ArrayList<>();

            for(int i=0;i<currentSubscriptions.length;i++){
                if(!currentSubscriptions[i].toLowerCase().toLowerCase().startsWith(subreddit+"/")){
                    newList.add(currentSubscriptions[i]);
                }
            }
            String[] newSubscriptions = (String[]) newList.toArray(new String[newList.size()]);

            String newString = "";
            for (int i = 0; i < newSubscriptions.length; i++) {
                if (i == 0) {
                    newString = newSubscriptions[i];
                } else {
                    newString = newString + " " + newSubscriptions[i];
                }
            }

            String sql;
            sql = "UPDATE guilds SET subreddits='" + newString + "' WHERE Guild=" + channel.getGuild().getId();
            stmt.execute(sql);
            if(channel!=null) {
                try {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(16743936);
                    String myTitle = "Successfully unsubscribed";
                    eb.setTitle(myTitle);
                    eb.setDescription("You are no longer subscribed to " + subreddit);
                    eb.setFooter(FooterMessage.footerMessage);
                    channel.sendMessageEmbeds(eb.build()).queue();

                }catch(InsufficientPermissionException e){
                    System.out.println("Cannot write to channel");
                }

            }
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }


    public long getLastUpdated(String subreddit) throws SQLException{
        subreddit = subreddit.toLowerCase();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT LastChecked FROM reddits where subreddit='"+subreddit+"'";
            ResultSet rs = stmt.executeQuery(sql);
            long lastChecked = 0;
            if(rs.next()) {
                lastChecked = rs.getLong("LastChecked");
            }
            rs.close();
            return lastChecked;
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        return 0;
    }

    public long setLastUpdated(String subreddit,long lastChecked) throws SQLException{
        subreddit = subreddit.toLowerCase();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "UPDATE reddits SET LastChecked="+lastChecked/1000+" Where subreddit='"+subreddit+"'";
            stmt.execute(sql);
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        return 0;
    }

    public String[] getGuildSubscriptionList(Guild guild) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT Guild,subreddits FROM guilds where Guild="+guild.getId();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {
                String subreddits = rs.getString("subreddits");
                String[] subscriptions = subreddits.split(" ");
                rs.close();
                return subscriptions;
            }
            rs.close();
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        String[] empty = {};
        return empty;
    }

    public ArrayList<String> getChannelsSubscribedToSubreddit(String subreddit) throws SQLException{
        subreddit = subreddit.toLowerCase();
        ResultSet rs;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ArrayList<String> listOfChannels = new ArrayList<>();
            String sql;
            sql = "SELECT Guild,subreddits FROM guilds where subreddits LIKE '% "+subreddit+"/%'";
            rs = stmt.executeQuery(sql);
            while(rs.next()) {
                String stringResult = rs.getString("subreddits");
                String guild = rs.getString("Guild");
                String[] subList = stringResult.split(" ");
                for(int i=0;i<subList.length;i++){
                    if(subList[i].toLowerCase().startsWith(subreddit+"/")){
                        String[] splitString = subList[i].split("/");
                        String textChannel = splitString[1];
                        listOfChannels.add(textChannel);
                    }
                }
            }
            rs.close();
            ArrayList<String> listOfChannelsFiltered = new ArrayList<>();
            Iterator<String> listIt = listOfChannels.iterator();
            while(listIt.hasNext()){
                String nextChannel = listIt.next();
                Iterator<String> listIt2 = listOfChannelsFiltered.iterator();
                boolean duplicate = false;
                while(listIt2.hasNext() && !duplicate){
                    String nextChannel2 = listIt2.next();
                    if(nextChannel.equals(nextChannel2)){
                        duplicate = true;
                        System.out.println("***************DUPLICATE CHANNEL FOUND*****************");
                    }
                }
                if(!duplicate){
                    listOfChannelsFiltered.add(nextChannel);
                }

            }
            return listOfChannelsFiltered;
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        return null;
    }


    public void exit(){
        try {
            conn.close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }


    public void checkGuildAddIfFalse(Guild guild) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT Guild FROM guilds where Guild="+guild.getId();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {   // Move the cursor to the next row, return false if no more row
                long guildId = rs.getLong("Guild");
                if(guildId==guild.getIdLong()){
                    rs.close();
                    return;
                }
            }
            rs.close();
            sql = "INSERT INTO guilds (Guild,subreddits) VALUES ("+guild.getId()+",'')";
            stmt.execute(sql);
        }catch(SQLException e){
            System.out.println("Error checking guild or adding");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }


    public void guildLeave(Guild guild) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "DELETE FROM guilds WHERE Guild="+guild.getId();
            stmt.execute(sql);
        }catch(SQLException e){
            System.out.println("Could not remove guild from list");
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

    public boolean isSubredditNSFW(String subreddit){
        try {
            JSONObject json = RedditJSONHandler.readJsonFromUrl("https://www.reddit.com/r/"+subreddit+"/about.json");
            boolean isNsfw = json.getJSONObject("data").getBoolean("over18");
            return isNsfw;
        }catch(FileNotFoundException fnf){
            System.out.println("Error with JSON request 2 (Subreddit :"+subreddit+")");
        }catch(IOException e){
            System.out.println("Error with JSON request, most likely 503 (Subreddit :"+subreddit+")");
        }catch(Exception idk){
            return false;
        }
        return true;
    }

    public void checkNewPremium(ShardManager shardManager) throws SQLException{
        Guild supServer = shardManager.getGuildById("XXX");
        Role premium = supServer.getRoleById("XXX");


        List<Member> premiumList = supServer.getMembersWithRoles(premium);

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT * FROM Premiumlist";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {   // Move the cursor to the next row, return false if no more row
                long userID = rs.getLong("UserID");
                long guildID = rs.getLong("GuildID");
                long tier = rs.getLong("Tier");

                Member curMem = supServer.getMemberById(userID);
                if(curMem!=null){
                    if(premiumList.contains(curMem)){
                        premiumList.remove(curMem);
                    }
                }
            }
            rs.close();

            Iterator<Member> it = premiumList.iterator();
            while(it.hasNext()) {
                Member nextMem = it.next();
                sql = "INSERT INTO Premiumlist (UserID,GuildID,Tier) VALUES('" + nextMem.getUser().getId()+"'" + ",0,0)";
                stmt.execute(sql);
                nextMem.getUser().openPrivateChannel().queue(privateChannel ->
                {
                    privateChannel.sendMessage(
                            "Congrats!\n You now have access to Premium benefits of the Redditor bot!\n" +
                                    "To activate your benefits in your server use the './/activate' command in that sever.\n" +
                                    "You can only have your premium benefits active in one server at a time. The './/activate' command will overwrite your previous server, but you can always switch back!").queue();
                });

            }
        }catch(SQLException e){
            System.out.println("Error checking guild or adding");
            e.printStackTrace();
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

    public int updatePremiumGuild(String userId,Guild guild) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "UPDATE Premiumlist SET GuildID="+guild.getId()+" Where UserID='"+userId+"'";
            stmt.execute(sql);
            return 1;
        }catch(SQLException e){
            System.out.println("SQL error");
            e.printStackTrace();
            return 0;
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

    public boolean userHasPremium(String userId) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT * FROM Premiumlist";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {   // Move the cursor to the next row, return false if no more row
                String userID = rs.getString("UserID");
                if(userID.equals(userId)){
                    rs.close();
                    return true;
                }
            }
            rs.close();
            return false;
        }catch(SQLException e){
            System.out.println("Error");
            e.printStackTrace();
            return false;
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

    public boolean GuildHasPremium(Guild guild) throws SQLException{
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT GuildID FROM Premiumlist WHERE GuildID='"+guild.getId()+"'";
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()) {   // Move the cursor to the next row, return false if no more row
                String userID = rs.getString("GuildID");
                if(userID.equals(guild.getId())){
                    rs.close();
                    return true;
                }
            }
            rs.close();
            return false;
        }catch(SQLException e){
            System.out.println("Error");
            e.printStackTrace();
            return false;
        } finally {
            if (stmt != null) { stmt.close(); }
        }
    }

}