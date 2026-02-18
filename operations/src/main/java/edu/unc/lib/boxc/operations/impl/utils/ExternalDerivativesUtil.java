package edu.unc.lib.boxc.operations.impl.utils;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ExternalDerivativesUtil {

    public static Path getDerivativePath(String basePath, String binaryId) {
        return Paths.get(basePath, getBinaryPath(binaryId), binaryId + ".txt");
    }

    public static void writeToFile(Path derivativePath, String text) throws IOException {
        var derivative = derivativePath.toFile();

        Files.createDirectories(derivative.getParentFile().toPath());
        FileUtils.write(derivative, text, UTF_8);
    }

    private static String getBinaryPath(String binaryId) {
        return idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
    }
}
