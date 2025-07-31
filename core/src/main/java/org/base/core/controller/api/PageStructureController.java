package org.base.core.controller.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.entity.page_tree.DirectoryNode;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.PageNodeMixIn;
import org.base.core.model.request.NodeRequest;
import org.base.core.service.PageStructureService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.Param;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pages")
@Api
@RequiredArgsConstructor
public class PageStructureController {

    private final PageStructureService pageStructureService;


    @PostMapping("create_alter")
    public ResponseEntity<PageNode> createNode(@RequestBody NodeRequest request) {
        return ResponseEntity.ok(pageStructureService.createNodeFromRequest(request));
    }

    @ResponseBody
    @GetMapping(path = "/roots", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PageNode>> getRootNodes() {
        return ResponseEntity.ok(pageStructureService.getRootNodes());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public List<PageNode> getAllPages(@Param("id") Long id, @Param("nodeType") String nodeType) {
        if (nodeType != null && id != null) {
            return pageStructureService.getDirectoryNodes(id);
        }
        return pageStructureService.getRootNodes(); // Return root pages
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<PageNode> getPage(@PathVariable Long id) {
        return pageStructureService.getPage(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PageNode> updatePage(@PathVariable Long id, @RequestBody NodeRequest updatedPage) {
        return pageStructureService.updatePage(id, updatedPage);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public ResponseEntity<Object> deletePage(@PathVariable Long id) {
        return pageStructureService.getPage(id)
                .map(page -> {
                    pageStructureService.deleteNode(id);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    private void reorder(ReorderRequest request) {
        PageNode page = pageStructureService.getPage(request.getPageId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid page ID"));
        page.setParent(request.getParentId() != null
                ? pageStructureService.getPage(request.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid parent ID"))
                : null);
        page.setSortOrder(request.getOrderIndex());
        pageStructureService.savePage(page);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Void> reorderPage(@RequestBody ReorderRequest request) {
        reorder(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reorders")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')") 
    public ResponseEntity<Void> reorderPages(@RequestBody List<ReorderRequest> requests) {
        for (ReorderRequest request : requests) {
            reorder(request);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<PageNode>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(pageStructureService.getChildren(parentId));
    }

    @PostMapping
    public ResponseEntity<PageNode> createNodeM(@RequestBody NodeRequest node) throws JsonMappingException {

        // 1. Use ObjectMapper to convert NodeRequest to a generic map
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> nodeMap = objectMapper.convertValue(node, Map.class);
        objectMapper.addMixIn(PageNode.class, PageNodeMixIn.class);

        // 2. Create the appropriate PageNode type based on isFolder flag
        PageNode pageNode;
        if (Boolean.TRUE.equals(node.getIsFolder())) {
            nodeMap.remove("resource");
            nodeMap.remove("metaTitle");
            nodeMap.remove("metaDescription");

            pageNode = new DirectoryNode();
        } else {
            nodeMap.remove("description");
            pageNode = new PageLeafNode();
        }
        nodeMap.remove("isFolder");

        // 3. Use ObjectMapper to map properties from the map to the PageNode object
        // This will dynamically set all matching properties
        objectMapper.updateValue(pageNode, nodeMap);


        return ResponseEntity.ok(pageStructureService.createNode(pageNode, node.getParentId()));
    }

    @PutMapping("/{nodeId}/order/{newOrder}")
    public ResponseEntity<PageNode> updateNodeOrder(
            @PathVariable Long nodeId,
            @PathVariable Integer newOrder) {
        return ResponseEntity.ok(pageStructureService.updateNodeOrder(nodeId, newOrder));
    }

    public static class ReorderRequest {
        private Long pageId;
        private Long parentId;
        private int orderIndex;

        public Long getPageId() {
            return pageId;
        }

        public void setPageId(Long pageId) {
            this.pageId = pageId;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }
    }
}

