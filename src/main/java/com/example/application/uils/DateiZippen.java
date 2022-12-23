package com.example.application.uils;

import org.apache.commons.io.FileUtils;
import org.springframework.lang.NonNull;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DateiZippen {


    public static void zipFile(String inFileName, String outFileName){
        ZipOutputStream zos = null;
        FileInputStream fis = null;
        try {
            zos = new ZipOutputStream(
                    new FileOutputStream(outFileName));
            fis = new FileInputStream(inFileName);
            zos.putNextEntry(new ZipEntry(new File(inFileName).getName()));
            int len;
            byte[] buffer = new byte[2048];
            while ((len = fis.read(buffer, 0, buffer.length)) > 0) {
                zos.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e) {}
            }
            if(zos != null){
                try {
                    zos.closeEntry();
                    zos.close();
                } catch (IOException e) {}
            }
        }
    }

    public void createZipOfFolder(@NonNull String sourceFolderName) {
        Path sourceFolderPath = Paths.get(sourceFolderName);
        Path zipFilePath = Paths.get(sourceFolderPath + ".zip");
        // This baseFolderPath is upto the parent folder of sourceFolder.
        // Used to remove nesting of parent folder inside zip
        Path baseFolderPath = Paths.get(sourceFolderName.substring(0,
                sourceFolderName.indexOf(sourceFolderPath.getFileName().toString())));

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    System.out.println(">createZipOfFolder< Adding Dir: " + dir);
                    // Ending slash is required to persist the folder as folder else it persists as file
                    zos.putNextEntry(new ZipEntry(baseFolderPath.relativize(dir) + "/"));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    System.out.println(">createZipOfFolder< Adding file: " + file);
                    zos.putNextEntry(new ZipEntry(baseFolderPath.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });

            //Original löschen:
            FileUtils.deleteDirectory(new File(sourceFolderPath.toUri()));

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

}
