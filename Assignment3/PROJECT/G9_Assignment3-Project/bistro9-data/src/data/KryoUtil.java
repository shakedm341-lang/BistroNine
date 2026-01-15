// Ilya Zeldner
package data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy; 
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoUtil {

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        
        // Disable Unsafe Memory Access
        // This tells Kryo: "Don't use the deprecated Unsafe methods."
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        
        return kryo;
    });

    /**
     * Serializes a given object into a byte array using Kryo.
     * Use this method to prepare an object for transmission over a network or storage.
     * * @param object The object to be serialized.
     * @return A byte array representing the serialized object.
     */
    public static byte[] serialize(Object object) {
        Kryo kryo = kryoThreadLocal.get();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        kryo.writeClassAndObject(output, object);
        output.close();
        return outputStream.toByteArray();
    }

    /**
     * Deserializes a byte array back into its original Java object.
     * * @param bytes The byte array containing the serialized object data.
     * @return The reconstructed Object, or null if the input byte array is null.
     */
    public static Object deserialize(byte[] bytes) {
        if (bytes == null) return null;
        Kryo kryo = kryoThreadLocal.get();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(inputStream);
        return kryo.readClassAndObject(input);
    }
}