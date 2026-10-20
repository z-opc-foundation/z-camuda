package com.zifang.z.camuda.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * README 里每一条定量主张都必须能从源码重算出来 —— 广告出去的话就是承诺。
 *
 * <p><b>为什么要有这一层</b>：本仓 README 把「4 个 Maven 模块」「当前版本 1.0.6」「父项目
 * z-boot-parent:1.1.0」「3 个生命周期钩子接口」「22 个 SPI 扩展接口」「7 个 Controller」
 * 「22 个接口里只有 4 个真正接入了派发点」「默认端口 8080 / z-rpc 端口 20880」写成了硬数字，
 * 而每加一个模块、每加一个 Controller、每抬一次版本、每接一个派发点都会漂一次。
 * 没有任何尺子去问代码「现在还是吗」，那这些数字的保质期就是上一次编辑的时刻。</p>
 *
 * <p><b>口径三条</b>：</p>
 * <ol>
 *   <li><b>事实对象只装源码侧的事实</b>（{@link Facts}）——README 侧的数字一律在核对当下
 *       现解析。要是两边都从同一份 README 取数，尺子就成了恒等式比较，README 改了也不红；</li>
 *   <li><b>主张抠不到就红</b>：不许「正则没匹配上就跳过」，那正是空跑；</li>
 *   <li><b>按结构树/章节名定位，不按数字定位</b>：数字是结论不是锚点 ——
 *       短数字是长数字的子串（「4 个」⊂「24 个」），按数字定位会匹配到别人的行上。</li>
 * </ol>
 *
 * <p>数值不等 = 红，且把<b>重算值</b>打进消息（红了要知道真数是多少，否则只是又一处猜）。
 * 核对结果<b>一次全报</b>（收集齐所有不一致再抛），撞停会把尺子退化成「一次只说一句」；
 * 尺子的牙口由 {@link #rulerActuallyCondemnsWrongNumbers()} 用合成 README 证明，
 * 而「不许冤枉健康页」由 {@link #readmeClaimsMatchTheCode()} 本身证明。</p>
 */
class ReadmeClaimsTest {

    /** 源码侧路径一律相对仓根写死，不按目录名统计（多模块仓里同名目录不止一处）。 */
    private static final String HOOK_DIR = "z-camuda-core/src/main/java/com/zifang/z/camuda/core/hook";
    private static final String SPI_DIR = "z-camuda-core/src/main/java/com/zifang/z/camuda/core/spi";
    /**
     * web 模块的整个 main 源码树（不只 {@code .../web/api} 一个包）——
     * README 说的「z-camuda-web 下 N 个 Controller」指的是整个模块，
     * 只扫 api 包的话，别处新加一个 Controller 就会从尺子眼皮底下溜过去。
     */
    private static final String WEB_MAIN_DIR = "z-camuda-web/src/main";
    private static final String ADMIN_PROPS = "z-camuda-admin/src/main/resources/application.properties";

    /** 仓根：向上找同时有 README.md 与 pom.xml 的目录（测试跑在模块目录，user.dir 不是仓根）。 */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 5 && p != null; i++, p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("README.md")) && Files.isRegularFile(p.resolve("pom.xml"))) {
                return p;
            }
        }
        throw new IllegalStateException("定位仓根失败：user.dir=" + System.getProperty("user.dir"));
    }

    private static String read(Path f) {
        try {
            return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 剥掉注释（块注释 + 行注释），字符串字面量原样保留。
     *
     * <p>判「类级注解」还是「方法级注解」时必须先剥注释：Javadoc 里出现过
     * {@code @RequestMapping} 字样就会被当成真映射。</p>
     */
    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < n) {
                    char d = s.charAt(i);
                    sb.append(d);
                    i++;
                    if (d == '\\' && i < n) {
                        sb.append(s.charAt(i));
                        i++;
                        continue;
                    }
                    if (d == quote) {
                        break;
                    }
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 取某个二级章节的正文（从标题行之后到下一个「## 」标题之前）。 */
    private static String section(String readme, String keyword) {
        Matcher head = Pattern.compile("(?m)^##[^\\n]*" + Pattern.quote(keyword) + "[^\\n]*$").matcher(readme);
        if (!head.find()) {
            throw new AssertionError("README 里找不到章节：" + keyword + "（章节被改名了？）");
        }
        int from = head.end();
        Matcher next = Pattern.compile("(?m)^##[^#\\n]").matcher(readme);
        int to = next.find(from) ? next.start() : readme.length();
        return readme.substring(from, to);
    }

    /** 项目结构树里某个节点的整行（锚点是树形前缀 + 节点名，不是数字）。 */
    private static String treeLine(String readme, String node) {
        Matcher m = Pattern.compile("(?m)^[ \\t]*(?:├──|└──)[ \\t]*" + Pattern.quote(node)
                        + "(?:/|(?=[ \\t#]))[^\\n]*$")
                .matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 的项目结构树里找不到节点行：" + node
                    + "（结构树被改过形？还是这行被挪走了？）");
        }
        return m.group();
    }

    /** 项目结构树里的模块目录行，按文档顺序取名（如 z-camuda-core、z-camuda-web …）。 */
    private static List<String> treeModuleDirs(String structure) {
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^[ \\t]*(?:├──|└──)[ \\t]*(z-camuda-[A-Za-z0-9._-]+)/").matcher(structure);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    // ==================================================================
    // 源码侧事实（Facts 里只装这些；README 侧的数字一律现解析）
    // ==================================================================

    /** 解析根 pom 的项目自身信息（XML 解析，不走正则）。 */
    private static Element projectElement(Path pom) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            Document doc = f.newDocumentBuilder().parse(pom.toFile());
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("解析 pom 失败：" + pom, e);
        }
    }

    private static Element childElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * 根 pom 的 {@code <modules>} 声明顺序。
     *
     * <p>用 XML 解析而不是正则：正则躲不掉被 {@code <!-- -->} 注释掉的 {@code <module>}，
     * 而且拿不到声明顺序（README 里的模块清单是有序的，顺序也是主张的一部分）。</p>
     */
    private static List<String> pomModules(Path root) {
        Element modules = childElement(projectElement(root.resolve("pom.xml")), "modules");
        if (modules == null) {
            throw new IllegalStateException("根 pom 里没有 <modules>");
        }
        List<String> list = new ArrayList<>();
        NodeList nl = modules.getElementsByTagName("module");
        for (int i = 0; i < nl.getLength(); i++) {
            list.add(nl.item(i).getTextContent().trim());
        }
        return list;
    }

    /**
     * 根 pom 的版本面。
     *
     * <p>CI-friendly versions：根 pom 的 {@code <version>${revision}</version>}，
     * 真版本面在 {@code <revision>} 属性里。必须读属性而不是抓第一个
     * {@code <version>} —— 那样会抓到 {@code <parent>} 里的那个。</p>
     */
    private static String pomRevision(Path root) {
        Element props = childElement(projectElement(root.resolve("pom.xml")), "properties");
        if (props == null) {
            throw new IllegalStateException("根 pom 里没有 <properties>");
        }
        Element rev = childElement(props, "revision");
        if (rev == null) {
            throw new IllegalStateException("根 pom 的 <properties> 里没有 <revision>");
        }
        return rev.getTextContent().trim();
    }

    /** 根 pom 的 {@code <parent>} 版本。 */
    private static String pomParentVersion(Path root) {
        Element parent = childElement(projectElement(root.resolve("pom.xml")), "parent");
        if (parent == null) {
            throw new IllegalStateException("根 pom 里没有 <parent>");
        }
        Element v = childElement(parent, "version");
        if (v == null) {
            throw new IllegalStateException("根 pom 的 <parent> 里没有 <version>");
        }
        return v.getTextContent().trim();
    }

    /** 某个包目录下声明为 {@code public interface} 的类型数（只看该目录的直接子文件）。 */
    private static int interfaceCount(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("目录不存在：" + dir);
        }
        try (Stream<Path> s = Files.list(dir)) {
            return (int) s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> Pattern.compile("(?m)^\\s*public\\s+interface\\s+\\w+")
                            .matcher(stripComments(read(p))).find())
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 某个目录下 {@code *Controller.java} 的类名（去掉 Controller 后缀）。 */
    private static Set<String> controllerNames(Path webMain) {
        if (!Files.isDirectory(webMain)) {
            throw new IllegalStateException("目录不存在：" + webMain);
        }
        try (Stream<Path> s = Files.walk(webMain)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .map(p -> {
                        String n = p.getFileName().toString();
                        n = n.substring(0, n.length() - ".java".length());
                        return n.substring(0, n.length() - "Controller".length());
                    })
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Controller 的类级 {@code @RequestMapping} 路径（只认类声明之前的那一个）。 */
    private static String classLevelMapping(Path file) {
        String src = stripComments(read(file));
        int cls = src.indexOf("public class ");
        if (cls < 0) {
            throw new IllegalStateException("源文件里找不到类声明：" + file);
        }
        Matcher m = Pattern.compile("@RequestMapping\\s*\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(src.substring(0, cls));
        String path = null;
        while (m.find()) {
            path = m.group(1);
        }
        if (path == null) {
            throw new IllegalStateException("Controller 没有类级 @RequestMapping：" + file);
        }
        return path;
    }

    /** Controller 名 → 类级映射路径。 */
    private static Map<String, String> controllerMappings(Path webMain) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.isDirectory(webMain)) {
            throw new IllegalStateException("目录不存在：" + webMain);
        }
        try (Stream<Path> s = Files.walk(webMain)) {
            List<Path> files = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path p : files) {
                String n = p.getFileName().toString();
                n = n.substring(0, n.length() - ".java".length());
                map.put(n, classLevelMapping(p));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }

    /**
     * main 源码树里全部 {@code getByCode("X")} 的 code —— 真实的派发点。
     *
     * <p>从代码提取而不是在判据里硬编码 4 个：硬编码的话，将来有人接了第 5 个派发点，
     * 尺子会拿旧数字去比，判不出「README 说只有 4 个」已经过期。</p>
     */
    private static Set<String> dispatchedCodes(Path root, List<String> modules) {
        Set<String> codes = new LinkedHashSet<>();
        Pattern call = Pattern.compile("getByCode\\s*\\(\\s*\"([A-Za-z0-9_]+)\"");
        for (String module : modules) {
            Path main = root.resolve(module).resolve("src/main");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(main)) {
                List<Path> files = s.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .collect(Collectors.toList());
                for (Path p : files) {
                    Matcher m = call.matcher(stripComments(read(p)));
                    while (m.find()) {
                        codes.add(m.group(1));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return codes;
    }

    /** application.properties 里 {@code key=${ENV:默认值}} 的默认值。 */
    private static int propertyDefault(Path props, String key) {
        Matcher m = Pattern.compile("(?m)^[ \\t]*" + Pattern.quote(key) + "[ \\t]*=[ \\t]*\\$\\{[^:}]+:(\\d+)\\}")
                .matcher(read(props));
        if (!m.find()) {
            throw new IllegalStateException(props + " 里找不到 " + key + " 的默认值");
        }
        return Integer.parseInt(m.group(1));
    }

    /** 一次重算出的全部源码侧事实。 */
    private static final class Facts {
        final List<String> modules;
        final String revision;
        final String parentVersion;
        final int hookInterfaces;
        final int spiInterfaces;
        final Set<String> controllers;
        final Map<String, String> controllerMappings;
        final Set<String> dispatchedCodes;
        final int httpPort;
        final int rpcPort;

        Facts(List<String> modules, String revision, String parentVersion, int hookInterfaces,
              int spiInterfaces, Set<String> controllers, Map<String, String> controllerMappings,
              Set<String> dispatchedCodes, int httpPort, int rpcPort) {
            this.modules = modules;
            this.revision = revision;
            this.parentVersion = parentVersion;
            this.hookInterfaces = hookInterfaces;
            this.spiInterfaces = spiInterfaces;
            this.controllers = controllers;
            this.controllerMappings = controllerMappings;
            this.dispatchedCodes = dispatchedCodes;
            this.httpPort = httpPort;
            this.rpcPort = rpcPort;
        }
    }

    private static Facts recomputed() {
        Path root = repoRoot();
        List<String> modules = pomModules(root);
        Path webMain = root.resolve(WEB_MAIN_DIR);
        Path props = root.resolve(ADMIN_PROPS);
        return new Facts(
                modules,
                pomRevision(root),
                pomParentVersion(root),
                interfaceCount(root.resolve(HOOK_DIR)),
                interfaceCount(root.resolve(SPI_DIR)),
                controllerNames(webMain),
                controllerMappings(webMain),
                dispatchedCodes(root, modules),
                propertyDefault(props, "server.port"),
                propertyDefault(props, "z.rpc.server.port"));
    }

    // ==================================================================
    // 核对
    // ==================================================================

    /**
     * README 里逐条抠出数字并与重算值对账；任何一条对不上就抛 AssertionError，
     * 并把<b>所有</b>不一致连同真数一起打进去。
     */
    private static void verifyClaims(String readme, Facts f) {
        List<String> bad = new ArrayList<>();
        Matcher m;

        String basic = section(readme, "基本信息");
        String structure = section(readme, "项目结构");
        String ext = section(readme, "扩展模型");
        String api = section(readme, "API 一览");

        // ---- 主张 1：Maven 模块数（根 pom <modules> 条数） ----
        m = Pattern.compile("(\\d+) 个 Maven 模块").matcher(readme);
        if (!m.find()) {
            bad.add("README 里找不到「N 个 Maven 模块」这条模块数主张");
        } else if (Integer.parseInt(m.group(1)) != f.modules.size()) {
            bad.add("Maven 模块数对不上：README 写 " + m.group(1) + " 个，根 pom <modules> 实数是 "
                    + f.modules.size() + "（" + f.modules + "）");
        }

        // ---- 主张 2：模块名单与声明顺序（结构树 ↔ 根 pom <modules>） ----
        List<String> readmeModules = treeModuleDirs(structure);
        if (readmeModules.isEmpty()) {
            bad.add("README 的项目结构树里抠不出模块目录行");
        } else if (!readmeModules.equals(f.modules)) {
            bad.add("Maven 模块名单对不上：README 项目结构树是 " + readmeModules
                    + "，根 pom <modules> 是 " + f.modules + "（顺序也算主张）");
        }

        // ---- 主张 3：当前版本（根 pom <revision>） ----
        m = Pattern.compile("\\*\\*当前版本\\*\\*[ \\t]*\\|[ \\t]*`([^`]+)`").matcher(basic);
        if (!m.find()) {
            bad.add("README 的基本信息表里找不到「**当前版本** | `X.Y.Z`」这条版本主张");
        } else if (!f.revision.equals(m.group(1))) {
            bad.add("版本号对不上：README 写 " + m.group(1) + "，根 pom <revision> 是 " + f.revision);
        }

        // ---- 主张 4：父项目版本（根 pom <parent><version>） ----
        m = Pattern.compile("z-boot-parent:(\\d+\\.\\d+\\.\\d+)").matcher(basic);
        if (!m.find()) {
            bad.add("README 的基本信息表里找不到「z-boot-parent:X.Y.Z」这条父项目版本主张");
        } else if (!f.parentVersion.equals(m.group(1))) {
            bad.add("父项目版本对不上：README 写 z-boot-parent:" + m.group(1)
                    + "，根 pom <parent> 是 " + f.parentVersion);
        }

        // ---- 主张 5：生命周期钩子接口数（core/hook 下的接口） ----
        m = Pattern.compile("钩子（(\\d+) 个接口").matcher(ext);
        if (!m.find()) {
            bad.add("README 的扩展模型一节里找不到「钩子（N 个接口」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.hookInterfaces) {
            bad.add("钩子接口数对不上：README 写 " + m.group(1) + " 个，"
                    + HOOK_DIR + " 下实数是 " + f.hookInterfaces);
        }

        // ---- 主张 6：SPI 扩展接口数（core/spi 下的接口） ----
        m = Pattern.compile("SPI 扩展点（(\\d+) 个接口").matcher(ext);
        if (!m.find()) {
            bad.add("README 的扩展模型一节里找不到「SPI 扩展点（N 个接口」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.spiInterfaces) {
            bad.add("SPI 接口数对不上：README 写 " + m.group(1) + " 个，"
                    + SPI_DIR + " 下实数是 " + f.spiInterfaces);
        }

        // ---- 主张 7：Controller 数与名单（结构树 z-camuda-web 行） ----
        String webLine = treeLine(structure, "z-camuda-web");
        m = Pattern.compile("(\\d+) 个 Controller（([^）]+)）").matcher(webLine);
        if (!m.find()) {
            bad.add("README 结构树的 z-camuda-web 那一行抠不出「N 个 Controller（名单）」： " + webLine);
        } else {
            if (Integer.parseInt(m.group(1)) != f.controllers.size()) {
                bad.add("Controller 数对不上：README 结构树写 " + m.group(1) + " 个，"
                        + WEB_MAIN_DIR + " 下实数是 " + f.controllers.size());
            }
            Set<String> readmeNames = new TreeSet<>();
            for (String part : m.group(2).split("/")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    readmeNames.add(t);
                }
            }
            if (!readmeNames.equals(f.controllers)) {
                bad.add("Controller 名单对不上：README 结构树写 " + readmeNames
                        + "，源码里实数是 " + f.controllers);
            }
        }

        // ---- 主张 8：API 一览表 ↔ 每个 Controller 的类级映射（双向对账） ----
        Map<String, String> readmeRows = new LinkedHashMap<>();
        Matcher row = Pattern.compile("(?m)^\\|[ \\t]*`(/[^`]+)`[ \\t]*\\|[ \\t]*`([A-Za-z0-9]+Controller)`[ \\t]*\\|")
                .matcher(api);
        while (row.find()) {
            readmeRows.put(row.group(1), row.group(2));
        }
        if (readmeRows.isEmpty()) {
            bad.add("README 的 API 一览一节里抠不出任何「路径前缀 | Controller」行");
        } else {
            Map<String, String> srcByPath = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : f.controllerMappings.entrySet()) {
                srcByPath.put(e.getValue(), e.getKey());
            }
            for (Map.Entry<String, String> e : srcByPath.entrySet()) {
                String path = e.getKey();
                String controller = e.getValue();
                if (!readmeRows.containsKey(path)) {
                    bad.add("API 一览表对不上：源码里 " + controller + " 的类级映射 " + path
                            + " 在 README 表里没有");
                } else if (!controller.equals(readmeRows.get(path))) {
                    bad.add("API 一览表对不上：路径 " + path + " README 写的是 "
                            + readmeRows.get(path) + "，源码里是 " + controller);
                }
            }
            for (Map.Entry<String, String> e : readmeRows.entrySet()) {
                if (!srcByPath.containsKey(e.getKey())) {
                    bad.add("API 一览表对不上：README 列了路径 " + e.getKey() + "（"
                            + e.getValue() + "），源码里没有这个类级映射");
                }
            }
        }

        // ---- 主张 9：派发点 —— 已接入 / 未接入的个数与已接入的 code 名单 ----
        m = Pattern.compile("只有 (\\d+) 个真正接入了派发点").matcher(ext);
        if (!m.find()) {
            bad.add("README 的扩展模型一节里找不到「只有 N 个真正接入了派发点」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.dispatchedCodes.size()) {
            bad.add("派发点已接入数对不上：README 写 " + m.group(1) + " 个，main 源码里实数是 "
                    + f.dispatchedCodes.size() + "（" + f.dispatchedCodes + "）");
        }
        m = Pattern.compile("其余 (\\d+) 个是\\*\\*预留扩展点\\*\\*").matcher(ext);
        if (!m.find()) {
            bad.add("README 的扩展模型一节里找不到「其余 N 个是预留扩展点」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.spiInterfaces - f.dispatchedCodes.size()) {
            bad.add("派发点未接入数对不上：README 写 " + m.group(1) + " 个，SPI 接口总数 "
                    + f.spiInterfaces + " 减去已接入 " + f.dispatchedCodes.size() + " 实数是 "
                    + (f.spiInterfaces - f.dispatchedCodes.size()));
        }
        m = Pattern.compile("\\*\\*已接入（(\\d+)）\\*\\*").matcher(ext);
        if (!m.find()) {
            bad.add("README 的派发点表里找不到「已接入（N）」这一行");
        } else if (Integer.parseInt(m.group(1)) != f.dispatchedCodes.size()) {
            bad.add("派发点已接入数对不上：README 表头写「已接入（" + m.group(1) + "）」，"
                    + "main 源码里实数是 " + f.dispatchedCodes.size());
        }
        m = Pattern.compile("\\*\\*未接入（(\\d+)）\\*\\*").matcher(ext);
        if (!m.find()) {
            bad.add("README 的派发点表里找不到「未接入（N）」这一行");
        } else if (Integer.parseInt(m.group(1)) != f.spiInterfaces - f.dispatchedCodes.size()) {
            bad.add("派发点未接入数对不上：README 表头写「未接入（" + m.group(1) + "）」，"
                    + "SPI 接口总数 " + f.spiInterfaces + " 减去已接入 " + f.dispatchedCodes.size()
                    + " 实数是 " + (f.spiInterfaces - f.dispatchedCodes.size()));
        }
        Set<String> readmeDispatched = readmeDispatchedCodes(ext);
        if (!readmeDispatched.equals(f.dispatchedCodes)) {
            bad.add("派发点 code 名单对不上：README 写 " + readmeDispatched
                    + "，main 源码里实数是 " + f.dispatchedCodes);
        }

        // ---- 主张 10：默认端口与 z-rpc 端口（application.properties 的默认值） ----
        m = Pattern.compile("\\*\\*默认端口\\*\\*[ \\t]*\\|[ \\t]*`(\\d+)`").matcher(basic);
        if (!m.find()) {
            bad.add("README 的基本信息表里找不到「**默认端口** | `N`」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.httpPort) {
            bad.add("默认端口对不上：README 写 " + m.group(1) + "，"
                    + ADMIN_PROPS + " 的 server.port 默认值是 " + f.httpPort);
        }
        m = Pattern.compile("z-rpc` 端口默认 `(\\d+)").matcher(basic);
        if (!m.find()) {
            bad.add("README 的基本信息表里找不到「z-rpc 端口默认 N」这条主张");
        } else if (Integer.parseInt(m.group(1)) != f.rpcPort) {
            bad.add("z-rpc 端口对不上：README 写 " + m.group(1) + "，"
                    + ADMIN_PROPS + " 的 z.rpc.server.port 默认值是 " + f.rpcPort);
        }

        if (!bad.isEmpty()) {
            throw new AssertionError("README 的定量主张与盘面对不上（" + bad.size() + " 条）：\n  - "
                    + String.join("\n  - ", bad));
        }
    }

    /**
     * 从 README 的派发点表里取出「已接入」那一段列出的 code 名。
     *
     * <p>只取「已接入（N）」行到「未接入（N）」行之间的第二列 —— 不能在整节里正则捞
     * {@code XxxService}，正文里的 {@code LeaveProcessService} 会被一起捞进来。</p>
     */
    private static Set<String> readmeDispatchedCodes(String ext) {
        String[] lines = ext.split("\n", -1);
        int from = -1;
        int to = -1;
        for (int i = 0; i < lines.length; i++) {
            if (from < 0 && lines[i].contains("**已接入（")) {
                from = i;
            }
            if (from >= 0 && to < 0 && lines[i].contains("**未接入（")) {
                to = i;
                break;
            }
        }
        if (from < 0 || to < 0 || to <= from) {
            throw new AssertionError("README 的派发点表里找不到「已接入（N）…未接入（N）」这两行");
        }
        Set<String> codes = new LinkedHashSet<>();
        for (int i = from; i < to; i++) {
            String[] cells = lines[i].split("\\|");
            if (cells.length < 3) {
                continue;
            }
            Matcher m = Pattern.compile("`([A-Za-z0-9]+Service)`").matcher(cells[2]);
            if (m.find()) {
                codes.add(m.group(1));
            }
        }
        if (codes.isEmpty()) {
            throw new AssertionError("README 派发点表的「已接入」那一段抠不出任何 code 名");
        }
        return codes;
    }

    /** 跑一遍核对，把它抛出的 AssertionError 消息取出来（没抛就返回空串）。 */
    private static String messageOf(String readme, Facts f) {
        try {
            verifyClaims(readme, f);
            return "";
        } catch (AssertionError e) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
    }

    /**
     * 把 README 里某个数字改掉（带命中次数断言：命中数不是 1 就停手，不盲改）。
     *
     * <p>正则里必须<b>只有一个捕获组</b>，且它就是那个数字 —— 直接用
     * {@code replaceFirst("$1…")} 会踩 Java 的坑：{@code $1} 后面紧跟数字会被解析成
     * {@code $19}（第 19 组），{@code ${1}} 那种写法 Java 又不支持。</p>
     */
    private static String tamper(String readme, String regex, String newValue) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(readme);
        int hits = 0;
        int from = -1;
        int to = -1;
        while (m.find()) {
            hits++;
            if (hits == 1) {
                from = m.start(1);
                to = m.end(1);
            }
        }
        if (hits != 1) {
            throw new IllegalStateException("篡改锚点命中 " + hits + " 次（应为 1 次）：" + regex);
        }
        String out = readme.substring(0, from) + newValue + readme.substring(to);
        if (out.equals(readme)) {
            throw new IllegalStateException("篡改没生效：" + regex);
        }
        return out;
    }

    @Test
    @DisplayName("README 的定量主张与源码重算值逐条对账")
    void readmeClaimsMatchTheCode() {
        Path root = repoRoot();
        Facts f = recomputed();
        verifyClaims(read(root.resolve("README.md")), f);
    }

    @Test
    @DisplayName("尺子的牙口：把 README 的数改错，判定必须判红")
    void rulerActuallyCondemnsWrongNumbers() {
        String readme = read(repoRoot().resolve("README.md"));
        Facts f = recomputed();

        // 每路变异：改错一个数 → 必须报出「这一条」主张的名字。
        // 改之前该主张的名字不应出现在报错里（证明它本来是对得上的）。
        List<String[]> mutations = new ArrayList<>(Arrays.asList(
                new String[]{"版本", "\\*\\*当前版本\\*\\*[ \\t]*\\|[ \\t]*`([0-9.]+)`", "9.9.9", "版本号对不上"},
                new String[]{"Controller 数", "(\\d+) 个 Controller（", "6", "Controller 数对不上"},
                new String[]{"模块数", "(\\d+) 个 Maven 模块", "5", "Maven 模块数对不上"},
                new String[]{"派发点数", "只有 (\\d+) 个真正接入了派发点", "5", "派发点已接入数对不上"},
                new String[]{"默认端口", "\\*\\*默认端口\\*\\*[ \\t]*\\|[ \\t]*`([0-9]+)`", "8081", "默认端口对不上"}));

        for (String[] mutation : mutations) {
            String label = mutation[0];
            String before = messageOf(readme, f);
            assertFalse(before.contains(mutation[3]),
                    "改之前「" + label + "」这条就不对上了（" + before + "）—— 换一条本来对得上的主张来做变异");

            String tampered = tamper(readme, mutation[1], mutation[2]);
            assertNotEquals(readme, tampered, "篡改失败：README 里找不到「" + label + "」的写法");

            AssertionError err = assertThrows(AssertionError.class,
                    () -> verifyClaims(tampered, f),
                    "改错「" + label + "」之后尺子居然没判红");
            assertTrue(err.getMessage() != null && err.getMessage().contains(mutation[3]),
                    "报错要指向真正对不上的那条主张（" + label + "），实际是：" + err.getMessage());
        }
    }

    @Test
    @DisplayName("不许冤枉健康页：真实 README 不得被判红")
    void healthyReadmeIsNotCondemned() {
        String readme = read(repoRoot().resolve("README.md"));
        verifyClaims(readme, recomputed());
    }
}
