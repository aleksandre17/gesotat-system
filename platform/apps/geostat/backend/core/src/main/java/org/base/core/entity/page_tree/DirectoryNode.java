package org.base.core.entity.page_tree;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.base.core.entity.embedded.AccessControl;
import org.hibernate.annotations.Nationalized;

@Entity
@Getter
@Setter
@DiscriminatorValue("DIRECTORY")
public class DirectoryNode extends PageNode {

    @Nationalized
    @Column(name = "description")
    private String description;

    @Embedded
    private AccessControl accessControl;
}
