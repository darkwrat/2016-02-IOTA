package ru.cdecl.pub.iota.models;

import ru.cdecl.pub.iota.models.base.BaseUserIdResponse;

import javax.xml.bind.annotation.XmlType;

@XmlType(name="")
public class UserLoginResponse extends BaseUserIdResponse {

    public UserLoginResponse() {
        super();
    }

    public UserLoginResponse(long userId) {
        super(userId);
    }

    public UserLoginResponse(long userId, int status, String message) {
        super(userId, status, message);
    }
}
