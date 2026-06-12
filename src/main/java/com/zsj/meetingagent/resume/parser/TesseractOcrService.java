package com.zsj.meetingagent.resume.parser;

import com.zsj.meetingagent.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Tesseract CLI 的 OCR 实现。
 * 扫描 PDF 会先渲染成图片，再调用 tesseract 识别；部署时需要本机安装 tesseract 和对应语言包。
 */
@Service
public class TesseractOcrService implements OcrService {

    private final OcrProperties properties;

    public TesseractOcrService(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public String recognizePdf(byte[] pdfBytes) {
        if (!properties.isEnabled()) {
            throw new BusinessException("R0406", "当前未开启 OCR，请上传带文字层的 PDF，或设置 RESUME_OCR_ENABLED=true 并安装 Tesseract");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = Math.min(document.getNumberOfPages(), Math.max(1, properties.getMaxPages()));
            List<String> pageTexts = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, properties.getDpi(), ImageType.RGB);
                pageTexts.add(recognizeImage(image, pageIndex + 1));
            }
            String text = normalize(String.join("\n\n", pageTexts));
            if (!StringUtils.hasText(text)) {
                throw new BusinessException("R0407", "OCR 没有识别到有效文字，请检查 PDF 清晰度或语言包配置");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("R0405", "扫描版 PDF OCR 解析失败，请确认文件未损坏且未加密");
        }
    }

    private String recognizeImage(BufferedImage image, int pageNumber) throws IOException {
        Path imagePath = Files.createTempFile("meeting-agent-ocr-page-" + pageNumber + "-", ".png");
        try {
            ImageIO.write(image, "png", imagePath.toFile());
            ProcessBuilder processBuilder = new ProcessBuilder(
                    properties.getCommand(),
                    imagePath.toAbsolutePath().toString(),
                    "stdout",
                    "-l",
                    properties.getLanguage(),
                    "--psm",
                    "6"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            Duration timeout = properties.getTimeout();
            boolean finished = process.waitFor(Math.max(1, timeout.toSeconds()), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("R0408", "OCR 识别超时，请降低页数或检查 Tesseract 安装");
            }
            if (process.exitValue() != 0) {
                throw new BusinessException("R0409", "OCR 调用失败：" + output.trim());
            }
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("R0408", "OCR 识别被中断，请稍后重试");
        } finally {
            Files.deleteIfExists(imagePath);
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
