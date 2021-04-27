package org.yangxin.pool;

import org.yangxin.http.Constant;
import org.yangxin.reflection.ObjectFactory;
import org.yangxin.until.collections.SynchronizedQueue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class PooledObject<T> {

    private int capacity;
    private int initSize;
    private SynchronizedQueue<Object> pool;
    private ObjectFactory objectFactory;
    private Class<T> type;

    public PooledObject(int capacity, int initSize, Class<T> type){
        if ( 0 == capacity || (capacity==-1?Integer.MAX_VALUE:capacity) < initSize ) {
            throw new IllegalArgumentException("参数不合法");
        }
        this.capacity = capacity;
        this.initSize = initSize;
        this.type = type;
        pool = new SynchronizedQueue<Object>();

        objectFactory = new ObjectFactory();
        for (int i = 0; i < this.initSize; i++) {
            pool.offer(objectFactory.create(type));
        }

    }

    public PooledObject(Class<T> type){

        this(-1, Constant.COMPUTOR_CORE, type);
    }

    public <T> T borrowObject() throws InterruptedException{
        Object o = pool.poll();
        if ((-1 == capacity || pool.size() < capacity) && null == o) {
            o = objectFactory.create(type);
            pool.offer(o);
        }

        return (T)o;
    }

    public void backObject(T o){
        pool.offer(o);
    }
}
