package org.base.core.entity.page_tree;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class PageNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(name = "name")
    private String name;

    // @JsonBackReference — circular reference-ს ასარიდებლად (@JsonManagedReference-ის წყვილი)
    // parent object არ ჩანს response-ში; parentId @Formula-ით მოდის
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PageNode parent;

    // parentId პირდაპირ SQL-ით — parent proxy-ს ჩატვირთვის გარეშე
    @Formula("parent_id")
    @JsonProperty("parentId")
    private Long parentId;

    // LAZY by default; @BatchSize N+1-ს ასარიდებლად ბატჩებად ტვირთავს
    // @JsonManagedReference + Hibernate6Module(FORCE_LAZY_LOADING=false):
    //   loaded → ჩანს response-ში; unloaded proxy → null (ბაზა არ ხვდება)
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

    @Column(name = "icon")
    private String icon;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageNode other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}