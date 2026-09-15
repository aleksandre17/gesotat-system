package org.base.api.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.PageNodeMixIn;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    /**
     * Prevents Jackson from force-loading uninitialized Hibernate lazy proxies
     * during serialization. Unloaded associations serialize as null instead of
     * triggering a DB query outside a transaction.
     */
    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, false);
        return module;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.mixIn(PageNode.class, PageNodeMixIn.class);
            builder.modulesToInstall(new com.fasterxml.jackson.databind.Module() {
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
                                        if (node.has("parentId")) {
                                            Long parentId = node.get("parentId").isNull() ? null : node.get("parentId").longValue();
                                            if (parentId != null) {
                                                PageNode parent = new PageNode() {};
                                                parent.setId(parentId);
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