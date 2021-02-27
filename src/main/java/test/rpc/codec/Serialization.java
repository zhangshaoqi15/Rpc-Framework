package test.rpc.codec;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 	序列化工具类
 */
public class Serialization {

	//schema缓存
    private static Map<Class<?>, Schema<?>> schemaCached = new ConcurrentHashMap<>();
    //一个实例化对象的工具类
    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 获取schema（序列化的规则）
     * @param <T>
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
	private static <T> Schema<T> getSchema(Class<T> cls) {
		Schema<T> schema = (Schema<T>) schemaCached.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(cls);
            if (schema != null) {
                schemaCached.put(cls, schema);
            }
        }
        return schema;
    }

	/**
	 * 序列化：对象->字节数组
	 * @param <T>
	 * @param obj
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public static <T> byte[] serialize(T obj) {
		Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化：字节数组->对象
     * @param <T>
     * @param data
     * @param cls
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        try {
            T message = objenesis.newInstance(cls);
            Schema<T> schema = getSchema(cls);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}