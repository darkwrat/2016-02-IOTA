package su.iota.backend.frontend;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.comsat.webactors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import com.esotericsoftware.minlog.Log;
import com.google.gson.*;
import org.glassfish.hk2.api.ServiceLocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.iota.backend.messages.OutgoingMessage;
import su.iota.backend.settings.SettingsService;
import su.iota.backend.misc.ServiceUtils;
import su.iota.backend.models.UserProfile;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static co.paralleluniverse.comsat.webactors.HttpResponse.error;
import static co.paralleluniverse.comsat.webactors.HttpResponse.ok;
import static javax.servlet.http.HttpServletResponse.*;
import static su.iota.backend.misc.SuspendableUtils.rethrowConsumer;

@WebActor(httpUrlPatterns = {"/session", "/user/*"}, webSocketUrlPatterns = {"/ws"})
public final class FrontendActor extends BasicActor<Object, Void> {

    private boolean isInitialized = false;
    private String contextPath;
    private FrontendService frontendService;
    private Set<ActorRef<WebMessage>> webSockets = new HashSet<>();

    private void init() throws InterruptedException, SuspendExecution {
        final ServiceLocator serviceLocator = ServiceUtils.getServiceLocator();
        final SettingsService settingsService = serviceLocator.getService(SettingsService.class);
        contextPath = settingsService.getServerContextPathSetting();
        frontendService = serviceLocator.getService(FrontendService.class);
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        if (!isInitialized) {
            init();
            isInitialized = true;
        }
        //noinspection InfiniteLoopStatement
        while (true) {
            final Object message = receive(5, TimeUnit.SECONDS);
            if (message instanceof WebMessage) {
                handleWebMessage((WebMessage) message);
            } else if (message instanceof OutgoingMessage) {
                final WebMessage jsonMessage = new WebDataMessage(self(), getGson().toJson(message));
                webSockets.stream().forEach(rethrowConsumer(ws -> {
                    ws.send(jsonMessage);
                }));
            }
            checkCodeSwap();
        }
    }

    private void handleWebMessage(WebMessage message) throws SuspendExecution {
        if (message instanceof HttpRequest) {
            final HttpRequest httpRequest = (HttpRequest) message;
            routeHttpRequest(httpRequest);
        } else if (message instanceof WebSocketOpened) {
            final ActorRef<? extends WebMessage> webSocketActor = message.getFrom();
            watch(webSocketActor);
            //noinspection unchecked
            webSockets.add((ActorRef<WebMessage>) webSocketActor);
        } else if (message instanceof WebDataMessage) {
            //noinspection SuspiciousMethodCalls
            if (!webSockets.contains(message.getFrom())) {
                throw new AssertionError();
            }
            handleWebSocketMessage((WebDataMessage) message);
        }
    }

    @Override
    protected Object handleLifecycleMessage(LifecycleMessage m) {
        final Object result = super.handleLifecycleMessage(m);
        if (m instanceof ExitMessage) {
            final ActorRef dyingActor = ((ExitMessage) m).getActor();
            if (webSockets.contains(dyingActor)) {
                webSockets.remove(dyingActor);
            }
        }
        return result;
    }

    private void handleWebSocketMessage(WebDataMessage message) {
        if (message.isBinary()) {
            Log.warn("Got binary message: " + message.getFrom().toString());
            return;
        }
        try {
            final JsonObject jsonMessage = new JsonParser().parse(message.getStringBody()).getAsJsonObject();
            Log.info(jsonMessage.toString()); // todo
        } catch (JsonSyntaxException | IllegalStateException ex) {
            Log.warn("Cannot deserialize message:", ex);
        }
    }

    private void routeHttpRequest(HttpRequest httpRequest) throws SuspendExecution {
        final String resourceUri = getResourceUri(httpRequest);
        if (resourceUri.startsWith("/session")) {
            handleHttpSessionRequest(httpRequest);
        } else if (resourceUri.startsWith("/user/")) {
            handleHttpConcreteUserRequest(httpRequest);
        } else if (resourceUri.startsWith("/user")) {
            handleHttpUserRequest(httpRequest);
        } else {
            throw new AssertionError(resourceUri);
        }
    }

    private void handleHttpSessionRequest(HttpRequest httpRequest) throws SuspendExecution {
        final JsonObject jsonObject = new JsonObject();
        switch (httpRequest.getMethod()) {
            case "GET": {
                handleHttpBusinessSessionGet(httpRequest, jsonObject);
                break;
            }
            case "PUT": {
                handleHttpBusinessSessionPut(httpRequest, jsonObject);
                break;
            }
            case "DELETE":
                handleHttpBusinessSessionDelete(httpRequest, jsonObject);
                break;
            default:
                respondWithError(httpRequest, SC_METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handleHttpBusinessSessionGet(HttpRequest httpRequest, JsonObject jsonObject) throws SuspendExecution {
        final UserProfile signedInUser = frontendService.getSignedInUser();
        if (signedInUser != null) {
            jsonObject.addProperty("id", signedInUser.getId());
        }
        jsonObject.addProperty("__ok", signedInUser != null);
        respondWithJson(httpRequest, jsonObject);
    }

    private void handleHttpBusinessSessionPut(HttpRequest httpRequest, JsonObject jsonObject) throws SuspendExecution {
        try {
            final UserProfile userProfile = new Gson().fromJson(httpRequest.getStringBody(), UserProfile.class);
            final boolean isSignedIn = frontendService.signIn(userProfile);
            if (isSignedIn) {
                final UserProfile signedInUser = frontendService.getSignedInUser();
                if (signedInUser == null) {
                    throw new AssertionError();
                }
                jsonObject.addProperty("id", frontendService.getSignedInUser().getId());
            }
            jsonObject.addProperty("__ok", isSignedIn);
            respondWithJson(httpRequest, jsonObject);
        } catch (JsonSyntaxException ex) {
            respondWithError(httpRequest, SC_BAD_REQUEST, ex);
        }
    }

    private void handleHttpBusinessSessionDelete(HttpRequest httpRequest, JsonObject jsonObject) throws SuspendExecution {
        frontendService.signOut();
        jsonObject.addProperty("__ok", true);
        respondWithJson(httpRequest, jsonObject);
    }

    private void handleHttpUserRequest(HttpRequest httpRequest) throws SuspendExecution {
        if (!httpRequest.getMethod().equals("PUT")) {
            respondWithError(httpRequest, SC_METHOD_NOT_ALLOWED);
            return;
        }
        final JsonObject jsonObject = new JsonObject();
        try {
            final UserProfile userProfile = new Gson().fromJson(httpRequest.getStringBody(), UserProfile.class);
            final boolean isSignedUp = frontendService.signUp(userProfile);
            if (isSignedUp) {
                jsonObject.addProperty("id", userProfile.getId());
            }
            jsonObject.addProperty("__ok", isSignedUp);
            respondWithJson(httpRequest, jsonObject);
        } catch (JsonSyntaxException ex) {
            respondWithError(httpRequest, SC_BAD_REQUEST, ex);
        }
    }

    private void handleHttpConcreteUserRequest(HttpRequest httpRequest) throws SuspendExecution {
        final Long userId = getUserIdFromResourceUri(getResourceUri(httpRequest));
        if (userId == null) {
            respondWithError(httpRequest, SC_BAD_REQUEST, new AssertionError());
            return;
        }
        final JsonObject jsonObject = new JsonObject();
        switch (httpRequest.getMethod()) {
            case "GET":
                handleHttpConcreteUserGet(httpRequest, jsonObject, userId);
                break;
            case "POST":
                handleHttpConcreteUserPost(httpRequest, jsonObject, userId);
                break;
            case "DELETE":
                handleHttpConcreteUserDelete(httpRequest, jsonObject, userId);
                break;
            default:
                respondWithError(httpRequest, SC_METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handleHttpConcreteUserGet(HttpRequest httpRequest, JsonObject jsonObject, Long userId) throws SuspendExecution {
        try {
            final Gson gson = getGson();
            final UserProfile userProfile = new UserProfile();
            userProfile.setId(userId);
            final boolean isGetUserDetailsOk = frontendService.getUserDetails(userProfile);
            if (isGetUserDetailsOk) {
                gson.toJsonTree(userProfile).getAsJsonObject().entrySet().stream().forEach(e -> jsonObject.add(e.getKey(), e.getValue()));
            }
            jsonObject.addProperty("__ok", isGetUserDetailsOk);
            respondWithJson(httpRequest, jsonObject);
        } catch (JsonSyntaxException ex) {
            respondWithError(httpRequest, SC_BAD_REQUEST, ex);
        }
    }

    private void handleHttpConcreteUserPost(HttpRequest httpRequest, JsonObject jsonObject, Long userId) throws SuspendExecution {
        try {
            final UserProfile userProfile = getGson().fromJson(httpRequest.getStringBody(), UserProfile.class);
            jsonObject.addProperty("__ok", frontendService.editProfile(userProfile));
            respondWithJson(httpRequest, jsonObject);
        } catch (JsonSyntaxException ex) {
            respondWithError(httpRequest, SC_BAD_REQUEST, ex);
        }
    }

    private void handleHttpConcreteUserDelete(HttpRequest httpRequest, JsonObject jsonObject, Long userId) throws SuspendExecution {
        try {
            final UserProfile userProfile = getGson().fromJson(httpRequest.getStringBody(), UserProfile.class);
            jsonObject.addProperty("__ok", frontendService.deleteUser(userProfile));
            respondWithJson(httpRequest, jsonObject);
        } catch (JsonSyntaxException ex) {
            respondWithError(httpRequest, SC_BAD_REQUEST, ex);
        }
    }

    private Gson getGson() {
        return defaultGsonBuilderFunction(new GsonBuilder());
    }

    private Gson defaultGsonBuilderFunction(GsonBuilder gsonBuilder) {
        return gsonBuilder.excludeFieldsWithoutExposeAnnotation().create();
    }

    private void respondWithJson(HttpRequest httpRequest, Object object) throws SuspendExecution {
        respondWithJson(httpRequest, object, this::defaultGsonBuilderFunction);
    }

    private void respondWithJson(HttpRequest httpRequest, Object object, Function<GsonBuilder, Gson> gsonFunction) throws SuspendExecution {
        final Gson gson = gsonFunction.apply(new GsonBuilder());
        httpRequest.getFrom().send(ok(self(), httpRequest, gson.toJson(object)).setContentType("application/json").build());
    }

    private void respondWithError(HttpRequest httpRequest, int code) throws SuspendExecution {
        respondWithError(httpRequest, code, null);
    }

    private void respondWithError(HttpRequest httpRequest, int code, @Nullable Throwable cause) throws SuspendExecution {
        httpRequest.getFrom().send(error(self(), httpRequest, code, cause).build());
    }

    private @NotNull String getResourceUri(HttpRequest httpRequest) {
        if (!isInitialized) {
            throw new IllegalStateException();
        }
        return httpRequest.getRequestURI().substring(contextPath.length());
    }

    private @Nullable Long getUserIdFromResourceUri(String resourceUri) {
        try {
            return Long.parseLong(resourceUri.substring(resourceUri.lastIndexOf('/') + 1));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

}
