package com.computernerd1101.goban.poet;

public class GenerateAll {

    public static void main(String[] args) {
        GenerateGobanRowsKt.main();
        GenerateDateTablesKt.main();
        String[] noPreview = { "--no-preview" };
        GenerateTreeViewKt.main(noPreview);
        GenerateToolbarKt.main(noPreview);
    }

}
