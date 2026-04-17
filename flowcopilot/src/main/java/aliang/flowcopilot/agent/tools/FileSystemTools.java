package aliang.flowcopilot.agent.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// @Component 纭繚鏂囦欢绯荤粺宸ュ叿榛樿涓嶈娉ㄥ叆
@Slf4j
public class FileSystemTools implements Tool {

    private static final Path BASE_DIRECTORY = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

    @Override
    public String getName() {
        return "fileSystemTool";
    }

    @Override
    public String getDescription() {
        return "提供基础文件系统操作。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @dev.langchain4j.agent.tool.Tool(name = "readFile", value = "读取指定文件的内容，参数为 filePath。")
    public String readFile(String filePath) {
        try {
            Path path = validateAndResolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + filePath;
            }
            if (!Files.isRegularFile(path)) {
                return "错误：路径不是文件 - " + filePath;
            }
            return "文件内容:\n" + Files.readString(path);
        } catch (Exception e) {
            log.error("读取文件失败: {}", filePath, e);
            return "错误：读取文件失败 - " + e.getMessage();
        }
    }

    @dev.langchain4j.agent.tool.Tool(name = "writeFile", value = "写入文件内容，参数为 filePath 和 content。")
    public String writeFile(String filePath, String content) {
        try {
            Path path = validateAndResolvePath(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return "成功写入文件: " + filePath;
        } catch (Exception e) {
            log.error("写入文件失败: {}", filePath, e);
            return "错误：写入文件失败 - " + e.getMessage();
        }
    }

    @dev.langchain4j.agent.tool.Tool(name = "appendToFile", value = "向文件末尾追加内容，参数为 filePath 和 content。")
    public String appendToFile(String filePath, String content) {
        try {
            Path path = validateAndResolvePath(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            return "成功追加内容到文件: " + filePath;
        } catch (Exception e) {
            log.error("追加文件失败: {}", filePath, e);
            return "错误：追加文件失败 - " + e.getMessage();
        }
    }

    @dev.langchain4j.agent.tool.Tool(name = "listFiles", value = "列出目录内容，参数为 directoryPath。")
    public String listFiles(String directoryPath) {
        try {
            Path path = validateAndResolvePath(directoryPath);
            if (!Files.exists(path)) {
                return "错误：目录不存在 - " + directoryPath;
            }
            if (!Files.isDirectory(path)) {
                return "错误：路径不是目录 - " + directoryPath;
            }
            try (Stream<Path> stream = Files.list(path)) {
                String content = stream
                        .sorted(Comparator.comparing(Path::toString))
                        .map(item -> (Files.isDirectory(item) ? "[DIR] " : "[FILE] ") + BASE_DIRECTORY.relativize(item.toAbsolutePath().normalize()))
                        .collect(Collectors.joining("\n"));
                return content.isEmpty() ? "(空目录)" : content;
            }
        } catch (Exception e) {
            log.error("列目录失败: {}", directoryPath, e);
            return "错误：列目录失败 - " + e.getMessage();
        }
    }

    @dev.langchain4j.agent.tool.Tool(name = "deleteFile", value = "删除指定文件，参数为 path。")
    public String deleteFile(String path) {
        try {
            Path resolvedPath = validateAndResolvePath(path);
            if (!Files.exists(resolvedPath)) {
                return "错误：文件不存在 - " + path;
            }
            if (!Files.isRegularFile(resolvedPath)) {
                return "错误：路径不是文件 - " + path;
            }
            Files.delete(resolvedPath);
            return "成功删除文件: " + path;
        } catch (Exception e) {
            log.error("删除文件失败: {}", path, e);
            return "错误：删除文件失败 - " + e.getMessage();
        }
    }

    @dev.langchain4j.agent.tool.Tool(name = "createDirectory", value = "创建目录，参数为 directoryPath。")
    public String createDirectory(String directoryPath) {
        try {
            Path path = validateAndResolvePath(directoryPath);
            Files.createDirectories(path);
            return "成功创建目录: " + directoryPath;
        } catch (Exception e) {
            log.error("创建目录失败: {}", directoryPath, e);
            return "错误：创建目录失败 - " + e.getMessage();
        }
    }

    private Path validateAndResolvePath(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        Path resolved = BASE_DIRECTORY.resolve(path).normalize();
        if (!resolved.startsWith(BASE_DIRECTORY)) {
            throw new SecurityException("路径超出允许访问的工作目录");
        }
        return resolved;
    }
}
