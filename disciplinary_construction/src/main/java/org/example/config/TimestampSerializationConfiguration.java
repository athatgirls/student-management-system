package org.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.example.util.UtcTimestamps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Configuration
public class TimestampSerializationConfiguration {
    // Deadlines and activity times are user-entered local wall times, NOT UTC event timestamps.
    private static final Set<String> EVENT_FIELDS = Set.of("createTime", "updateTime", "submissionTime",
            "auditTime", "checkInTime", "approvalTime", "reviewTime");
    @Bean
    public SimpleModule utcEventTimestamps() {
        SimpleModule module = new SimpleModule("utc-event-timestamps");
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                    BeanDescription description, List<BeanPropertyWriter> properties) {
                for (BeanPropertyWriter property : properties) {
                    if (EVENT_FIELDS.contains(property.getName()) && property.getType().hasRawClass(LocalDateTime.class)) {
                        property.assignSerializer(new JsonSerializer<Object>() {
                            @Override public void serialize(Object value, JsonGenerator generator,
                                    SerializerProvider provider) throws IOException {
                                generator.writeString(UtcTimestamps.toWire((LocalDateTime) value));
                            }
                        });
                    }
                }
                return properties;
            }
        });
        return module;
    }
}
