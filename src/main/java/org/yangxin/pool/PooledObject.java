package org.yangxin.pool;

import org.yangxin.http.Constant;
import org.yangxin.reflection.ObjectFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class PooledObject<T> {

    private int capacity;
    private int initSize;
    private LinkedBlockingDeque<Object> pool;
    private ObjectFactory objectFactory;
    private Class<T> type;

    public PooledObject(int capacity, int initSize, Class<T> type){
        if ( 0 == capacity || capacity < initSize ) {
            throw new IllegalArgumentException("参数不合法");
        }
        this.capacity = capacity;
        this.initSize = initSize;
        this.type = type;
        pool = new LinkedBlockingDeque<Object>(capacity);
        objectFactory = new ObjectFactory();
        for (int i = 0; i < this.initSize; i++) {
            pool.offer(objectFactory.create(type));
        }

    }

    public PooledObject(Class<T> type){

        this(Integer.MAX_VALUE>>>26, Constant.COMPUTOR_CORE*50, type);
    }

    public <T> T borrowObject() throws InterruptedException{
        Object o = null;
        if (pool.size() < capacity) {
            o = objectFactory.create(type);
            pool.offer(o);
        } else {
            o=pool.poll(3, TimeUnit.SECONDS);
        }
        return (T)o;
    }

    public void backObject(T o){
        pool.offer(o);
    }
}
