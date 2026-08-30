package com.tucanoo.davkit.demo

/**
 * One row per document, mirroring demo-spring-boot's entity. {@code version} (GORM optimistic
 * locking) and {@code lastUpdated} (auto-timestamp) are exactly what the provider needs for the
 * ETag contract: the ETag is {@code <id>-<version>}, so it moves on every save.
 */
class Document {

    String name
    byte[] bytes
    Date dateCreated
    Date lastUpdated

    static constraints = {
        name blank: false, unique: true
        bytes maxSize: 50 * 1024 * 1024
    }
}
