package ru.cdecl.pub.iota.models.base;

import javax.xml.bind.annotation.XmlElement;

public class BaseApiResponse {
    private int status;
    private String message;

    public BaseApiResponse() {

    }

    public BaseApiResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @XmlElement(name="status")
    public int getStatus() {
        return status;
    }

    @XmlElement(name="message")
    public String getMessage() {
        return message;
    }
}
