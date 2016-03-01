package ru.cdecl.pub.iota.models;

import ru.cdecl.pub.iota.models.base.BaseUserIdResponse;

import javax.xml.bind.annotation.XmlType;

@XmlType(name="")
public class UserCreateResponse extends BaseUserIdResponse {

    public UserCreateResponse() {
        super();
    }

    public UserCreateResponse(long userId) {
        super(userId);
    }

    public UserCreateResponse(long userId, int status, String message) {
        super(userId, status, message);
    }

}
