package org.base.core.repository;

import org.base.core.entity.page_tree.PageNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PageNodeRepository extends JpaRepository<PageNode, Long> {

    @Query("SELECT p, TYPE(p) FROM PageNode p WHERE p.parent IS NULL")
    List<PageNode> findRoots();

    @Query("SELECT p, TYPE(p) FROM PageNode p WHERE p.nodeType = 'DIRECTORY' and p.id != :id order by p.id, p.level, p.sortOrder, p.name")
    List<PageNode> findAllDirectory(Long id);

    List<PageNode> findByParentId(Long parentId);

    @Query("SELECT p FROM PageNode p WHERE p.level = :level")
    List<PageNode> findByLevel(int level);

    @Query("SELECT p FROM PageNode p WHERE p.parent.id = :parentId ORDER BY p.sortOrder")
    List<PageNode> findByParentIdOrdered(Long parentId);
}

