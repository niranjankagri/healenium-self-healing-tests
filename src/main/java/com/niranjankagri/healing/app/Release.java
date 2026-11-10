package com.niranjankagri.healing.app;

/**
 * The two releases of the demo shop. Both are served at the same URL, so to the browser (and to Healenium)
 * release 2 is simply "the same page after a deployment".
 */
public enum Release {

    /** The release the tests and their locators were written for. */
    V1("v1", "Release 1: the locators in the page objects match the page"),

    /** A front-end redesign: every id and class the tests use is renamed, and elements are wrapped or moved. */
    V2("v2", "Release 2: every locator in the page objects is out of date");

    private final String folder;
    private final String description;

    Release(String folder, String description) {
        this.folder = folder;
        this.description = description;
    }

    /** Folder under {@code src/main/resources/shop/} that holds this release's files. */
    public String folder() {
        return folder;
    }

    /** One line for logs and reports. */
    public String description() {
        return description;
    }
}
