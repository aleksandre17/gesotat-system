package org.base.core.service;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.embedded.AccessControl;
import org.base.core.entity.page_tree.DirectoryNode;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.AccessControlRequest;
import org.base.core.model.request.NodeRequest;
import org.base.core.model.request.ParentRef;
import org.base.core.model.request.ReorderRequest;
import org.base.core.repository.PageNodeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PageStructureService {

    private final PageNodeRepository pageNodeRepository;

    @CacheEvict(value = "pageTree", allEntries = true)
    public PageNode create(NodeRequest req) {
        PageNode node = buildNode(req);
        applyCommonFields(node, req);
        resolveParent(node, req.getParent());
        node.setLevel(computeLevel(req.getParent()));
        node.setSortOrder(computeSortOrder(req.getParent(), req.getOrderIndex()));
        return pageNodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pageNode", key = "#id")
    public Optional<PageNode> findById(Long id) {
        return pageNodeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable("pageTree")
    public List<PageNode> getRootNodes() {
        return pageNodeRepository.findRoots();
    }

    @Transactional(readOnly = true)
    public List<PageNode> findByIds(List<Long> ids) {
        return pageNodeRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pageTree", key = "#rootsOnly + '-' + #nodeType + '-' + #page + '-' + #limit + '-' + #sort + '-' + #order")
    public List<PageNode> findAll(Boolean rootsOnly, String nodeType, int page, int limit, String sort, String order) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(order).orElse(Sort.Direction.ASC);
        Sort sortObj = Sort.by(direction, resolveSortField(sort));
        Pageable pageable = PageRequest.of(page - 1, limit, sortObj); // _page is 1-based

        if (rootsOnly != null && rootsOnly) {
            return getRootNodes();
        }

        if (nodeType != null && !nodeType.isBlank()) {
            return pageNodeRepository.findByType(resolveType(nodeType), pageable).getContent();
        }

        return pageNodeRepository.findAll(pageable).getContent();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pageTree", key = "'children-' + #parentId")
    public List<PageNode> getChildren(Long parentId) {
        return pageNodeRepository.findByParentIdOrdered(parentId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pageTree", key = "'dir-' + #excludeId")
    public List<PageNode> getDirectoryNodes(Long excludeId) {
        return pageNodeRepository.findAllDirectory(excludeId);
    }

    @CacheEvict(value = {"pageTree", "pageNode"}, allEntries = true)
    public PageNode update(Long id, NodeRequest req) {
        PageNode node = pageNodeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + id));
        applyCommonFields(node, req);
        applyTypeSpecificFields(node, req);
        resolveParent(node, req.getParent());
        if (req.getOrderIndex() != null) {
            node.setSortOrder(req.getOrderIndex());
        }
        return pageNodeRepository.save(node);
    }

    @CacheEvict(value = {"pageTree", "pageNode"}, allEntries = true)
    public void delete(Long id) {
        if (!pageNodeRepository.existsById(id)) {
            throw new NoSuchElementException("Page not found: " + id);
        }
        pageNodeRepository.deleteById(id);
    }

    @CacheEvict(value = {"pageTree", "pageNode"}, allEntries = true)
    public void reorder(List<ReorderRequest> requests) {
        for (ReorderRequest r : requests) {
            PageNode node = pageNodeRepository.findById(r.getPageId())
                    .orElseThrow(() -> new NoSuchElementException("Page not found: " + r.getPageId()));
            if (r.getParentId() != null) {
                PageNode parent = pageNodeRepository.findById(r.getParentId())
                        .orElseThrow(() -> new NoSuchElementException("Parent not found: " + r.getParentId()));
                node.setParent(parent);
                node.setLevel(parent.getLevel() + 1);
            } else {
                node.setParent(null);
                node.setLevel(1);
            }
            node.setSortOrder(r.getOrderIndex());
            pageNodeRepository.save(node);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private AccessControl toAccessControl(AccessControlRequest req) {
        if (req == null) return null;
        AccessControl ac = new AccessControl();
        ac.setUserId(req.getUserId());
        ac.setRoleId(req.getRoleId());
        ac.setPermissionId(req.getPermissionId());
        return ac;
    }

    private String resolveSortField(String sort) {
        return switch (sort != null ? sort : "") {
            case "name"  -> "name";
            case "level" -> "level";
            default      -> "sortOrder";  // orderIndex → sortOrder
        };
    }

    private Class<? extends PageNode> resolveType(String nodeType) {
        return switch (nodeType.toUpperCase()) {
            case "DIRECTORY" -> DirectoryNode.class;
            case "PAGE"      -> PageLeafNode.class;
            default -> throw new IllegalArgumentException("Unknown nodeType: " + nodeType);
        };
    }

    private PageNode buildNode(NodeRequest req) {
        if (Boolean.TRUE.equals(req.getIsFolder())) {
            DirectoryNode dir = new DirectoryNode();
            dir.setDescription(req.getDescription());
            dir.setAccessControl(toAccessControl(req.getAccessControl()));
            return dir;
        }
        PageLeafNode leaf = new PageLeafNode();
        leaf.setResource(req.getResource());
        leaf.setMetaTitle(req.getMetaTitle());
        leaf.setMetaDescription(req.getMetaDescription());
        leaf.setMetaDatabaseType(req.getMetaDatabaseType());
        leaf.setMetaDatabaseUrl(req.getMetaDatabaseUrl());
        leaf.setMetaDatabaseUser(req.getMetaDatabaseUser());
        leaf.setMetaDatabasePassword(req.getMetaDatabasePassword());
        leaf.setMetaDatabaseName(req.getMetaDatabaseName());
        return leaf;
    }

    private void applyCommonFields(PageNode node, NodeRequest req) {
        node.setName(req.getName());
        node.setSlug(req.getSlug());
        node.setIcon(req.getIcon() != null ? req.getIcon() : "folder");
    }

    private void applyTypeSpecificFields(PageNode node, NodeRequest req) {
        if (node instanceof DirectoryNode dir) {
            dir.setDescription(req.getDescription());
            dir.setAccessControl(toAccessControl(req.getAccessControl()));
        } else if (node instanceof PageLeafNode leaf) {
            leaf.setResource(req.getResource());
            leaf.setMetaTitle(req.getMetaTitle());
            leaf.setMetaDescription(req.getMetaDescription());
            leaf.setMetaDatabaseType(req.getMetaDatabaseType());
            leaf.setMetaDatabaseUrl(req.getMetaDatabaseUrl());
            leaf.setMetaDatabaseUser(req.getMetaDatabaseUser());
            leaf.setMetaDatabasePassword(req.getMetaDatabasePassword());
            leaf.setMetaDatabaseName(req.getMetaDatabaseName());
        }
    }

    private void resolveParent(PageNode node, ParentRef parentId) {
        if (parentId == null) {
            node.setParent(null);
            return;
        }
        PageNode parent = pageNodeRepository.findById(parentId.getId())
                .orElseThrow(() -> new NoSuchElementException("Parent not found: " + parentId));
        node.setParent(parent);
    }

    private int computeLevel(ParentRef parentId) {
        if (parentId == null) return 1;
        return pageNodeRepository.findById(parentId.getId())
                .map(p -> p.getLevel() + 1)
                .orElse(1);
    }

    private int computeSortOrder(ParentRef parentId, Integer requested) {
        if (requested != null) return requested;
        List<PageNode> siblings = parentId == null
                ? pageNodeRepository.findRoots()
                : pageNodeRepository.findByParentId(parentId.getId());
        return siblings.size() + 1;
    }
}