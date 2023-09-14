package dev.darealturtywurty.superturtybot.commands.util;

import com.google.gson.JsonObject;
import dev.darealturtywurty.superturtybot.core.command.CommandCategory;
import dev.darealturtywurty.superturtybot.core.command.CoreCommand;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.PaginatedEmbed;
import lombok.Data;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GetRobloxUserFriendListCommand extends CoreCommand {

    public GetRobloxUserFriendListCommand() {
        super(new Types(true,false,false,false));
    }

    @Override
    public List<OptionData> createOptions() {
        return List.of(new OptionData(OptionType.STRING, "userid", "The userid of the roblox user.", true));
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.UTILITY;
    }

    @Override
    public String getDescription() {
        return "Returns the roblox user friend list";
    }

    @Override
    public String getName() {
        return "roblox-friend";
    }

    @Override
    public String getRichName() {
        return "Roblox Friend";
    }

    @Override
    public Pair<TimeUnit, Long> getRatelimit() {
        return Pair.of(TimeUnit.SECONDS, 30L);
    }

    @Override
    protected void runSlash(SlashCommandInteractionEvent event) {
        String robloxUserId = event.getOption("userid", null, OptionMapping::getAsString);

        if(robloxUserId == null){
            reply(event, "❌ You must provide a valid userid!", false, true);
            return;
        }

        String RobloxSearchForUsernameUrl = "https://friends.roblox.com/v1/users/%s/friends"
                .formatted(robloxUserId);


        final Request request = new Request.Builder().url(RobloxSearchForUsernameUrl).get().build();

        try(Response response = new OkHttpClient().newCall(request).execute()){
            if(!response.isSuccessful()){
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            ResponseBody body = response.body();
            if(body == null){
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }
            String bodyString = body.string();

            if (bodyString.isBlank()) {
                event.getHook()
                        .sendMessage("❌ Failed to get response!")
                        .mentionRepliedUser(false)
                        .queue();
                return;
            }

            RobloxFriendData robloxResponse = RobloxFriendData.fromJsonString(bodyString);
            List<FriendData> robloxData = robloxResponse.data;

            if(robloxData.isEmpty()) {
                reply(event, "❌ Failed to get data!", false, true);
                return;
            }
            event.deferReply().queue();
            var contents = new PaginatedEmbed.ContentsBuilder();

            for (FriendData data : robloxData) {

                contents.field(data.name, data.id +
                        "\n**Display Name**: " + data.displayName +
                        "\n**Has Verified Badge**: " + data.hasVerifiedBadge +
                        "\n**Is Online**: " + data.isOnline +
                        "\n**Is Deleted**: " + data.isDeleted);
            }

            var embed = new PaginatedEmbed.Builder(4, contents)
                    .title("Roblox UserID: " + robloxUserId)
                    .color(0xecbc4c)
                    .authorOnly(event.getUser().getIdLong())
                    .build(event.getJDA());

            embed.send(event.getHook(), () -> event.getHook().editOriginal("❌ Failed to list roblox friend list!").queue());

        }catch(IOException exception){
            reply(event, "❌ Failed to response!");
            Constants.LOGGER.error("Failed to get response!", exception);
        }
        return;
    }
    @Data
    private class RobloxFriendData{
        List<FriendData> data;
        private static RobloxFriendData fromJsonString(String json) {
            JsonObject object = Constants.GSON.fromJson(json, JsonObject.class);
            return Constants.GSON.fromJson(object, RobloxFriendData.class);
        }
    }

    @Data
    public class FriendData{
        private boolean isOnline;
        private boolean isDeleted;
        private int friendFrequentScore;
        private int friendFrequentRank;
        private boolean hasVerifiedBadge;
        private String description;
        private String created;
        private boolean isBanned;
        private String externalAppDisplayName;
        private long id;
        private String name;
        private String displayName;

    }

}