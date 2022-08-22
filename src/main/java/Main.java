import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.Thread.interrupted;

public class Main extends ListenerAdapter {
    private static Logger logger;
    private static boolean isLast = true;
    private static boolean isUpdating = false;
    private static UpdateLogger updateLogger = new UpdateLogger();
    private static Database database = new Database();
    private static ShardManager shardManager;
    private static ArrayList<SubQueue> subQueueList = new ArrayList<>();
    private static double lastInterval = 0;
    private static String currentlyPosting = "";
    private static DiscordBotListAPI api;
    private static ArrayList<String> redditList = new ArrayList<>();
    private static long startTime = System.currentTimeMillis();
    private static long endTime = System.currentTimeMillis();
    private static ArrayList<Thread> threadList = new ArrayList<>();

    public static void main(String[] args) throws LoginException {

        /*
        Ignore unknown channel errors
         */
        ErrorResponseException.ignore(ErrorResponse.UNKNOWN_CHANNEL);
        String token = "XXX";
        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(token
                ,GatewayIntent.GUILD_BANS
                ,GatewayIntent.GUILD_EMOJIS_AND_STICKERS
                ,GatewayIntent.GUILD_INVITES
               // ,GatewayIntent.GUILD_VOICE_STATES
                ,GatewayIntent.GUILD_MESSAGES
                ,GatewayIntent.GUILD_MESSAGE_REACTIONS
                ,GatewayIntent.GUILD_MESSAGE_TYPING
                ,GatewayIntent.DIRECT_MESSAGE_REACTIONS
                ,GatewayIntent.DIRECT_MESSAGE_TYPING
                ,GatewayIntent.DIRECT_MESSAGES
                ,GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.OWNER)
                .disableCache(CacheFlag.ACTIVITY)
                .disableCache(CacheFlag.CLIENT_STATUS)
                .disableCache(CacheFlag.EMOJI)
                .disableCache(CacheFlag.MEMBER_OVERRIDES)
                .disableCache(CacheFlag.VOICE_STATE);
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.setLargeThreshold(200);
        builder.setShardsTotal(15);
        builder.setToken(token);
        builder.addEventListeners(new Main());
        shardManager = builder.build();
        List<JDA> shardList = shardManager.getShards();
        Iterator<JDA> shardIterator = shardList.iterator();
        while(shardIterator.hasNext()){
            JDA nextShard = shardIterator.next();
            nextShard.upsertCommand("help","information about commands and usage of the bot").queue();
            nextShard.upsertCommand("subscribe","subscribe a text channel to a subreddit")
                    .addOption(OptionType.STRING,"subreddit","the name of the subreddit you wish to receive updates of",true)
                    .addOption(OptionType.CHANNEL,"textchannel","choose the text channel where you wish to receive updates",true).queue();
            nextShard.upsertCommand("unsubscribe","unsubscribe from a subreddit")
                    .addOption(OptionType.STRING,"subreddit","the name of the subreddit you wish to unsubscribe from",true).queue();
            nextShard.upsertCommand("list","show a list of your subscribed subreddits")
                    .addOption(OptionType.INTEGER,"page", "number of page you wish to display",true).queue();
            nextShard.upsertCommand("activate","activate premium benefits in your server").queue();
            nextShard.upsertCommand("interval","shows how long it took for the bot to finish grabbing and posting all new reddit posts").queue();
            nextShard.upsertCommand("vote","displays the link to the top.gg voting page. vote for redditor!").queue();
        }



        try{
            Thread.sleep(180000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        api = new DiscordBotListAPI.Builder()
                .token("XXX")
                .botId("XXX")
                .build();
        /* LOGGER */
        logger = Logger.getLogger("MyLog");
        FileHandler fh;


        try {

            // This block configure the logger with handler and formatter
            fh = new FileHandler("/home/redditorlogs/logfile"+System.currentTimeMillis()+".log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages
            logger.info("Bot Started");

            Guild myTestGuild = shardManager.getGuildById("XXX");
            myTestGuild.upsertCommand("help","information about commands and usage of the bot").queue();
            myTestGuild.upsertCommand("subscribe","subscribe a text channel to a subreddit")
                    .addOption(OptionType.STRING,"subreddit","the name of the subreddit you wish to receive updates of",true)
                    .addOption(OptionType.CHANNEL,"textchannel","choose the text channel where you wish to receive updates",true).queue();
            myTestGuild.upsertCommand("unsubscribe","unsubscribe from a subreddit")
                    .addOption(OptionType.STRING,"subreddit","the name of the subreddit you wish to unsubscribe from",true).queue();
            myTestGuild.upsertCommand("list","show a list of your subscribed subreddits")
                    .addOption(OptionType.INTEGER,"page", "number of page you wish to display",true).queue();
            myTestGuild.upsertCommand("activate","activate premium benefits in your server").queue();
            myTestGuild.upsertCommand("interval","shows how long it took for the bot to finish grabbing and posting all new reddit posts").queue();
            myTestGuild.upsertCommand("vote","displays the link to the top.gg voting page. vote for redditor!").queue();

        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }

        try{
            Thread.sleep(10000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        //Start thread to check for premium purchases//
        Thread premiumChecker = new Thread(() -> {
            while(!interrupted()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    database.checkNewPremium(shardManager);
                } catch (NullPointerException | SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        premiumChecker.start();
        ////
        System.out.println("Running on "+shardManager.getShardsRunning()+" Shards");
        Thread statusUpdater = new Thread(() -> {
            int statusCounter = 0;
            while(!interrupted()) {
                try {
                    if(statusCounter==0) {
                        shardManager.setPresence(OnlineStatus.ONLINE, Activity.watching(" people .//vote"));
                        statusCounter=1;
                    }else if(statusCounter==1){
                        //shardManager.setPresence(OnlineStatus.ONLINE, Activity.watching(" people get Premium"));
                        statusCounter=2;
                    }else {
                        shardManager.setPresence(OnlineStatus.ONLINE, Activity.watching(" discord.gg/B28urAZ"));
                        statusCounter = 0;
                    }
                    Thread.sleep(600000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        statusUpdater.start();
        //jda.getPresence().setActivity(Activity.playing(".//vote"));
        ArrayList<PostList> allPosts = new ArrayList<>();
        Runnable updateService = () -> {
            while(!interrupted()) {
                long endTime1 = System.currentTimeMillis();
                updateLogger.setLastUpdateRan(endTime1);
                if(isLast) {

                    //  System.out.println("Updated Logger : "+updateLogger.getLastUpdateRan());
                    try {
                        int serverCount = shardManager.getGuilds().size();
                        System.out.println("Total users : "+shardManager.getUsers().size());
                        System.out.println("Current server count : " + serverCount);
                        System.out.println("Running on "+shardManager.getShardsRunning()+" Shards");
                        List<Guild> guildList = shardManager.getGuilds();
                        api.setStats(serverCount);

                        if(guildList.size()>=20) {
                            System.out.println("--Biggest Guilds--");
                            Guild[] top5 = getTop10(guildList);
                            System.out.println("1 : " + top5[0].getName()+","+top5[0].getId() + ", Members :" + top5[0].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[0]).length);
                            System.out.println("2 : " + top5[1].getName()+","+top5[1].getId()  + ", Members :" + top5[1].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[1]).length);
                            System.out.println("3 : " + top5[2].getName()+","+top5[2].getId()  + ", Members :" + top5[2].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[2]).length);
                            System.out.println("4 : " + top5[3].getName()+","+top5[3].getId()  + ", Members :" + top5[3].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[3]).length);
                            System.out.println("5 : " + top5[4].getName()+","+top5[4].getId()  + ", Members :" + top5[4].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[4]).length);
                            System.out.println("6 : " + top5[5].getName()+","+top5[5].getId()  + ", Members :" + top5[5].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[5]).length);
                            System.out.println("7 : " + top5[6].getName()+","+top5[6].getId()  + ", Members :" + top5[6].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[6]).length);
                            System.out.println("8 : " + top5[7].getName()+","+top5[7].getId()  + ", Members :" + top5[7].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[7]).length);
                            System.out.println("9 : " + top5[8].getName()+","+top5[8].getId()  + ", Members :" + top5[8].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[8]).length);
                            System.out.println("10 : " + top5[9].getName()+","+top5[9].getId()  + ", Members :" + top5[9].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[9]).length);
                            System.out.println("11 : " + top5[10].getName()+","+top5[10].getId()  + ", Members :" + top5[10].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[10]).length);
                            System.out.println("12 : " + top5[11].getName()+","+top5[11].getId()  + ", Members :" + top5[11].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[11]).length);
                            System.out.println("13 : " + top5[12].getName()+","+top5[12].getId()  + ", Members :" + top5[12].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[12]).length);
                            System.out.println("14 : " + top5[13].getName()+","+top5[13].getId()  + ", Members :" + top5[13].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[13]).length);
                            System.out.println("15 : " + top5[14].getName()+","+top5[14].getId()  + ", Members :" + top5[14].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[14]).length);
                            System.out.println("16 : " + top5[15].getName()+","+top5[15].getId()  + ", Members :" + top5[15].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[15]).length);
                            System.out.println("17 : " + top5[16].getName()+","+top5[16].getId()  + ", Members :" + top5[16].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[16]).length);
                            System.out.println("18 : " + top5[17].getName()+","+top5[17].getId()  + ", Members :" + top5[17].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[17]).length);
                            System.out.println("19 : " + top5[18].getName()+","+top5[18].getId()  + ", Members :" + top5[18].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[18]).length);
                            System.out.println("20 : " + top5[19].getName()+","+top5[19].getId()  + ", Members :" + top5[19].getMembers().size()+ ", Subs :" +database.getGuildSubscriptionList(top5[19]).length);
                        }
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        System.out.println("Error updating DBL server count");
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("-----Updating all channels!-----");
                isUpdating = true;
                Iterator<PostList> postIt = allPosts.iterator();
                while (postIt.hasNext() && !interrupted()) {
                    PostList nextList = postIt.next();
                    String nextSub = nextList.getSubreddit();
                    long lastChecked = nextList.getLastChecked();
                    ArrayList<RedditPost> posts = nextList.getPostList();
                    Runnable taskRunnable = () -> updateSubRedditNew(nextSub, posts);
                    try{
                        Thread.sleep(10);
                    }catch(InterruptedException e){
                        e.printStackTrace();

                    }
                    currentlyPosting = nextSub;
                    try {
                        database.setLastUpdated(nextSub, lastChecked);
                    }catch(SQLException e){e.printStackTrace();}
                    postIt.remove();
                    new TimeLimiter(taskRunnable);
                    //updateSubRedditNew(nextSub, posts);
                }
                currentlyPosting = "Done posting";
                if (subQueueList.size() > 0) {
                    ArrayList<SubQueue> subQueueListCopy = new ArrayList<>(subQueueList);
                    subQueueList.clear();
                    Iterator<SubQueue> subIt = subQueueListCopy.iterator();
                    while (subIt.hasNext() && !interrupted()) {
                        SubQueue nextSubRequest = subIt.next();
                        try {
                            database.subscribe(nextSubRequest.getSubreddit(), nextSubRequest.getChannel(), nextSubRequest.getAuthorChannel());
                        }catch(SQLException e){e.printStackTrace();}

                        try {
                            subIt.remove();
                        }catch(ConcurrentModificationException e){
                            e.printStackTrace();
                        }
                    }
                }

                isUpdating = false;
                System.out.println("-----Getting Next Posts!-----");

                Iterator<String> it = redditList.iterator();
                try{
                    Thread.sleep(1000);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                int subsPerCycle = 200;
                if(redditList.size()>subsPerCycle) {
                    isLast=false;
                }else{
                    isLast=true;
                    endTime = System.currentTimeMillis();
                    lastInterval = (endTime - startTime)/1000;
                    try{
                        redditList = database.getAllRedditsWUpdate();
                    }catch(SQLException e){e.printStackTrace();}
                    startTime = System.currentTimeMillis();
                }
                int subsPerRequest = 50;
                for (int q=0;q<subsPerCycle/subsPerRequest && !interrupted() && it.hasNext();q++) {
                    ArrayList<Subreddit> checkList = new ArrayList<>();
                    for(int z=0;z<subsPerRequest && it.hasNext();z++){
                        String[] nextItem = it.next().split(",");
                        String subreddit = nextItem[0];
                        long lastChecked = Long.parseLong(nextItem[1]);
                        checkList.add(new Subreddit(subreddit,lastChecked));
                        it.remove();
                    }
                    Thread getThread = new Thread(()->{
                        ArrayList<RedditPost> mixedPostList = getPosts(checkList);
                        ArrayList<PostList> postListKeeper = new ArrayList<>();

                        Iterator<RedditPost> mixedIt = mixedPostList.iterator();
                        while(mixedIt.hasNext() && !interrupted()) {
                            Iterator<PostList> keeperIt = postListKeeper.iterator();
                            RedditPost nextPost = mixedIt.next();
                            boolean listAlreadyExists = false;
                            while(keeperIt.hasNext() && !interrupted() && !listAlreadyExists){
                                PostList nextPostList = keeperIt.next();
                                if(nextPost.getSubreddit().equals(nextPostList.getSubreddit())){
                                    nextPostList.addPost(nextPost);
                                    listAlreadyExists = true;
                                }
                            }
                            if(!listAlreadyExists){
                                PostList newPostList = new PostList(nextPost.getSubreddit(), new ArrayList<>());
                                newPostList.addPost(nextPost);
                                postListKeeper.add(newPostList);
                            }
                        }
                        Iterator<PostList> keeperIt2 = postListKeeper.iterator();
                        while(keeperIt2.hasNext() && !interrupted()) {
                            PostList nextPostList = keeperIt2.next();
                            allPosts.add(nextPostList);
                        }
                        try{
                            Thread.sleep(15);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    });
                    threadList.add(getThread);
                    getThread.start();
                    try{
                        Thread.sleep(25);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }

                while(threadList.size()>0){
                    threadList.removeIf(nextThread -> !nextThread.isAlive());
                }
            }
        };

        Thread updaterThread = new Thread(updateService);
        updaterThread.start();
        Thread rebooter = new Thread(()-> {
            while(true) {
                if ((System.currentTimeMillis()-updateLogger.getLastUpdateRan()) > 2700000) {
                    logger.info("Bot stuck when posting "+currentlyPosting);
                    System.out.println("System Stuck, Restarting updates...");
                    allPosts.clear();
                    System.out.println("Shutting down old threads.");
                    updaterThread.interrupt();
                    try {
                        Thread.sleep(5000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    Thread restartUpdate = new Thread(updateService);
                    restartUpdate.start();
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if ((System.currentTimeMillis()-updateLogger.getLastUpdateRan()) > 360000) {
                    logger.info("Bot stuck for over 30 minutes...");
                }

                try {
                    Thread.sleep(180000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("System check succes, Program not stuck.");
            }
        });
        rebooter.start();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        boolean isRedditor = false;
        List<Role> roles = Objects.requireNonNull(event.getMember()).getRoles();
        for (Role nextRole : roles) {
            if (nextRole.getName().equals("Redditor")) {
                isRedditor = true;
            }
        }

        if (event.getName().equals("help")) {
            try {
                event.reply("Command received!");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(16743936);
                String myTitle = "List of commands";
                eb.setTitle(myTitle);
                eb.addField("Subscribe command", ".//subscribe [Subreddit name] [Text channel name]\nThis command adds a subreddit to a discord text channel.\n\nUsage example : .//subscribe askreddit reddit-chat\n\n---Users require 'Redditor' role or Admin permissions---\n\n", false);
                eb.addField("Unubscribe command", ".//unsubscribe [Subreddit name]\nThis command removes a subreddit from the discord server.\n\nUsage example : .//unsubscribe askreddit\n\n---Users require 'Redditor' role or Admin permissions---\n\n", false);
                eb.addField("List command", ".//list [Page number]\nThis command displays the subreddits you're subscribed to in a list divided into pages.\n\nUsage example : .//list 1\n\n---Users require no special permissions---\n\n", false);
                eb.addField("Activate command", ".//activate\nActivate your Premium benefits in your server.\n\n---Users require no special permissions---\n\n", false);
                eb.addField("Interval command", ".//interval\nShows how long it took for the bot to finish grabbing and posting all new Reddit posts\n\n-Users require no special permissions-\n\n", false);
                eb.addField("Vote command", ".//vote\nDisplays the link to the top.gg voting page. Vote for Redditor!\n\n---Users require no special permissions---\n\n", false);
                eb.setFooter(FooterMessage.footerMessage);
                event.replyEmbeds(eb.build()).queue();
            } catch (InsufficientPermissionException e) {
                System.out.println("Cannot write to channel");
                event.reply("Sorry, Something went wrong!");
            }
        }

        if (event.getName().equals("subscribe")) {
            try {
                if ((event.getMember().getPermissions().contains(Permission.ADMINISTRATOR) || isRedditor)) {
                    event.reply("Command received!");
                    if (true) {
                        TextChannel myChannel = null;
                        try {
                            if(event.getOption("textchannel").getAsChannel().getType().equals(ChannelType.TEXT)) {
                                myChannel = event.getOption("textchannel").getAsChannel().asTextChannel();
                            }else{
                                System.out.println("CHANNEL IS NOT A TEXTCHANNEL");
                                myChannel = null;
                            }
                        } catch (NullPointerException npe) {
                        }catch (IllegalStateException ise){}

                        if (myChannel != null) {
                            int exist = 0;
                            String subreddit = event.getOption("subreddit").getAsString();
                            if (subreddit.startsWith("r/")) {
                                subreddit = subreddit.replace("r/", "");
                            }
                            String newSubreddit = subreddit;
                            if (newSubreddit.contains("+") || newSubreddit.contains("/")) {
                            } else {
                                exist = doesSubredditExist(subreddit);
                            }
                            if (exist == 1) {
                                try {
                                    //Check if user voted
                                    String userId = event.getUser().getId();
                                    final TextChannel myChannelFinal = myChannel;
                                    boolean hasPremium = database.GuildHasPremium(event.getGuild());

                                    //api.hasVoted(userId).whenComplete((hasVoted, e) -> { //UNCOMMENT FOR VOTING
                                    try {
                                        boolean hasVoted = true;
                                        String[] sublist = database.getGuildSubscriptionList(event.getGuild());
                                        if (hasVoted || sublist.length <= 10 || hasPremium) {
                                            System.out.println("This person has voted!");
                                            if (sublist.length <= 20 || (hasPremium)) {

                                                if (!isUpdating) {
                                                    try {
                                                        database.subscribe(subreddit, myChannelFinal, shardManager.getTextChannelById(event.getMessageChannel().getId()));
                                                    } catch (SQLException e2) {
                                                        e2.printStackTrace();
                                                    } catch (IllegalStateException ilest) {
                                                    }
                                                    System.out.println("Subscribed a guild to " + subreddit);
                                                } else {
                                                    boolean subPending = false;
                                                    for (SubQueue myQ : subQueueList) {
                                                        if (myQ.getChannel().getGuild().getId().equals(event.getGuild().getId()) && myQ.getSubreddit().equals(subreddit)) {
                                                            try {

                                                                EmbedBuilder eb = new EmbedBuilder();
                                                                eb.setColor(16743936);
                                                                String myTitle = "Already Pending";
                                                                eb.setTitle(myTitle);
                                                                eb.setDescription("You already have a subscription pending for this subreddit, please be patient.");
                                                                eb.setFooter(FooterMessage.footerMessage);
                                                                event.replyEmbeds(eb.build()).queue();
                                                            } catch (InsufficientPermissionException exc) {
                                                                System.out.println("Cannot write to channel");
                                                            }
                                                            subPending = true;
                                                        }
                                                    }
                                                    if (!subPending) {
                                                        try {
                                                            subQueueList.add(new SubQueue(subreddit, myChannelFinal,shardManager.getTextChannelById(event.getMessageChannel().getId())));
                                                        } catch (IllegalStateException ilest) {
                                                        }
                                                        try {
                                                            EmbedBuilder eb = new EmbedBuilder();
                                                            eb.setColor(16743936);
                                                            String myTitle = "Please Wait";
                                                            eb.setTitle(myTitle);
                                                            eb.setDescription("I'm currently in the middle of posting, you'll be subscribed as soon as i'm finished!\nIf this lasts more than 5 minutes, please try again.");
                                                            eb.setFooter(FooterMessage.footerMessage);
                                                            event.replyEmbeds(eb.build()).queue();

                                                        } catch (InsufficientPermissionException exc) {
                                                            System.out.println("Cannot write to channel");
                                                        }
                                                        System.out.println("Currently updating, subscription request for " + subreddit + "put in queue.");
                                                    }
                                                }
                                            } else {
                                                try {

                                                    EmbedBuilder eb = new EmbedBuilder();
                                                    eb.setColor(16743936);
                                                    String myTitle = "Maximum subscriptions reached!";
                                                    eb.setTitle(myTitle);
                                                    eb.setDescription("You have reached the 20 subscription limit.\n");
                                                    //eb.addField("Premium features","-Unlimited subscriptions\n-Up to 25 posts per subreddit per update cycle\nNon-premium users get 1 post per subreddit per update cycle.",true);
                                                    //eb.addField("Minimum donation amount","€9.99 for Lifetime Premium",true);
                                                    eb.setFooter(FooterMessage.footerMessage);
                                                    event.replyEmbeds(eb.build()).queue();
                                                } catch (InsufficientPermissionException exc) {
                                                    System.out.println("Cannot write to channel");
                                                }
                                            }
                                        } else {
                                            System.out.println("This person has not voted");
                                            try {
                                                EmbedBuilder eb = new EmbedBuilder();
                                                eb.setColor(16743936);
                                                String myTitle = "Vote for extra subscriptions";
                                                eb.setTitle(myTitle);
                                                eb.setDescription("You've reached 10 subscriptions. To add more, vote for Redditor using the .//vote command!\nYou will be able to add up to 20 subscriptions by simply voting!\n" +
                                                        "Votes last 24hours, if you wish to make changes to your subscriptions after that period ,you will be prompted to vote again");
                                                eb.setFooter(FooterMessage.footerMessage);
                                                event.replyEmbeds(eb.build()).queue();
                                            } catch (InsufficientPermissionException exc) {
                                                System.out.println("Cannot write to channel");
                                            }
                                        }
                                    } catch (SQLException e3) {
                                        e3.printStackTrace();
                                    }
                                    //  }); //UNCOMMENT FOR VOTING
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            } else if (exist == -1) {
                                try {
                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setColor(16743936);
                                    String myTitle = "Can't verify subreddit";
                                    eb.setTitle(myTitle);
                                    eb.setDescription("Error verifying Subreddit, try again later.\n");
                                    eb.setFooter(FooterMessage.footerMessage);
                                    event.replyEmbeds(eb.build()).queue();

                                } catch (InsufficientPermissionException e) {
                                    System.out.println("Cannot write to channel");
                                }
                            } else {
                                try {

                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setColor(16743936);
                                    String myTitle = "Can't find subreddit";
                                    eb.setTitle(myTitle);
                                    eb.setDescription("Subreddit does not exist, double check your spelling");
                                    eb.setFooter(FooterMessage.footerMessage);
                                    event.replyEmbeds(eb.build()).queue();


                                } catch (InsufficientPermissionException e) {
                                    System.out.println("Cannot write to channel");
                                }
                            }
                        } else {
                            try {

                                EmbedBuilder eb = new EmbedBuilder();
                                eb.setColor(16743936);
                                String myTitle = "Can't find text channel";
                                eb.setTitle(myTitle);
                                eb.setDescription("Text channel " + myChannel.getName() + " does not exist.\nMake sure you use the name of the text channel, not a @mention of it.");
                                eb.setFooter(FooterMessage.footerMessage);
                                event.replyEmbeds(eb.build()).queue();


                            } catch (InsufficientPermissionException e) {
                                System.out.println("Cannot write to channel");
                            }catch(NullPointerException exp){}
                        }
                    } else {
                        try {

                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(16743936);
                            String myTitle = "Wrong usage";
                            eb.setTitle(myTitle);
                            eb.setDescription("Wrong format. Use : .//subscribe [Subreddit name] [Text channel]\nReplace [Subreddit name] with the name of the subreddit such as askreddit or dankmemes.\n" +
                                    "Replace [Text channel] with the name of your Text channel. Don't use an @mention, just type the name.");
                            eb.setFooter(FooterMessage.footerMessage);
                            event.replyEmbeds(eb.build()).queue();
                        } catch (InsufficientPermissionException e) {
                            System.out.println("Cannot write to channel");
                        }
                    }
                } else {
                    try {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(16743936);
                        String myTitle = "Missing permissions";
                        eb.setTitle(myTitle);
                        eb.setDescription("You must have the 'Redditor' Role or Administrator permissions for this command.");
                        eb.setFooter(FooterMessage.footerMessage);
                        event.replyEmbeds(eb.build()).queue();

                    } catch (InsufficientPermissionException e) {
                        System.out.println("Cannot write to channel");
                    }
                }
            } catch (InsufficientPermissionException e) {
                System.out.println("Cannot write to channel");
            }
        }

        if (event.getName().equals("unsubscribe")) {
            if ((event.getMember().getPermissions().contains(Permission.ADMINISTRATOR) || isRedditor)) {
                event.reply("Command received!");
                if (true) {
                    try {
                        String subreddit = event.getOption("subreddit").getAsString();
                        if (subreddit.startsWith("r/")) {
                            subreddit = subreddit.replace("r/", "");
                        }
                        database.unsubscribe(subreddit, shardManager.getTextChannelById(event.getMessageChannel().getId()));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(16743936);
                        String myTitle = "Wrong format";
                        eb.setTitle(myTitle);
                        eb.setDescription("Wrong format. Use : .//unsubscribe [Subreddit]\nReplace [Subreddit name] with the name of the subreddit you wish to remove.");
                        eb.setFooter(FooterMessage.footerMessage);
                        event.replyEmbeds(eb.build()).queue();
                    } catch (InsufficientPermissionException e) {
                        System.out.println("Cannot write to channel");
                    }
                }
            } else {
                try {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(16743936);
                    String myTitle = "Missing permissions";
                    eb.setTitle(myTitle);
                    eb.setDescription("You must have the 'Redditor' Role or Administrator permissions for this command.");
                    eb.setFooter(FooterMessage.footerMessage);
                    event.replyEmbeds(eb.build()).queue();

                } catch (InsufficientPermissionException e) {
                    System.out.println("Cannot write to channel");
                }
            }

        }

        if(event.getName().equals("list")){
            event.getMember().getPermissions();
            if(true) { // Override this permission
                if(true) {
                    try {
                        int pagenr = event.getOption("page").getAsInt();
                        if (pagenr > 0) {
                            String[] sublist = null;
                            ArrayList<String> subArrayList = new ArrayList<>();
                            try {
                                sublist = database.getGuildSubscriptionList(event.getGuild());
                                if(sublist!=null){
                                    for (String s : sublist) {
                                        if (!s.isEmpty() && !s.equals(" ")) {
                                            subArrayList.add(s);
                                        }
                                    }

                                    sublist = new String[subArrayList.size()];
                                    for(int q=0;q<subArrayList.size();q++){
                                        sublist[q] = subArrayList.get(q);
                                    }

                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            if (sublist != null) {
                                int pagelength = 10;
                                String subs = "";
                                for (int i =(pagenr-1)*pagelength; i < sublist.length && i<(((pagenr)*pagelength)); i++) {
                                    try {
                                        String[] subscription = sublist[i].split("/");
                                        String subreddit = subscription[0];
                                        TextChannel textChannel = shardManager.getTextChannelById(subscription[1]);
                                        if (textChannel != null) {
                                            subs = subs + "\n" + subreddit + " - " + textChannel.getName();
                                        } else {
                                            subs = subs + "\n" + subreddit + " - " + "UNKNOWN CHANNEL";
                                        }
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (subs.isEmpty()) {
                                    subs = "Empty Page";
                                }
                                try {
                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setColor(16743936);
                                    String myTitle = "Subscription List (Page " + pagenr + "/" + (int)Math.ceil(((double)sublist.length)/pagelength)+")";
                                    eb.setTitle(myTitle);
                                    eb.setDescription(subs);
                                    eb.addField("Total subscriptions",""+sublist.length,true);
                                    eb.setFooter(FooterMessage.footerMessage);
                                    event.replyEmbeds(eb.build()).queue();
                                } catch (InsufficientPermissionException e) {
                                    System.out.println("Cannot write to channel");
                                }
                            } else {
                                try {
                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setColor(16743936);
                                    String myTitle = "No subscriptions yet";
                                    eb.setTitle(myTitle);
                                    eb.setDescription("Use the .//subscribe command to subscribe to new subreddits!\nFind more info using the .//help command.");
                                    eb.setFooter(FooterMessage.footerMessage);
                                    event.replyEmbeds(eb.build()).queue();
                                } catch (InsufficientPermissionException e) {
                                    System.out.println("Cannot write to channel");
                                }
                            }
                        }else{
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(16743936);
                            String myTitle = "Page number can't be zero";
                            eb.setTitle(myTitle);
                            eb.setDescription("Please specify a page number of at least 1");
                            eb.setFooter(FooterMessage.footerMessage);
                            event.replyEmbeds(eb.build()).queue();
                        }
                    }catch(NumberFormatException e){
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(16743936);
                        String myTitle = "No page number specified";
                        eb.setTitle(myTitle);
                        eb.setDescription("Use the command as follows :\n.//List [Page Number]\nReplace [Page Number] with the page number you wish to view of your subscription list.");
                        eb.setFooter(FooterMessage.footerMessage);
                        event.replyEmbeds(eb.build()).queue();
                    }catch(ArrayIndexOutOfBoundsException e){
                        e.printStackTrace();
                    }
                }else{
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(16743936);
                    String myTitle = "No page number specified";
                    eb.setTitle(myTitle);
                    eb.setDescription("Use the command as follows :\n.//List [Page Number]\nReplace [Page Number] with the page number you wish to view of your subscription list.");
                    eb.setFooter(FooterMessage.footerMessage);
                    event.replyEmbeds(eb.build()).queue();
                }
            }else{
                try {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(16743936);
                    String myTitle = "Missing Permissions";
                    eb.setTitle(myTitle);
                    eb.setDescription("You must have the 'Redditor' role or Administrator privileges to use this command.");
                    eb.setFooter(FooterMessage.footerMessage);
                    event.replyEmbeds(eb.build()).queue();
                }catch(InsufficientPermissionException e){
                    System.out.println("Cannot write to channel");
                }
            }
        }

        if(event.getName().equals("activate")){
            try{
                if(database.userHasPremium(event.getUser().getId())){
                    int success = database.updatePremiumGuild(event.getUser().getId(),event.getGuild());
                    try {
                        if(success==1) {

                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(16743936);
                            String myTitle = "Premium activated!";
                            eb.setTitle(myTitle);
                            eb.setDescription("Your premium features have been activated on this server!\n" +
                                    "When you transfer your premium to a different server, your subscriptions will not be lost.\n"+
                                    "Simply use the same command to activate your premium on a different server.");
                            eb.setFooter(FooterMessage.footerMessage);
                            event.replyEmbeds(eb.build()).queue();

                        }else{
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setColor(16743936);
                            String myTitle = "Error activating";
                            eb.setTitle(myTitle);
                            eb.setDescription("Error activating, please try again or contact the developer if issue persists.");
                            eb.setFooter(FooterMessage.footerMessage);
                            event.replyEmbeds(eb.build()).queue();

                        }
                    } catch (InsufficientPermissionException e) {
                        System.out.println("Cannot write to channel");
                    }

                }else{
                    try {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setColor(16743936);
                        String myTitle = "No premium!";
                        eb.setTitle(myTitle);
                        eb.setDescription("You don't have access to Premium features.\nDonate in the Redditor discord server found below to get lifetime access to Premium features!");
                        eb.setFooter(FooterMessage.footerMessage);
                        event.replyEmbeds(eb.build()).queue();
                    } catch (InsufficientPermissionException e) {
                        System.out.println("Cannot write to channel");
                    }
                }
            }catch(SQLException e){e.printStackTrace();}
        }

        if(event.getName().equals("interval")){
            try {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(16743936);
                String myTitle = "Interval";
                eb.setTitle(myTitle);
                eb.setDescription("Last cycle it took me "+lastInterval+" seconds to fetch and post all new reddit posts!");
                eb.setFooter(FooterMessage.footerMessage);
                event.replyEmbeds(eb.build()).queue();
            }catch(InsufficientPermissionException e){
                System.out.println("Cannot write to channel");
            }
        }

        if(event.getName().equals("vote")){
            try {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(16743936);
                String myTitle = "Vote!";
                eb.setTitle(myTitle);
                eb.setDescription("Please vote for me on top.gg!\nhttps://top.gg/bot/557580985646972928\nPress the 'Vote' button on the link above!");
                //eb.setFooter("Get premium bot features in the community server!");
                eb.addField("Community server","Also consider joining our community & support server!\nhttps://discord.gg/B28urAZ",false);
                event.replyEmbeds(eb.build()).queue();
            }catch(InsufficientPermissionException e){
                System.out.println("Cannot write to channel");
            }
        }
    }

    private static ArrayList<RedditPost> getPosts(ArrayList<Subreddit> subList) {
        ArrayList<RedditPost> posts = new ArrayList<>();
        Subreddit[] subArray = new Subreddit[subList.size()];

        for(int i=0;i<subList.size();i++){
            subArray[i]=subList.get(i);
        }
        String subString="";
        for(int i=0;i<(subArray.length-1);i++){
            subString=subString+""+subArray[i].getName()+"+";
        }
        subString=subString+""+subArray[subArray.length-1].getName();
        //System.out.println("Updating "+subString);

        try {
            //System.out.println("Checking : "+subString+"\n");
            JSONObject json = RedditJSONHandler.readJsonFromUrl("https://www.reddit.com/r/" + subString + "/new.json?limit=100");
            JSONArray JSONPosts = json.getJSONObject("data").getJSONArray("children");
            int counter = 0;
            for (int i = 0; i < JSONPosts.length() && counter < 100; i++) {
                Long date = JSONPosts.getJSONObject(i).getJSONObject("data").getLong("created_utc");
                String subredditName = JSONPosts.getJSONObject(i).getJSONObject("data").getString("subreddit").toLowerCase();
                String permaLink = "";
                long lastupdated = 0;
                for (Subreddit subreddit : subArray) {
                    if (subredditName.equals(subreddit.getName().toLowerCase())) {
                        lastupdated = subreddit.lastChecked;
                    }
                }
                if (date <= lastupdated) {
                    //counter = 5000; //Stop iterating -> NOT SORTING BY NEW ANYMORE, DON'T DO THIS
                } else {
                    try {
                        permaLink = JSONPosts.getJSONObject(i).getJSONObject("data").getString("permalink");
                    } catch (JSONException ej) {
                        // System.out.println("Error with JSON request 1 (Subreddit :"+subreddit+")");
                    }
                    boolean duplicate = false;
                    Iterator<RedditPost> it = posts.iterator();
                    while (it.hasNext() && !duplicate) {
                        RedditPost nextPost = it.next();
                        if (nextPost.getPermaLink().equals(permaLink)) {
                            duplicate = true;
                        }
                    }
                    if (!duplicate && !permaLink.equals("")) {
                        String title = JSONPosts.getJSONObject(i).getJSONObject("data").getString("title");
                        String imageUrl = JSONPosts.getJSONObject(i).getJSONObject("data").getString("url");
                        String text = JSONPosts.getJSONObject(i).getJSONObject("data").getString("selftext");
                        boolean nsfw = JSONPosts.getJSONObject(i).getJSONObject("data").getBoolean("over_18");
                        RedditPost post = new RedditPost(imageUrl,subredditName, title, text, date, permaLink, nsfw);
                        posts.add(post);
                        counter++;
                    }
                }
            }
        } catch (FileNotFoundException fnf) {
            System.out.println("Error with JSON request 2 (Subreddit :)");
          /*  try {
                database.removeReddit(subreddit);
            } catch (SQLException e) {
                e.printStackTrace();
            }*/
        } catch (IOException e) {
            System.out.println("Error with JSON request, most likely 503 (Subreddit :)");
        }

        return posts;
    }

    private static int doesSubredditExist(String subreddit){
        subreddit = subreddit.toLowerCase();
        try {
            JSONObject json = RedditJSONHandler.readJsonFromUrl("https://www.reddit.com/r/"+subreddit+"/new.json?sort=new");
            JSONArray JSONPosts = json.getJSONObject("data").getJSONArray("children");
            if(JSONPosts.length() == 0){
                return 0;
            }else{
                return 1;
            }
        }catch(FileNotFoundException fnf){
            return -1;
        }catch(IOException e){
            System.out.println("Error with JSON request, most likely 503");
            return -1;
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        super.onGuildLeave(event);
        try{
            database.guildLeave(event.getGuild());
        }catch(SQLException e){e.printStackTrace();}
        System.out.println("Guild "+event.getGuild().getId()+" Removed");
    }

    private static void updateSubRedditNew(String subreddit, ArrayList<RedditPost> postList){
        subreddit = subreddit.toLowerCase();
        ArrayList<RedditPost> newPostList = new ArrayList<>();
        Iterator<RedditPost> it = postList.iterator();
        int postcounter = 0;
        while(it.hasNext()) {
            RedditPost nextPost = it.next();
            String permaLink = nextPost.getPermaLink();
            boolean duplicate = false;
            Iterator<RedditPost> newIt = newPostList.iterator();
            while(newIt.hasNext() && !duplicate){
                RedditPost nextNewPost = newIt.next();
                if(nextNewPost.getPermaLink().equals(permaLink)){
                    duplicate = true;
                    System.out.println("Duplicate found");
                }
            }
            if(!duplicate){
                newPostList.add(nextPost);
                postcounter++;
            }
        }
        if(postcounter>0) {
            System.out.println(postcounter + " New posts in r/" + subreddit);
        }
        try{
            ArrayList<String> channelList = database.getChannelsSubscribedToSubreddit(subreddit);
            if(channelList!=null){
                if(channelList.size()==0){
                    System.out.println("No channel subscribed to "+subreddit+". Removing from list");
                   try {
                        database.removeReddit(subreddit);
                    }catch(SQLException e){e.printStackTrace();}
                }else {
                    ArrayList<String> channelsPosted = new ArrayList<>();
                    for (String nextChannel : channelList) {
                        ArrayList<String> urlsPosted = new ArrayList<>();
                        TextChannel textChannel = shardManager.getTextChannelById(nextChannel);

                        if (textChannel != null) {
                            //  System.out.println("Posting to :"+textChannel.getId());
                            if(!channelsPosted.contains(textChannel.getId())) {
                                channelsPosted.add(textChannel.getId());
                                if (textChannel.getSlowmode() == 0) {

                                    //  System.out.println("4");
                                    Guild guild = textChannel.getGuild();
                                    Member selfMember = guild.getSelfMember();
                                    if (true) {
                                        if (selfMember.hasPermission(textChannel, Permission.MESSAGE_SEND)) {
                                            Iterator<RedditPost> postIterator = newPostList.iterator();
                                            int maxposts = 1;
                                            try {
                                                if (database.GuildHasPremium(guild)) {
                                                    maxposts = 5;
                                                }
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                            int maxPostCounter = 0;
                                            while (postIterator.hasNext() && maxPostCounter < maxposts) {
                                                // System.out.println("1");

                                                RedditPost myPost = postIterator.next();
                                                if(!urlsPosted.contains(myPost.getPermaLink())){
                                                urlsPosted.add(myPost.getPermaLink());
                                                if (!textChannel.isNSFW() && myPost.nsfw) {
                                                    try {
                                                        System.out.println("Tried to post NSFW post in non NSFW channel");
                                                        EmbedBuilder eb = new EmbedBuilder();
                                                        eb.setColor(16743936);
                                                        String myTitle = myPost.getTitle() + " - " + subreddit;
                                                        eb.setTitle(myTitle);
                                                        eb.setDescription("This is an NSFW post.\nPlease mark this text channel as NSFW to view the post.");
                                                        eb.setFooter(FooterMessage.footerMessage);
                                                        eb.addField("Post Url", "https://www.reddit.com" + myPost.getPermaLink(), false);
                                                        textChannel.sendMessageEmbeds(eb.build()).queue(null, new ErrorHandler()
                                                                .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> {
                                                                    System.out.println("Text Channel DELETED");
                                                                }));
                                                    } catch (InsufficientPermissionException e) {
                                                        database.unsubscribe(myPost.subreddit,textChannel);
                                                        System.out.println("Cannot write to channel");
                                                    } catch (ErrorResponseException e) {
                                                        database.unsubscribe(myPost.subreddit,textChannel);
                                                        System.out.println("Channel Not Found");
                                                    }
                                                } else {
                                                    maxPostCounter++;
                                                    if (myPost.getUrl().endsWith(".jpg") || myPost.getUrl().endsWith(".png")) {
                                                        if (selfMember.hasPermission(textChannel, Permission.MESSAGE_SEND)) {
                                                            // System.out.println("2");
                                                            //  System.out.println(myPost.getTitle());
                                                            EmbedBuilder eb = new EmbedBuilder();
                                                            eb.setColor(16743936);
                                                            String myTitle;
                                                            if (myPost.getTitle().length() > 151) {
                                                                myTitle = myPost.getTitle().substring(0, 150) + "... - " + subreddit;
                                                            } else {
                                                                myTitle = myPost.getTitle() + " - " + subreddit;
                                                            }
                                                            eb.setTitle(myTitle);
                                                            eb.setDescription("");
                                                            eb.setImage(myPost.getUrl());
                                                            eb.setFooter(FooterMessage.footerMessage);
                                                            eb.addField("Post Url", "https://www.reddit.com" + myPost.getPermaLink(), false);
                                                            try {
                                                                textChannel.sendMessageEmbeds(eb.build()).queue(null, new ErrorHandler()
                                                                        .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> {
                                                                            System.out.println("Text Channel DELETED");
                                                                        }));
                                                            } catch (InsufficientPermissionException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Cannot write to channel");
                                                            } catch (ErrorResponseException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Channel Not Found");
                                                            }
                                                            //  System.out.println("3");
                                                            // System.out.println(myPost.getTitle());
                                                        } else {
                                                            System.out.println("Cannot talk in channel");
                                                        }
                                                    } else {
                                                        //  System.out.println("3");
                                                        // System.out.println(myPost.getTitle());
                                                        String selftext = myPost.getSelftext();
                                                        if (selftext.length() >= 200) {
                                                            selftext = selftext.substring(0, 185) + "...";
                                                        }
                                                        if (myPost.getUrl().endsWith(".gifv") || myPost.getUrl().endsWith(".gif") || myPost.getUrl().startsWith("https://gfycat.com/") || myPost.getUrl().startsWith("https://redgifs.com/watch/")) {
                                                            try {

                                                                EmbedBuilder eb = new EmbedBuilder();
                                                                eb.setColor(16743936);
                                                                String myTitle;
                                                                if (myPost.getTitle().length() > 151) {
                                                                    myTitle = myPost.getTitle().substring(0, 150) + "... - " + subreddit;
                                                                } else {
                                                                    myTitle = myPost.getTitle() + " - " + subreddit;
                                                                }
                                                                eb.setTitle(myTitle);
                                                                eb.setDescription("https://www.reddit.com" + myPost.getPermaLink());
                                                               // eb.setFooter("Join our discord server at https://discord.gg/B28urAZ\nCome donate for Premium features!\n\n Want your sponsored message here? Contact us in the discord server above.");
                                                                textChannel.sendMessageEmbeds(eb.build()).queue(
                                                                        null, new ErrorHandler()
                                                                                .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> System.out.println("Text Channel DELETED"))
                                                                );

                                                                textChannel.sendMessage(myPost.getUrl()).queue(
                                                                        null, new ErrorHandler()
                                                                                .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> System.out.println("Text Channel DELETED"))
                                                                );

                                                            } catch (InsufficientPermissionException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Cannot write to channel");
                                                            } catch (ErrorResponseException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Channel Not Found");
                                                            }

                                                        } else {
                                                       /* try {
                                                            textChannel.sendMessage("-----\n" + myPost.getTitle() + "\n" + myPost.getUrl() + selftext + "\n" + "<https://www.reddit.com" + myPost.getPermaLink() + ">").queue(
                                                                    null, new ErrorHandler()
                                                                            .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> {
                                                                                System.out.println("Text Channel DELETED");
                                                                            })
                                                            );

                                                        } catch (InsufficientPermissionException e) {
                                                            System.out.println("Cannot write to channel");
                                                        }catch (ErrorResponseException e){
                                                            System.out.println("Channel Not Found");
                                                        }*/

                                                            try {
                                                                EmbedBuilder eb = new EmbedBuilder();
                                                                eb.setColor(16743936);
                                                                String myTitle;
                                                                if (myPost.getTitle().length() > 151) {
                                                                    myTitle = myPost.getTitle().substring(0, 150) + "... - " + subreddit;
                                                                } else {
                                                                    myTitle = myPost.getTitle() + " - " + subreddit;
                                                                }
                                                                eb.setTitle(myTitle);
                                                                eb.setDescription(selftext);
                                                                eb.setFooter(FooterMessage.footerMessage);
                                                                eb.addField("Post Url", "https://www.reddit.com" + myPost.getPermaLink(), false);
                                                                if (!myPost.getUrl().equals(("https://www.reddit.com" + myPost.getPermaLink()))) {
                                                                    eb.addField("Content", myPost.getUrl(), false);
                                                                }
                                                                textChannel.sendMessageEmbeds(eb.build()).queue(null, new ErrorHandler()
                                                                        .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> System.out.println("Text Channel DELETED")));
                                                            } catch (InsufficientPermissionException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Cannot write to channel");
                                                            } catch (ErrorResponseException e) {
                                                                database.unsubscribe(myPost.subreddit,textChannel);
                                                                System.out.println("Channel Not Found");
                                                            }

                                                        }
                                                    }
                                                }

                                                try {
                                                    Thread.sleep(60);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            }
                                        } else {
                                            database.unsubscribe(subreddit,textChannel);
                                            System.out.println("Cannot write to channel");
                                        }


                                    } else {
                                        //database.unsubscribe(subreddit,textChannel);
                                        try {
                                     /*   textChannel.sendMessage("Unsubscribed from "+subreddit+". Channel is not marked NSFW").queue(null, new ErrorHandler()
                                                .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> {
                                                    System.out.println("Text Channel DELETED");
                                                }));*/

                                        } catch (InsufficientPermissionException e) {
                                            System.out.println("Cannot write to channel");
                                        }
                                    }
                                } else {
                                    database.unsubscribe(subreddit,textChannel);
                                    System.out.println("Slowmode on " + textChannel.getId());
                            /*    try{
                                    database.unsubscribe(subreddit,textChannel);
                                }catch(SQLException e){e.printStackTrace();}*/ //Don't unsub
                                    try {
                                        EmbedBuilder eb = new EmbedBuilder();
                                        eb.setColor(16743936);
                                        String myTitle = "Slowmode enabled";
                                        eb.setDescription("Tried to post but slowmode is enabled.\nPlease disable slowmode on this text channelfor Redditor to work.");
                                        eb.setTitle(myTitle);
                                        eb.setFooter(FooterMessage.footerMessage);
                                        textChannel.sendMessageEmbeds(eb.build()).queue(null, new ErrorHandler()
                                                .handle(ErrorResponse.UNKNOWN_CHANNEL, (exception) -> System.out.println("Text Channel DELETED")));

                                    } catch (InsufficientPermissionException e) {
                                        database.unsubscribe(subreddit,textChannel);
                                        System.out.println("Cannot write to channel");
                                    }

                                }
                            }else{
                                System.out.println("************CHANNEL ALREADY POSTED TO***********");
                            }

                        } else {
                            //System.out.println("Cannot find text channel, Unsubscribing");
                        }

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }catch(SQLException e){e.printStackTrace();}
        //  System.out.println("5");
    }
    private static Guild[] getTop10(List<Guild> myList){
        ArrayList<Guild> top5 = new ArrayList<>();
        for(int i=0;i<20;i++) {
            Iterator<Guild> it = myList.iterator();
            Guild largest = null;
            if (it.hasNext()) {
                largest = it.next();
                while(top5.contains(largest)){
                    largest = it.next();
                }
            }
            while (it.hasNext() && !interrupted() && largest != null) {
                Guild nextGuild = it.next();
                if (nextGuild.getMembers().size() > largest.getMembers().size()) {
                    if(!top5.contains(nextGuild)) {
                        largest = nextGuild;
                    }
                }
            }
            top5.add(largest);
        }

        return new Guild[]{top5.get(0),top5.get(1),top5.get(2),top5.get(3),top5.get(4),top5.get(5),top5.get(6),top5.get(7),top5.get(8),top5.get(9),top5.get(10),top5.get(11),top5.get(12),top5.get(13),top5.get(14),top5.get(15),top5.get(16),top5.get(17),top5.get(18),top5.get(19)};
    }
}

