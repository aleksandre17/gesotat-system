package org.base.core.entity.page_tree;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;


@Entity
@Getter
@Setter
@DiscriminatorValue("PAGE")
public class PageLeafNode extends PageNode {

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String resource;

    @Nationalized
    @Column(name = "meta_title")
    private String metaTitle;

    @Nationalized
    @Column(name = "meta_database_url")
    private String metaDatabaseUrl;

    @Nationalized
    @Column(name = "meta_database_type")
    private String metaDatabaseType;

    @Nationalized
    @Column(name = "meta_database_user")
    private String metaDatabaseUser;

    @Nationalized
    @Column(name = "meta_database_password")
    private String metaDatabasePassword;

    @Nationalized
    @Column(name = "meta_database_name")
    private String metaDatabaseName;

    @Nationalized
    @Column(name = "meta_description")
    private String metaDescription;
}
