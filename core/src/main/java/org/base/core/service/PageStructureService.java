package org.base.core.service;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.page_tree.DirectoryNode;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.NodeRequest;
import org.base.core.repository.PageNodeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageStructureService {

    private final PageNodeRepository pageNodeRepository;
    private static final int DEFAULT_MAX_LEVEL = 10; // Default maximum depth

    @Transactional
    public PageNode createNodeFromRequest(NodeRequest request) {
        List<PageNode> roots = createTreeStructure(List.of(request));
        return roots.get(0);
    }

    @Transactional
    public List<PageNode> createTreeStructure(List<NodeRequest> request) {

        List<PageNode> roots = new ArrayList<>();

        for (NodeRequest rootRequest : request) {
            rootRequest.setLevel(1);
            validateLevel(rootRequest, DEFAULT_MAX_LEVEL);
            PageNode root = createNodeStructure(rootRequest, null);
            roots.add(root);
        }

        return roots;
    }

    public Optional<PageNode> getPage(@PathVariable Long id) {
        return pageNodeRepository.findById(id);
    }

    public void savePage(PageNode page) {
        pageNodeRepository.save(page);
    }

    private PageNode createNodeStructure(NodeRequest request, PageNode parent) {

        PageNode node;

        if (request.getIsFolder()) {
            DirectoryNode folder = new DirectoryNode();
            folder.setDescription(request.getDescription());
            node = folder;
        } else {
            PageLeafNode page = new PageLeafNode();
            page.setResource(request.getResource());
            page.setMetaDatabaseType(request.getMetaDatabaseType());
            page.setMetaDatabaseName(request.getMetaDatabaseName());
            page.setMetaTitle(request.getMetaTitle());
            page.setMetaDatabaseUrl(request.getMetaDatabaseUrl());
            page.setMetaDatabaseUser(request.getMetaDatabaseUser());
            page.setMetaDatabasePassword(request.getMetaDatabasePassword());
            page.setMetaDescription(request.getMetaDescription());
            node = page;
        }

        if (request.getParentId() != null) {
            PageNode p = pageNodeRepository.findById(request.getParentId()).orElseThrow(() -> new IllegalArgumentException("Parent node not found"));
            node.setParent(p);
        } else {
            node.setParent(parent);
        }

        node.setSlug(request.getSlug());
        node.setName(request.getName());
        node.setLevel(request.getLevel());
        node.setIcon(request.getIcon() == null ? "folder" : request.getIcon());

        if (request.getSortOrder() != null) {
            node.setSortOrder(request.getSortOrder());
        } else {
            List<PageNode> siblings = request.getParentId() == null ?
                    pageNodeRepository.findRoots() :
                    pageNodeRepository.findByParentId(request.getParentId());
            node.setSortOrder(siblings.size() + 1);
        }

        // Save the node first to get its ID
        node = pageNodeRepository.save(node);

        // Create children if any
        if (request.getChildren() != null && !request.getChildren().isEmpty()) {
            int childOrder = 1;
            for (NodeRequest childRequest : request.getChildren()) {
                // Set child level
                childRequest.setLevel(request.getLevel() + 1);
                validateLevel(childRequest, DEFAULT_MAX_LEVEL);

                if (childRequest.getSortOrder() == null) {
                    childRequest.setSortOrder(childOrder++);
                }
                createNodeStructure(childRequest, node);
            }
        }

        return node;
    }

    public ResponseEntity<PageNode> updatePage(Long id, NodeRequest request) {
        return pageNodeRepository.findById(id).map(node -> {
            // Handle node type change if needed
            boolean isCurrentlyFolder = node instanceof DirectoryNode;
            boolean shouldBeFolder = request.getIsFolder();

            if (isCurrentlyFolder != shouldBeFolder) {
                // Create new instance of correct type but preserve ID and children
                PageNode newNode = shouldBeFolder ? new DirectoryNode() : new PageLeafNode();
                newNode.setId(node.getId());
                newNode.setChildren(node.getChildren());
                node = newNode;
            }

            // Update type-specific fields
            if (node instanceof DirectoryNode) {
                ((DirectoryNode) node).setDescription(request.getDescription());
            } else if (node instanceof PageLeafNode) {
                PageLeafNode leafNode = (PageLeafNode) node;
                leafNode.setMetaDatabaseType(request.getMetaDatabaseType());
                leafNode.setMetaDatabaseUrl(request.getMetaDatabaseUrl());
                leafNode.setMetaDatabaseUser(request.getMetaDatabaseUser());
                leafNode.setMetaDatabasePassword(request.getMetaDatabasePassword());
                leafNode.setMetaDatabaseName(request.getMetaDatabaseName());
                leafNode.setResource(request.getResource());
                leafNode.setMetaTitle(request.getMetaTitle());
                leafNode.setMetaDescription(request.getMetaDescription());
            }

            // Update parent relationship
            node.setParent(request.getParentId() != null ?
                    pageNodeRepository.findById(request.getParentId())
                            .orElseThrow(() -> new IllegalArgumentException("Parent node not found")) :
                    null);

            node.setNodeType(request.getNodeType());
            // Update common properties
            node.setName(request.getName());
            node.setSlug(request.getSlug());
            node.setLevel(request.getLevel());
            node.setIcon(request.getIcon() == null ? "folder" : request.getIcon());

            // Handle sort order
            node.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() :
                    (request.getParentId() == null ?
                            pageNodeRepository.findRoots() :
                            pageNodeRepository.findByParentId(request.getParentId())).size() + 1);

            // Save node and process children
            node = pageNodeRepository.save(node);

            if (request.getChildren() != null && !request.getChildren().isEmpty()) {
                int childOrder = 1;
                for (NodeRequest childRequest : request.getChildren()) {
                    childRequest.setLevel(request.getLevel() + 1);
                    validateLevel(childRequest, DEFAULT_MAX_LEVEL);

                    if (childRequest.getSortOrder() == null) {
                        childRequest.setSortOrder(childOrder++);
                    }
                    createNodeStructure(childRequest, node);
                }
            }

            return ResponseEntity.ok(node);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }



    @Transactional(readOnly = true)
    public List<PageNode> getRootNodes() {
        return pageNodeRepository.findRoots();
    }

    @Transactional(readOnly = true)
    public List<PageNode> getDirectoryNodes(Long selfId) {
        return pageNodeRepository.findAllDirectory(selfId);
    }

    @Transactional(readOnly = true)
    public List<PageNode> getChildren(Long parentId) {
        return pageNodeRepository.findByParentIdOrdered(parentId);
    }

    @Transactional
    public PageNode createNode(PageNode node, Long parentId) {
        if (parentId != null) {
            PageNode parent = pageNodeRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent node not found"));
            node.setParent(parent);
        }

        // Set the sort order to be last in the current level
        List<PageNode> siblings = parentId == null ?
                pageNodeRepository.findRoots() :
                pageNodeRepository.findByParentId(parentId);
        node.setSortOrder(siblings.size() + 1);

        return pageNodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        pageNodeRepository.deleteById(nodeId);
    }

    @Transactional
    public PageNode updateNodeOrder(Long nodeId, Integer newOrder) {
        PageNode node = pageNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found"));

        node.setSortOrder(newOrder);
        return pageNodeRepository.save(node);
    }


    private void validateLevel(NodeRequest request, int maxLevel) {
        if (request.getLevel() > maxLevel) {
            throw new IllegalArgumentException(
                    String.format("Node '%s' exceeds maximum allowed level of %d",
                            request.getName(), maxLevel)
            );
        }
    }


}

