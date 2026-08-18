package com.novastack.sms.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Module springPageModule() {
        SimpleModule module = new SimpleModule("springPage");
        module.addSerializer((Class) Page.class, new JsonSerializer<Page>() {
            @Override
            public void serialize(Page value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeStartObject();
                serializers.defaultSerializeField("content", value.getContent(), gen);
                gen.writeNumberField("totalElements", value.getTotalElements());
                gen.writeNumberField("totalPages", value.getTotalPages());
                gen.writeNumberField("size", value.getSize());
                gen.writeNumberField("number", value.getNumber());
                gen.writeBooleanField("first", value.isFirst());
                gen.writeBooleanField("last", value.isLast());
                gen.writeBooleanField("empty", value.isEmpty());
                gen.writeNumberField("numberOfElements", value.getNumberOfElements());
                gen.writeEndObject();
            }
        });
        return module;
    }
}
