package ru.cdecl.pub.iota.main;

import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jetbrains.annotations.NotNull;
import ru.cdecl.pub.iota.annotations.UserProfileDetailedView;
import ru.cdecl.pub.iota.annotations.UserProfileIdView;
import ru.cdecl.pub.iota.resources.SessionResource;
import ru.cdecl.pub.iota.resources.UserResource;
import ru.cdecl.pub.iota.services.UserProfileService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("api")
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, new Annotation[]{});
        register(EntityFilteringFeature.class);

        @NotNull final UserProfileService userProfileService = new UserProfileService();

        register(new UserResource(userProfileService));
        register(new SessionResource(userProfileService));
    }

    public static final Object EMPTY_RESPONSE = new Object();
    public static final String SESSION_USER_ID_ATTRIBUTE = "user_id";

}
