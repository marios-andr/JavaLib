package io.github.congueror;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHelper {

    public static FileWriter writeTempFile(Path path) throws IOException {
        File tempFile = Path.of(path.toString(), "temp.txt").toFile();
        tempFile.deleteOnExit();

        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Something went wrong while creating temp file.");
            e.printStackTrace();
        }

        return new FileWriter(tempFile, false);
    }

    public static Path extractFile(ZipFile zip, String fileName, Path outputFolder) throws IOException {
        File tempFile = new File(outputFolder + "/" + fileName);
        ZipEntry entry = zip.getEntry(fileName);

        if (entry == null) {
            throw new FileNotFoundException("cannot find file: " + fileName + " in archive: " + zip.getName());
        }

        InputStream zipStream = zip.getInputStream(entry);
        OutputStream fileStream = null;

        try {
            final byte[] buf;

            fileStream = new FileOutputStream(tempFile);
            buf = new byte[1024];

            int i;
            while ((i = zipStream.read(buf)) != -1) {
                fileStream.write(buf, 0, i);
            }
        } finally {
            if (zipStream != null)
                zipStream.close();
            if (fileStream != null)
                fileStream.close();
        }

        return Path.of(tempFile.getPath());
    }

    public static Path extractResourceFile(String fileName, Path outputFolder) throws IOException {
        InputStream file = FileHelper.class.getClassLoader().getResourceAsStream(fileName);
        if (file == null)
            return null;
        Files.createDirectories(outputFolder);
        Files.copy(file, outputFolder.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return outputFolder.resolve(fileName);
    }
}
