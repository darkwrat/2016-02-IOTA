package ru.cdecl.pub.iota.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.cdecl.pub.iota.models.UserProfile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfileService {

    private final ConcurrentMap<Long, UserProfile> userIdToProfile = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> userLoginToId = new ConcurrentHashMap<>();

    public boolean isUserPresent(@NotNull String userLogin) {
        @Nullable final Long userId = userLoginToId.getOrDefault(userLogin, null);

        return userId != null && isUserPresent(userId);

    }

    public boolean isUserPresent (@Nullable Long userId) {
        return userId != null && userIdToProfile.containsKey(userId);

    }

    public boolean addUser(@NotNull UserProfile userProfile) {
        final String userLogin = userProfile.getLogin();

        if (userLogin == null || isUserPresent(userLogin)) {
            return false;
        }

        final long newUserId = ID_GENERATOR.getAndIncrement();

        userProfile.setUserId(newUserId);
        userIdToProfile.put(newUserId, userProfile);
        userLoginToId.put(userLogin, newUserId);

        return true;
    }

    public boolean updateUser(@Nullable Long userId, @NotNull UserProfile newUserProfile) {
        if (userId == null) {
            return false;
        }

        @Nullable final UserProfile oldUserProfile = userIdToProfile.getOrDefault(userId, null);

        if (oldUserProfile == null) {
            return false;
        }

        @Nullable final String newLogin = newUserProfile.getLogin();

        if (newLogin != null) {
            if (isUserPresent(newLogin)) {
                return false;
            }

            userLoginToId.remove(oldUserProfile.getLogin());
            oldUserProfile.setLogin(newLogin);
            userLoginToId.put(newLogin, userId);
        }

        @Nullable final String newEmail = newUserProfile.getEmail();

        if (newEmail != null) {
            oldUserProfile.setEmail(newEmail);
        }

        @Nullable final String newPassword = newUserProfile.getPassword();

        if (newPassword != null) {
            oldUserProfile.setPassword(newPassword.trim());
        }

        userIdToProfile.put(userId, oldUserProfile);

        return true;
    }

    public void deleteUser(@NotNull String userLogin) {
        deleteUser(userLoginToId.getOrDefault(userLogin, null));
    }

    public void deleteUser(@Nullable Long userId) {
        if (userId == null) {
            return;
        }

        @Nullable final UserProfile userProfile = userIdToProfile.getOrDefault(userId, null);

        if(userProfile == null) {
            return;
        }

        @Nullable final String userLogin = userProfile.getLogin();

        if (userLogin != null) {
            userLoginToId.remove(userLogin);
        }

        userIdToProfile.remove(userId);
    }

    public boolean checkUserPassword(@NotNull String userLogin, @NotNull String userPassword) {
        return checkUserPassword(userLoginToId.getOrDefault(userLogin, null), userPassword);
    }

    public boolean checkUserPassword(@Nullable Long userId, @NotNull String userPassword) {
        if (userId == null) {
            return false;
        }

        @Nullable final UserProfile userProfile = userIdToProfile.getOrDefault(userId, null);

        if (userProfile == null) {
            return false;
        }

        @Nullable final String actualUserPassword = userProfile.getPassword();

        return actualUserPassword != null && actualUserPassword.trim().equals(userPassword.trim());
    }

    @Nullable
    public UserProfile getUserByLogin(@NotNull String userLogin) {
        return getUserById(userLoginToId.getOrDefault(userLogin, null));

    }

    @Nullable
    public UserProfile getUserById(@Nullable Long userId) {
        return userId == null
                ? null
                : userIdToProfile.getOrDefault(userId, null);

    }

    @Nullable
    public Long getUserIdByLogin(@NotNull String userLogin) {
        return userLoginToId.getOrDefault(userLogin, null);
    }

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

}
