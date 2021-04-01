package org.yangxin.reflection;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

public class ObjectFactory {

    public <T> T create(Class<T> type) {
        return create(type, null, null);
    }

    public <T> T create(Class<T> type, List<Class<?>> constructorArgsTypes, List<Object> constructorArgs){
        Constructor<T> constructor;
        try {
            if (null == constructorArgs || null == constructorArgsTypes ) {
                constructor = type.getDeclaredConstructor();
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                return constructor.newInstance();
            }
            constructor = type.getDeclaredConstructor(constructorArgsTypes.toArray(new Class[constructorArgsTypes.size()]));
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance(constructorArgs.toArray());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ReflectionException("创建对象失败", e);
        }

    }

}
