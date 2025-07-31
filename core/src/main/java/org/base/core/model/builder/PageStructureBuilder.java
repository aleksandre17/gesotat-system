package org.base.core.model.builder;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.page_tree.PageNode;
import org.base.core.model.request.NodeRequest;
import org.base.core.service.PageStructureService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PageStructureBuilder {

    private final PageStructureService pageStructureService;

    public List<PageNode> createSampleStructure() {
        // Create the structure with levels
        NodeRequest mainMenu = folder("Main Menu", 1)
                .withChildren(
                        folder("Products", 2)
                                .withChildren(
                                        folder("Electronics", 3)
                                                .withChildren(
                                                        folder("Computers", 4)
                                                                .withChildren(
                                                                        page("Laptops", "laptops", 5).build(),
                                                                        page("Desktops", "desktops", 5).build()
                                                                ).build(),
                                                        folder("Smartphones", 4)
                                                                .withChildren(
                                                                        page("Android Phones", "android-phones", 5).build(),
                                                                        page("iPhones", "iphones", 5).build()
                                                                ).build()
                                                ).build()
                                ).build(),
                        folder("About", 2)
                                .withChildren(
                                        page("Company History", "history", 3).build(),
                                        page("Contact Us", "contact", 3).build()
                                ).build()
                ).build();


        return List.of(pageStructureService.createNodeFromRequest(mainMenu));
    }

    public static class NodeBuilder {
        private final NodeRequest node;

        private NodeBuilder(String name, boolean isFolder, int level) {
            node = new NodeRequest();
            node.setName(name);
            node.setIsFolder(isFolder);
            node.setLevel(level);
            node.setChildren(new ArrayList<>());
        }

        public NodeBuilder withDescription(String description) {
            node.setDescription(description);
            return this;
        }

        public NodeBuilder withResource(String content) {
            node.setResource(content);
            return this;
        }

        public NodeBuilder withMeta(String title, String description) {
            node.setMetaTitle(title);
            node.setMetaDescription(description);
            return this;
        }

        public NodeBuilder withChildren(NodeRequest... children) {
            node.getChildren().addAll(List.of(children));
            return this;
        }

        public NodeRequest build() {
            return node;
        }
    }

    private static NodeBuilder folder(String name, int level) {
        return new NodeBuilder(name, true, level)
                .withDescription("Folder: " + name);

    }

    private static NodeBuilder page(String name, String slug, int level) {
        return new NodeBuilder(name, false, level)
                .withResource("Resource for " + name)
                .withMeta("Page: " + name, "Description for " + name);

    }
}

