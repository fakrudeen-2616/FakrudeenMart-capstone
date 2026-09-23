package com.fakrudeenmart.model;

public final class CatalogOptions {
    private CatalogOptions() {}

    public enum StockFilter {
        ALL, IN_STOCK, OUT_OF_STOCK;

        public static StockFilter from(String value) {
            if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) return ALL;
            if ("in".equalsIgnoreCase(value) || "in_stock".equalsIgnoreCase(value)) return IN_STOCK;
            if ("out".equalsIgnoreCase(value) || "out_of_stock".equalsIgnoreCase(value)) return OUT_OF_STOCK;
            throw new IllegalArgumentException("Invalid stock filter.");
        }
    }

    public enum SortOption {
        NEWEST("newest", "p.created_at DESC, p.id DESC"),
        PRICE_ASC("price_asc", "p.price ASC, p.id DESC"),
        PRICE_DESC("price_desc", "p.price DESC, p.id DESC"),
        NAME_ASC("name_asc", "LOWER(p.name) ASC, p.id DESC"),
        NAME_DESC("name_desc", "LOWER(p.name) DESC, p.id DESC");

        private final String parameter;
        private final String sql;

        SortOption(String parameter, String sql) { this.parameter = parameter; this.sql = sql; }
        public String getParameter() { return parameter; }
        public String getSql() { return sql; }

        public static SortOption from(String value) {
            if (value == null || value.isBlank()) return NEWEST;
            for (SortOption option : values()) if (option.parameter.equalsIgnoreCase(value)) return option;
            throw new IllegalArgumentException("Invalid sort option.");
        }
    }
}
