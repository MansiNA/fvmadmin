package com.example.application.service;

import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Route("file-tree")
//public class FileTree extends VerticalLayout {
public class FileTree extends Div {

    /*
     * This static code block only sets up some artificial file tree, for users to browse.
     * Normally, you would likely want to expose some existing directory tree.
     */
    static {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File root = new File(tmpDir.getAbsolutePath() + File.separator + "Table Viewer");
            boolean createdRoot = root.mkdir();
            if (createdRoot){
                File subdir = new File(root.getAbsolutePath() + File.separator + "Mailbox");
                boolean createdSubDir = subdir.mkdir();
                if (createdSubDir) {
                    File four = new File(subdir.getAbsolutePath() + File.separator + "Config");
                    four.createNewFile();
                }
            }
            File one = new File(root.getAbsolutePath() + File.separator + "Abnahme");
            one.createNewFile();
            File two = new File(root.getAbsolutePath() + File.separator + "Metadaten");
            two.createNewFile();
            File three = new File(root.getAbsolutePath() + File.separator + "Huhu");
            three.createNewFile();
        }
        catch (IOException ioException){
            ioException.printStackTrace();
        }
    }
    private static final File rootFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "Table Viewer");
    //private static final File rootFile = new File(System.getProperty("c:\tmp") );

    private static class Tree<T> extends TreeGrid<T> {

        Tree(ValueProvider<T, ?> valueProvider) {
            Column<T> only = addHierarchyColumn(valueProvider);
            only.setAutoWidth(true);
           // only.setHeader("Auswahl");
            only.setWidth("500px");

        }
    }

    public FileTree() {
        Tree<File> filesTree = new Tree<>(File::getName);
        filesTree.setItems(Collections.singleton(rootFile), this::getFiles);
        filesTree
                .getColumns()
                .stream()
                .findFirst()
                .ifPresent(
                        fileColumn -> {
                            fileColumn.setComparator(Comparator.naturalOrder());
                            GridSortOrder<File> sortOrder = new GridSortOrder<>(fileColumn, SortDirection.ASCENDING);
                            filesTree.sort(Collections.singletonList(sortOrder));
                         //  filesTree.setWidthFull();
                         //   filesTree.setHeight("300px");
                            filesTree
                                    .asSingleSelect()
                                    .addValueChangeListener(
                                            event -> {
                                                File file = event.getValue();
                                                if (file != null && file.isFile()) { // deselecting: file == null
                                                    System.out.println((event.getValue().getName()));
                                                } else {
                                                    // don't do anything
                                                }
                                            }
                                    );
                        }
                );
        filesTree.getStyle().set("--_lumo-grid-border-width","0px");

        this.add(filesTree);
      //  setSizeFull();
    }

    private List<File> getFiles(File parent) {
        if (parent.isDirectory()) {
            File[] list = parent.listFiles();
            if (list != null) {
                return Arrays.asList(list);
            }
        }

        return Collections.emptyList();
    }
}



