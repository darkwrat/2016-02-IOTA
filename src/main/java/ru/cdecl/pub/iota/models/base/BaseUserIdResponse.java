package ru.cdecl.pub.iota.models.base;

import ru.cdecl.pub.iota.models.base.BaseApiResponse;

import javax.xml.bind.annotation.XmlElement;

public class BaseUserIdResponse extends BaseApiResponse {

    private long userId;

    public BaseUserIdResponse() {
        userId = -1;
    }

    public BaseUserIdResponse(long userId) {
        this.userId = userId;
    }

    public BaseUserIdResponse(long userId, int status, String message) {
        super(status, message);
        this.userId = userId;
    }

    @XmlElement(name="id")
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

}
