package id.payu.cache.serializer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Custom {@link RedisSerializer} that preserves the runtime type of cached values,
 * including top-level {@link java.util.List} payloads.
 *
 * <p>The default {@code GenericJackson2JsonRedisSerializer} cannot round-trip
 * collections under type-erased deserialization ({@code Object.class}): Jackson's
 * polymorphic typing is fundamentally limited because JSON arrays cannot carry
 * a type-id {@code @class} property. This serializer works around the
 * limitation with a two-part wire format:</p>
 *
 * <ol>
 *   <li>A type header: {@code <outerTypeName>} for simple values, or
 *       {@code <outerTypeName><<elementTypeName>>} for collections whose
 *       element type is discovered at serialize time by inspecting the first
 *       non-null element.</li>
 *   <li>A standard JSON body produced by a plain {@link ObjectMapper}
 *       (without polymorphic typing), so element classes round-trip
 *       naturally without nested wrappers.</li>
 * </ol>
 *
 * <p>Example payloads (delimiter = {@code |}):</p>
 * <ul>
 *   <li>{@code id.payu.cms.domain.dto.ContentResponse|{"id":"…","title":"…"}}</li>
 *   <li>{@code java.util.ImmutableCollections$List12<id.payu.cms.domain.dto.ContentResponse>|[{"id":"…","title":"A"},{"id":"…","title":"B"}]}</li>
 * </ul>
 *
 * <p>On deserialization the type header is parsed, the element type (if any)
 * is composed into a {@link JavaType} via Jackson's
 * {@code TypeFactory#constructCollectionType}, and the JSON body is read
 * with that target type so each element is reconstructed to its concrete
 * class — avoiding {@code ClassCastException: LinkedHashMap cannot be cast to ContentResponse}
 * (E2E-2026-06-13-06).</p>
 *
 * <p>This format is internal to the {@code cms-service} cache. The platform
 * standard ({@code GenericJackson2JsonRedisSerializer}) remains in use elsewhere;
 * a cross-service migration is tracked under the READY-001 follow-up.</p>
 */
public class TypedJsonRedisSerializer implements RedisSerializer<Object> {

    private static final char DELIMITER = '|';
    private static final char ELEMENT_OPEN = '<';
    private static final char ELEMENT_CLOSE = '>';

    private final ObjectMapper mapper;

    public TypedJsonRedisSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            StringBuilder header = new StringBuilder(value.getClass().getName());
            if (value instanceof Collection<?> coll && !coll.isEmpty()) {
                Object first = null;
                for (Object element : coll) {
                    if (element != null) {
                        first = element;
                        break;
                    }
                }
                if (first != null) {
                    header.append(ELEMENT_OPEN)
                        .append(first.getClass().getName())
                        .append(ELEMENT_CLOSE);
                }
            }
            String json = mapper.writeValueAsString(value);
            return (header.toString() + DELIMITER + json).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new SerializationException("Could not serialize value of type "
                + value.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object deserialize(byte[] source) throws SerializationException {
        if (source == null || source.length == 0) {
            return null;
        }
        String wire = new String(source, StandardCharsets.UTF_8);
        int delim = wire.indexOf(DELIMITER);
        if (delim <= 0 || delim == wire.length() - 1) {
            throw new SerializationException("Malformed typed payload: missing type header");
        }
        String header = wire.substring(0, delim);
        String json = wire.substring(delim + 1);

        try {
            String outerTypeName = header;
            String elementTypeName = null;
            int lt = header.indexOf(ELEMENT_OPEN);
            if (lt > 0 && header.charAt(header.length() - 1) == ELEMENT_CLOSE) {
                outerTypeName = header.substring(0, lt);
                elementTypeName = header.substring(lt + 1, header.length() - 1);
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> outerType = Class.forName(outerTypeName, true, cl);

            if (elementTypeName != null && Collection.class.isAssignableFrom(outerType)) {
                Class<?> elementType = Class.forName(elementTypeName, true, cl);
                @SuppressWarnings("unchecked")
                Class<? extends Collection<?>> collectionType =
                    (Class<? extends Collection<?>>) outerType;
                JavaType javaType = mapper.getTypeFactory()
                    .constructCollectionType(collectionType, elementType);
                return mapper.readValue(json, javaType);
            }

            Object raw = mapper.readValue(json, Object.class);
            if (outerType.isInstance(raw)) {
                return raw;
            }
            return mapper.convertValue(raw, outerType);
        } catch (ClassNotFoundException ex) {
            throw new SerializationException("Unknown cached type: " + header, ex);
        } catch (Exception ex) {
            throw new SerializationException("Could not deserialize payload with header "
                + header + ": " + ex.getMessage(), ex);
        }
    }
}



