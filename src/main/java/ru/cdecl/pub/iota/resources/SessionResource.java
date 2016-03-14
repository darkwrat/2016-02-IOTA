package ru.cdecl.pub.iota.resources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.cdecl.pub.iota.annotations.UserProfileIdView;
import ru.cdecl.pub.iota.main.RestApplication;
import ru.cdecl.pub.iota.models.UserProfile;
import ru.cdecl.pub.iota.services.UserProfileService;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/session")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    @NotNull
    private final UserProfileService userProfileService;

    public SessionResource(@NotNull UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GET
    @UserProfileIdView
    public Response getUserId(@Context HttpServletRequest httpServletRequest) {
        @Nullable final HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession == null) {
            return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        @Nullable final Object userId = httpSession.getAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE);

        if (userId != null && userId instanceof Long) {
            @Nullable final UserProfile userProfile = userProfileService.getUserById((Long) userId);

            if (userProfile != null) {
                return Response.ok(userProfile).build();
            }
        }

        return Response.status(Response.Status.UNAUTHORIZED).entity(RestApplication.EMPTY_RESPONSE).build();
    }

    @PUT
    @UserProfileIdView
    public Response doLogin(@NotNull UserLoginRequest userLoginRequest, @Context HttpServletRequest httpServletRequest) {
        @Nullable final Long userId = userProfileService.getUserIdByLogin(userLoginRequest.getLogin());
        boolean isPasswordOk = false;

        if (userId != null) {
            isPasswordOk = userProfileService.checkUserPassword(userId, userLoginRequest.getPassword());
        }

        if (isPasswordOk) {
            @NotNull final HttpSession httpSession = httpServletRequest.getSession();

            httpSession.setAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE, userId);

            @Nullable final UserProfile userProfile = userProfileService.getUserById(userId);

            return Response.ok(userProfile != null
                    ? userProfile
                    : RestApplication.EMPTY_RESPONSE
            ).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(RestApplication.EMPTY_RESPONSE).build();
    }

    @DELETE
    public Response doLogout(@Context HttpServletRequest httpServletRequest) {
        @Nullable final HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession == null) {
            return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        httpSession.setAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE, null);
        httpSession.invalidate();

        return Response.ok(RestApplication.EMPTY_RESPONSE).build();
    }


}
