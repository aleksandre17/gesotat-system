package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.NodeRequest;
import org.base.core.model.request.ReorderRequest;
import org.base.core.service.PageStructureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pages")
@Api
@RequiredArgsConstructor
public class PageStructureController {

    private final PageStructureService pageStructureService;

    @PostMapping("create_alter")
    public ResponseEntity<PageNode> createNode(@RequestBody NodeRequest request) {
        return ResponseEntity.ok(pageStructureService.create(request));
    }

    @ResponseBody
    @GetMapping(path = "/roots", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PageNode>> getRootNodes() {
        return ResponseEntity.ok(pageStructureService.getRootNodes());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PageNode> create(@RequestBody @Valid NodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageStructureService.create(request));
    }


    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<PageNode>> getAll(
            @RequestParam(required = false, defaultValue = "false") boolean rootsOnly,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "1")    int _page,
            @RequestParam(defaultValue = "1000") int _limit,
            @RequestParam(defaultValue = "orderIndex") String _sort,
            @RequestParam(defaultValue = "ASC")  String _order) {

        // getDirectoryNodes: all directories excluding the given id
        if (nodeType != null && id != null && !id.isBlank()) {
            return ResponseEntity.ok(pageStructureService.getDirectoryNodes(Long.parseLong(id.trim())));
        }

        // getMany: ?id=1,2,3
        if (id != null && !id.isBlank()) {
            List<Long> ids = id.lines()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .toList();
            return ResponseEntity.ok(pageStructureService.findByIds(ids));
        }

        // getManyReference: children of a parent
        if (parentId != null) {
            return ResponseEntity.ok(pageStructureService.getChildren(parentId));
        }

        return ResponseEntity.ok(pageStructureService.findAll(rootsOnly, nodeType, _page, _limit, _sort, _order));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<PageNode> findById(@PathVariable Long id) {
        return pageStructureService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<PageNode>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(pageStructureService.getChildren(parentId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PageNode> update(@PathVariable Long id, @RequestBody @Valid NodeRequest request) {
        return pageStructureService.findById(id)
                .map(existing -> ResponseEntity.ok(pageStructureService.update(id, request)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return pageStructureService.findById(id)
                .map(existing -> {
                    pageStructureService.delete(id);
                    return ResponseEntity.<Void>noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/reorders")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Void> reorder(@RequestBody @Valid List<ReorderRequest> requests) {
        pageStructureService.reorder(requests);
        return ResponseEntity.noContent().build();
    }
}