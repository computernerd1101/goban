package com.computernerd1101.goban.poet;

import kotlin.collections.ArraysKt;

public class GenerateAll {

    public static void main(String[] args) {
        GenerateGobanRowsKt.main();
        GenerateDateTablesKt.main();
        String[] reversePreview;
        if (args != null && ArraysKt.contains(args, "--preview"))
            reversePreview = NO_ARGS;
        else reversePreview = new String[] { NO_PREVIEW };
        GenerateToolbarKt.main(reversePreview);
        reversePreview(reversePreview);
        GenerateTreeViewKt.main(reversePreview);
    }

    private static void reversePreview(String[] args) {
        if (args.length > 0) args[0] = NO_PREVIEW;
    }

    public static final String NO_PREVIEW = "--no-preview";
    public static final String[] NO_ARGS = new String[0];

}
