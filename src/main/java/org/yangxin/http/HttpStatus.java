package org.yangxin.http;

public enum HttpStatus {

    OK(200),
    Create(201),
    RESOURCES(209),

    MovePermanently(301),

    BadRequest(400),
    NotFound(404),

    InternalServerError(500)
    ;

    private int statCode;

    private HttpStatus(int statCode){
        this.statCode =statCode;
    }

    public int getStatCode() {
        return statCode;
    }
}
