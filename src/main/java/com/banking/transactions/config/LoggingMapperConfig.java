package com.banking.transactions.config;

import com.banking.transactions.annotations.Censor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@Configuration
public class LoggingMapperConfig {


    protected static class CensorSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (!Objects.isNull(value) && value instanceof String str) {
                String censored;
                if (str.length() == 1) {
                    censored = "*";
                } else if (str.length() == 2) {
                    censored = str.charAt(0) + "*";
                } else {
                    censored = str.charAt(0) + "*".repeat(str.length() - 2) + str.charAt(str.length() - 1);
                }
                gen.writeString(censored);
                return;
            }
            gen.writeString("********");
        }
    }

    protected static class CensorBeanSerializerModifier extends BeanSerializerModifier {

        private final transient JsonSerializer<Object> censorSerializer = new CensorSerializer();

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            for (BeanPropertyWriter writer : beanProperties) {
                if (writer.getMember().hasAnnotation(Censor.class)) {
                    writer.assignSerializer(censorSerializer);
                }
            }
            return beanProperties;
        }
    }

    @Bean("loggingMapper")
    public ObjectMapper loggingMapper() {
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new CensorBeanSerializerModifier());
        return JsonMapper.builder()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()
                .findAndRegisterModules()
                .registerModule(module)
                .setTimeZone(TimeZone.getDefault());
    }
}
