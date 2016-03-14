package ru.cdecl.pub.iota.resources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.cdecl.pub.iota.annotations.UserProfileDetailedView;
import ru.cdecl.pub.iota.annotations.UserProfileIdView;
import ru.cdecl.pub.iota.main.RestApplication;
import ru.cdecl.pub.iota.services.UserProfileService;
import ru.cdecl.pub.iota.models.UserProfile;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Singleton
@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @NotNull
    private final UserProfileService userProfileService;

    public UserResource(@NotNull UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GET
    @Path("{id}")
    @UserProfileDetailedView
    public Response getUserById(@PathParam("id") long userId, @Context HttpServletRequest httpServletRequest) {
        @Nullable final HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession == null) {
            return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        @Nullable final Object userIdFromSession = httpSession.getAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE);

        if (userIdFromSession != null && userIdFromSession instanceof Long && userId == (long) userIdFromSession) {
            @Nullable final UserProfile userProfile = userProfileService.getUserById(userId);

            if (userProfile != null) {
                return Response.ok(userProfile).build();
            }
        }

        return Response.status(Response.Status.NOT_FOUND).entity(RestApplication.EMPTY_RESPONSE).build();
    }

    @POST
    @UserProfileIdView
    public Response createUser(@NotNull UserCreateRequest userCreateRequest) {
        if (!userCreateRequest.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        if (!userProfileService.isUserPresent(userCreateRequest.getLogin())) {
            @NotNull final UserProfile newUserProfile = userCreateRequest.toUserProfile();

            if (userProfileService.addUser(newUserProfile)) {
                @Nullable final Long newUserId = newUserProfile.getUserId();
                assert (newUserId != null);

                authenticationService.setPasswordForUser(newUserId, userCreateRequest.getPassword());

                return Response.status(Response.Status.OK).entity(newUserProfile).build();
            }
        }

        return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteUser(@PathParam("id") long userId, @Context HttpServletRequest httpServletRequest) {
        @Nullable final HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession == null) {
            return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        @Nullable final Object userIdFromSession = httpSession.getAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE);

        if (userIdFromSession == null || !(userIdFromSession instanceof Long)) {
            return Response.status(Response.Status.NOT_FOUND).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        if (userId == (long) userIdFromSession) {
            userProfileService.deleteUser(userId);
            httpSession.invalidate();

            return Response.ok(RestApplication.EMPTY_RESPONSE).build();
        }

        return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
    }

    @POST
    @Path("{id}")
    public Response editUser(@PathParam("id") long userId, UserEditRequest userEditRequest, @Context HttpServletRequest httpServletRequest) {
        @Nullable final HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession == null) {
            return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        if (!userEditRequest.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        @Nullable final Object userIdFromSession = httpSession.getAttribute(RestApplication.SESSION_USER_ID_ATTRIBUTE);

        if (userIdFromSession == null || !(userIdFromSession instanceof Long)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(RestApplication.EMPTY_RESPONSE).build();
        }

        if (userId == (long) userIdFromSession) {

            if (!userProfileService.updateUser(userId, userEditRequest)) {
                return Response.status(Response.Status.CONFLICT).entity(RestApplication.EMPTY_RESPONSE).build();
            }

            authenticationService.setPasswordForUser(userId, userEditRequest.getPassword());
            httpSession.invalidate();

            return Response.ok(RestApplication.EMPTY_RESPONSE).build();
        }


        return Response.status(Response.Status.FORBIDDEN).entity(RestApplication.EMPTY_RESPONSE).build();
    }

}
