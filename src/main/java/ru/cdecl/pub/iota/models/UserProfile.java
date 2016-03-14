package ru.cdecl.pub.iota.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.cdecl.pub.iota.annotations.UserProfileDetailedView;
import ru.cdecl.pub.iota.annotations.UserProfileIdView;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

public class UserProfile {

    @Nullable
    private Long userId = null;
    @Nullable
    private String login = null;
    @Nullable
    private String email = null;
    @Nullable
    private String password = null;

    public UserProfile(@Nullable String login, @Nullable String email) {
        userId = null;
        this.login = login;
        this.email = email;
    }

    @Nullable
    @XmlElement(name = "id")
    @UserProfileIdView
    public Long getUserId() {
        return userId;
    }

    public void setUserId(@Nullable Long userId) {
        this.userId = userId;
    }

    @Nullable
    @UserProfileDetailedView
    public String getLogin() {
        return login;
    }

    public void setLogin(@NotNull String login) {
        this.login = login.trim();
    }

    @Nullable
    @UserProfileDetailedView
    public String getEmail() {
        return email;
    }

    public void setEmail(@NotNull String email) {
        this.email = email.trim();
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

}
