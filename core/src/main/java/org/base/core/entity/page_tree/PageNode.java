package org.base.core.entity.page_tree;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Nationalized;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "page_nodes")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "node_type")
public abstract class PageNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "name")
    private String name;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private PageNode parent;

    @Formula("parent_id")
    @JsonProperty("parent_id")
    private Long parentId;

    @JsonManagedReference
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 100)
    private List<PageNode> children = new ArrayList<>();

    @Column(name = "level")
    private Integer level;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Nationalized
    @Column(name = "slug")
    private String slug;

    @Formula("node_type")
    private String nodeType;

    @Pattern(regexp = "^[A-Z][a-zA-Z0-9]*$|^$", message = "Invalid icon name")
    @Column(name = "icon")
    private String icon;

//    @Override
//    public void serialize(PageNode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//        gen.writeStartObject();
//        // Reflect on fields
//        for (Field field : PageNode.class.getDeclaredFields()) {
//            if (!field.getName().equals("parent")) {
//                field.setAccessible(true);
//                try {
//                    Object propValue = field.get(value);
//                    gen.writeFieldName(field.getName());
//                    if (propValue == null) {
//                        gen.writeNull();
//                    } else {
//                        serializers.findValueSerializer(field.getType()).serialize(propValue, gen, serializers);
//                    }
//                } catch (IllegalAccessException e) {
//                    throw new IOException("Failed to access field: " + field.getName(), e);
//                }
//            }
//        }
//        // Add parentId
//        gen.writeFieldName("parentId");
//        if (value.getParent() != null) {
//            gen.writeNumber(value.getParent().getId());
//        } else {
//            gen.writeNull();
//        }
//        gen.writeEndObject();
//    }

//    @JsonProperty("parentId")
//    public Long getParentId() {
//        return parent != null ? parent.getId() : null;
//    }
}
