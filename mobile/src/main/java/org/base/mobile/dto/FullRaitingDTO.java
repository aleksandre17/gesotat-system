package org.base.mobile.dto;

/**
 * DTO for full-raiting endpoint items.
 */

import java.util.List;

/**
 * DTO for full-raiting endpoint response.
 */
public class FullRaitingDTO {
    private final List<Item> result;
    private final int currentPage;
    private final int totalPages;
    private final String totalResults;

    public FullRaitingDTO(List<Item> result, int currentPage, int totalPages, String totalResults) {
        this.result = result;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalResults = totalResults;
    }

    public List<Item> getResult() {
        return result;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getTotalResults() {
        return totalResults;
    }

    public static class Item {
        private final String name;
        private final int value;

        public Item(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
