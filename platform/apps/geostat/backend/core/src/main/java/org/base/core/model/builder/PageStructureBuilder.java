package org.base.core.model.builder;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.NodeRequest;
import org.base.core.model.request.ParentRef;
import org.base.core.service.PageStructureService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PageStructureBuilder {

    private final PageStructureService pageStructureService;

    public List<PageNode> createSampleStructure() {
        NodeBuilder mainMenu = folder("Main Menu")
                .withChildren(
                        folder("Products")
                                .withChildren(
                                        folder("Electronics")
                                                .withChildren(
                                                        folder("Computers")
                                                                .withChildren(
                                                                        page("Laptops", "laptops"),
                                                                        page("Desktops", "desktops")
                                                                ),
                                                        folder("Smartphones")
                                                                .withChildren(
                                                                        page("Android Phones", "android-phones"),
                                                                        page("iPhones", "iphones")
                                                                )
                                                )
                                ),
                        folder("About")
                                .withChildren(
                                        page("Company History", "history"),
                                        page("Contact Us", "contact")
                                )
                );

        return List.of(mainMenu.save(pageStructureService, null));
    }

    // ── Builder ────────────────────────────────────────────────────────────────

    public static class NodeBuilder {
        private final NodeRequest node;
        private final List<NodeBuilder> children = new ArrayList<>();

        private NodeBuilder(String name, boolean isFolder) {
            node = new NodeRequest();
            node.setName(name);
            node.setIsFolder(isFolder);
        }

        public NodeBuilder withDescription(String description) {
            node.setDescription(description);
            return this;
        }

        public NodeBuilder withResource(String resource) {
            node.setResource(resource);
            return this;
        }

        public NodeBuilder withMeta(String title, String description) {
            node.setMetaTitle(title);
            node.setMetaDescription(description);
            return this;
        }

        public NodeBuilder withChildren(NodeBuilder... children) {
            this.children.addAll(List.of(children));
            return this;
        }

        public PageNode save(PageStructureService service, Long parentId) {
            if (parentId != null) {
                ParentRef ref = new ParentRef();
                ref.setId(parentId);
                node.setParent(ref);
            }
            PageNode saved = service.create(node);
            for (int i = 0; i < children.size(); i++) {
                children.get(i).node.setOrderIndex(i);
                children.get(i).save(service, saved.getId());
            }
            return saved;
        }
    }

    // ── Factory helpers ────────────────────────────────────────────────────────

    private static NodeBuilder folder(String name) {
        return new NodeBuilder(name, true)
                .withDescription("Folder: " + name);
    }

    private static NodeBuilder page(String name, String slug) {
        return new NodeBuilder(name, false)
                .withResource("Resource for " + name)
                .withMeta("Page: " + name, "Description for " + name);
    }
}