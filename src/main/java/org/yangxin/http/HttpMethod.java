package org.yangxin.http;

/**
 * http请求方法
 */
public enum HttpMethod {

    GET("get"), POST("post");

    private String name;

    private HttpMethod(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

}
