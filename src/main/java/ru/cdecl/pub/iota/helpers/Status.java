package ru.cdecl.pub.iota.helpers;

public class Status {
    public static final int OK = 0;
    public static final int WRONG_LOGIN_OR_PASSWORD = 1;
    public static final int USER_ALREADY_EXIST = 2;
    public static final int UNAUTHORIZED = 3;
    public static final int NO_PERMISSION = 4;

    public static final String OK_MSG = "Ok";
    public static final String WRONG_LOGIN_OR_PASSWORD_MSG = "Wrong login or password";
    public static final String USER_ALREADY_EXIST_MSG = "User already exist";
    public static final String UNAUTHORIZED_MSG = "Unauthorized";
    public static final String NO_PERMISSION_MSG = "No permission for this action";
}
