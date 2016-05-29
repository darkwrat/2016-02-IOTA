package su.iota.backend.messages.game.impl;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.behaviors.FromMessage;
import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.iota.backend.messages.OutgoingMessage;
import su.iota.backend.models.game.Coordinate;
import su.iota.backend.models.game.FieldItem;

import java.lang.reflect.Type;
import java.util.*;

public class GameStateMessage implements OutgoingMessage, FromMessage {

    @Nullable
    private ActorRef<?> from;

    @NotNull
    private UUID uuid = new UUID(0L, 0L);

    @Nullable
    private Integer ref;

    @NotNull
    private Collection<Integer> players = new HashSet<>();

    @NotNull
    private Map<Integer, Long> playerIds = new HashMap<>();

    @NotNull
    private Map<Integer, Integer> playerScores = new HashMap<>();

    @NotNull
    private Map<Integer, String> playerLogins = new HashMap<>();

    @NotNull
    private Map<Coordinate, FieldItem> field = new HashMap<>();

    public void setUuid(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public void setPlayerRef(@Nullable Integer playerRef) {
        ref = playerRef;
    }

    public void addPlayer(int playerRef, long id, int score, @NotNull String login) {
        players.add(playerRef);
        playerIds.put(playerRef, id);
        playerScores.put(playerRef, score);
        playerLogins.put(playerRef, login);
    }

    public void addFieldItem(@NotNull Coordinate coordnate, @NotNull FieldItem fieldItem) {
        field.put(coordnate, fieldItem);
    }

    @Nullable
    @Override
    public ActorRef<?> getFrom() {
        return from;
    }

    public void setFrom(@Nullable ActorRef<?> from) {
        this.from = from;
    }

    public static class Serializer implements JsonSerializer<GameStateMessage> {

        @Override
        public JsonElement serialize(GameStateMessage src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("__ok", true);
            if (src != null) {
                jsonObject.addProperty("state", src.uuid.toString());
                jsonObject.addProperty("ref", src.ref);
                jsonObject.add("players", serializePlayers(src));
                jsonObject.add("field", serializeField(src));
            }
            return jsonObject;
        }

        @NotNull
        private JsonArray serializePlayers(@NotNull GameStateMessage src) {
            final JsonArray jsonArray = new JsonArray();
            for (int playerRef : src.players) {
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("ref", playerRef);
                jsonObject.addProperty("id", src.playerIds.get(playerRef));
                jsonObject.addProperty("score", src.playerScores.get(playerRef));
                jsonObject.addProperty("login", src.playerLogins.get(playerRef));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }

        @NotNull
        private JsonArray serializeField(@NotNull GameStateMessage src) {
            final JsonArray jsonArray = new JsonArray();
            for (Map.Entry<Coordinate, FieldItem> entry : src.field.entrySet()) {
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("offx", entry.getKey().getOffX());
                jsonObject.addProperty("offy", entry.getKey().getOffY());
                jsonObject.add("item", serializeFieldItem(entry.getValue()));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        }

        @NotNull
        private JsonObject serializeFieldItem(@NotNull FieldItem src) {
            final JsonObject jsonObject = new JsonObject();
            if (!src.isEphemeral()) {
                jsonObject.addProperty("uuid", src.getUuid().toString());
                jsonObject.addProperty("concrete", src.isConcrete());
                if (src.isConcrete()) {
                    jsonObject.addProperty("color", String.valueOf(src.getColor()));
                    jsonObject.addProperty("shape", String.valueOf(src.getShape()));
                    jsonObject.addProperty("number", String.valueOf(src.getNumber()));
                }
            }
            return jsonObject;
        }

    }

}
