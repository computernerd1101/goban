package com.computernerd1101.goban.poet;

import kotlin.collections.ArraysKt;

public class GenerateAll {

    public static void main(String[] args) {
        String[] reversePreview;
        if (ArraysKt.contains(args, "--preview"))
            reversePreview = new String[0];
        else reversePreview = new String[] { "--no-preview" };
        GenerateGobanRowsKt.main();
        GenerateDateTablesKt.main();
        GenerateTreeViewKt.main(reversePreview);
        GenerateToolbarKt.main(reversePreview);
    }

}
