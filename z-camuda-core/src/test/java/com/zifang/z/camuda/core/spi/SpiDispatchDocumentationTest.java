package com.zifang.z.camuda.core.spi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 钉住「SPI 接口的 Javadoc 必须与它<b>实际有没有派发点</b>一致」。
 *
 * <p><b>为什么需要这条</b>：22 个 SPI 接口各有一份 Javadoc，而实现与文档曾经全面脱节——
 * 每份都写着"引擎会在对应生命周期点调用所有实现"、"isSuccess() 为 false 时引擎中断流程"，
 * 实际只有 4 个 code 有派发点（其余 18 个注册后永不被调用），
 * 而已派发的那 4 个在返回失败时也只是记 warn、流程照常推进。
 * 业务方正是靠这段 Javadoc 决定要不要实现某个扩展点，写错等于发出去一个假的扩展点清单。</p>
 *
 * <p><b>判别式的构造</b>：派发集合<b>从代码里提取</b>（扫 main 源码树里全部
 * {@code getByCode("X")} 调用），而不是在判据里硬编码 4 个。
 * 硬编码的话，将来有人新增第 5 个派发点、忘了改那 18 份 Javadoc，判据照样全绿。
 * 从代码提取 ⇒ 派发集合一变，文档就必须跟着变。</p>
 */
class SpiDispatchDocumentationTest {

    private static final Path MAIN = Paths.get("src/main/java/com/zifang/z/camuda/core");
    private static final Path SPI_DIR = MAIN.resolve("spi");

    /** 判定"已接入派发"的 Javadoc 标识。 */
    private static final String MARK_LIVE = "流程照常推进";
    /** 判定"预留未接入"的 Javadoc 标识。 */
    private static final String MARK_IDLE = "当前版本没有派发点会调用本接口";
    /** 订正前的错误说法，任何一份 Javadoc 都不该再留。 */
    private static final String STALE_CLAIM = "{@code false} 时引擎中断流程";

    @Test
    @DisplayName("判据自身有效：扫得到 22 个接口，且派发集合恰为 4 个")
    void fixturesAreStillWhatWeThinkTheyAre() throws IOException {
        assertEquals(22, spiInterfaces().size(), "SPI 接口数量变了——判据需要跟着更新，不能继续用旧配平数");
        assertEquals(4, dispatchedCodes().size(),
                "当前版本的派发点数量变了（" + dispatchedCodes() + "）——"
                        + "若确实新增了派发点，请同步订正那 18 份“预留”Javadoc 与 README");
    }

    @Test
    @DisplayName("已接入派发的接口：Javadoc 必须写明失败时流程照常推进，不得再留“引擎中断流程”")
    void dispatchedInterfacesDocumentRealFailureSemantics() throws IOException {
        Set<String> dispatched = dispatchedCodes();
        List<String> problems = new ArrayList<>();

        for (Path f : spiInterfaces()) {
            String code = codeOf(f);
            if (!dispatched.contains(code)) {
                continue;
            }
            String doc = read(f);
            if (!doc.contains(MARK_LIVE)) {
                problems.add(code + "：有派发点，但 Javadoc 没有说明失败时“" + MARK_LIVE + "”");
            }
            if (doc.contains(MARK_IDLE)) {
                problems.add(code + "：已经有派发点了，Javadoc 却还挂着“" + MARK_IDLE + "”的预留说明");
            }
        }

        assertTrue(problems.isEmpty(), "以下已派发接口的 Javadoc 与实现不符：\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("未接入派发的接口：Javadoc 必须标明是预留扩展点（注册成功 ≠ 被调用）")
    void undispatchedInterfacesAreMarkedAsReserved() throws IOException {
        Set<String> dispatched = dispatchedCodes();
        List<String> problems = new ArrayList<>();

        for (Path f : spiInterfaces()) {
            String code = codeOf(f);
            if (dispatched.contains(code)) {
                continue;
            }
            String doc = read(f);
            if (!doc.contains(MARK_IDLE)) {
                problems.add(code + "：没有派发点，但 Javadoc 没有“" + MARK_IDLE + "”的预留标注"
                        + "（业务方会以为实现它就会被调用）");
            }
        }

        assertTrue(problems.isEmpty(), "以下接口注册后永不被调用，却没标成预留：\n  " + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("全 SPI 包不得再残留“引擎中断流程”的旧承诺")
    void noStaleInterruptPromiseAnywhere() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path f : spiInterfaces()) {
            if (read(f).contains(STALE_CLAIM)) {
                offenders.add(codeOf(f));
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些接口的 @return 仍写着“" + STALE_CLAIM + "”，而引擎从不因 SPI 失败中断流程："
                        + offenders);
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** main 源码树里全部 {@code getByCode("X")} 的 code —— 真实派发点。 */
    private static Set<String> dispatchedCodes() throws IOException {
        Set<String> codes = new LinkedHashSet<>();
        Pattern call = Pattern.compile("getByCode\\s*\\(\\s*\"([A-Za-z0-9_]+)\"");
        try (Stream<Path> tree = Files.walk(MAIN)) {
            List<Path> files = tree.filter(p -> p.toString().endsWith(".java"))
                    .collect(java.util.stream.Collectors.toList());
            for (Path p : files) {
                Matcher m = call.matcher(read(p));
                while (m.find()) {
                    codes.add(m.group(1));
                }
            }
        }
        return codes;
    }

    private static List<Path> spiInterfaces() throws IOException {
        try (Stream<Path> s = Files.list(SPI_DIR)) {
            List<Path> list = new ArrayList<>();
            s.filter(p -> p.getFileName().toString().startsWith("Camuda"))
                    .filter(p -> p.getFileName().toString().endsWith("Service.java"))
                    .forEach(list::add);
            return list;
        }
    }

    /** {@code CamudaXxxService.java} → code {@code XxxService}（注册表用的就是这个名字）。 */
    private static String codeOf(Path f) {
        String n = f.getFileName().toString();
        return n.substring("Camuda".length(), n.length() - ".java".length());
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}