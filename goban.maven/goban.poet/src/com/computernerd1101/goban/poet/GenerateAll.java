package com.computernerd1101.goban.poet;

import kotlin.collections.ArraysKt;

public class GenerateAll {

    public static void main(String[] args) {
        String[] reversePreview;
        if (args != null && ArraysKt.contains(args, "--preview"))
            reversePreview = preview;
        else reversePreview = noPreview;
        GenerateGobanRowsKt.main();
        GenerateDateTablesKt.main();
        GenerateTreeViewKt.main(reversePreview);
        GenerateToolbarKt.main(reversePreview);
    }

    public static final String NO_PREVIEW = "--no-preview";
    private static final String[] noPreview = { NO_PREVIEW };
    private static final String[] preview = new String[0];

}
