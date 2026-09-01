package com.tucanoo.davkit.demo

/** One page: the documents table; the edit links come from the plugin's taglib. */
class HomeController {

    def index() {
        [documents: Document.list(sort: 'name')]
    }
}
