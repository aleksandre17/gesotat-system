package org.base.api.setting;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.PageNodeMixIn;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.mixIn(PageNode.class, PageNodeMixIn.class);
            builder.modulesToInstall(new Module() {
                @Override
                public String getModuleName() {
                    return "PageNodeMixInModule";
                }

                @Override
                public Version version() {
                    return Version.unknownVersion();
                }

                @Override
                public void setupModule(SetupContext context) {
                    context.setMixInAnnotations(PageNode.class, PageNodeMixIn.class);
                    context.addSerializers(new SimpleSerializers() {
                        @Override
                        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
                            if (type.getRawClass().isAssignableFrom(PageNode.class)) {
                                return new JsonSerializer<PageNode>() {
                                    @Override
                                    public void serialize(PageNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                                        gen.writeStartObject();
                                        // Serialize all fields except parent
//                                        for (BeanPropertyWriter prop : beanDesc.findProperties()) {
//                                            if (!prop.getName().equals("parent")) {
//                                                prop.serializeAsField(value, gen, serializers);
//                                            }
//                                        }
//
//                                        JsonSerializer<Object> defaultSerializer = serializers.findValueSerializer(PageNode.class);
//                                        JsonSerializer<Object> filteredSerializer = new JsonSerializer<Object>() {
//                                            @Override
//                                            public void serialize(Object obj, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//                                                gen.writeStartObject();
//                                                BeanSerializerBase beanSerializer = (BeanSerializerBase) defaultSerializer;
//                                                for (BeanPropertyWriter prop : beanSerializer.properties()) {
//                                                    if (!prop.getName().equals("parent")) {
//                                                        prop.serializeAsField(obj, gen, serializers);
//                                                    }
//                                                }
//                                                // Add parentId
//                                                gen.writeFieldName("parentId");
//                                                if (((PageNode) obj).getParent() != null) {
//                                                    gen.writeNumber(((PageNode) obj).getParent().getId());
//                                                } else {
//                                                    gen.writeNull();
//                                                }
//                                                gen.writeEndObject();
//                                            }
//                                        };
//                                        filteredSerializer.serialize(value, gen, serializers);

                                        for (BeanPropertyDefinition propDef : beanDesc.findProperties()) {
                                            if (!propDef.getName().equals("parent")) {
                                                Object propValue = propDef.getAccessor().getValue(value);
                                                gen.writeFieldName(propDef.getName());
                                                if (propValue == null) {
                                                    gen.writeNull();
                                                } else {
                                                    serializers.findValueSerializer(propDef.getPrimaryType()).serialize(propValue, gen, serializers);
                                                }
                                            }
                                        }

                                        // Add parentId
                                        gen.writeFieldName("parentId");
                                        if (value.getParent() != null) {
                                            gen.writeNumber(value.getParent().getId());
                                        } else {
                                            gen.writeNull();
                                        }
                                        gen.writeEndObject();
                                    }
                                };
                            }
                            return null;
                        }
                    });
                    context.addDeserializers(new SimpleDeserializers() {
                        @Override
                        public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) {
                            if (type.getRawClass().isAssignableFrom(PageNode.class)) {
                                return new JsonDeserializer<PageNode>() {
                                    @Override
                                    public PageNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                                        JsonNode node = p.getCodec().readTree(p);
                                        ObjectMapper mapper = (ObjectMapper) p.getCodec();
                                        PageNode pageNode = mapper.treeToValue(node, PageNode.class);
                                        // Handle parentId
                                        if (node.has("parentId")) {
                                            Long parentId = node.get("parentId").isNull() ? null : node.get("parentId").longValue();
                                            if (parentId != null) {
                                                PageNode parent = new PageNode() {
                                                    @Override
                                                    public Long getId() {
                                                        return parentId;
                                                    }
                                                };
                                                pageNode.setParent(parent);
                                            } else {
                                                pageNode.setParent(null);
                                            }
                                        }
                                        return pageNode;
                                    }
                                };
                            }
                            return null;
                        }
                    });
                }
            });
        };
    }
}
