package com.edusistem.core.exam.infrastructure.adapter.pdf;

import com.edusistem.core.exam.domain.outputports.ScannedPdfPort;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Rasteriza con PDFBox a 200 DPI (suficiente para QR y burbujas en tamaño carta/A4) y codifica cada página en JPEG. */
@Component
public class PdfBoxScannedPdfAdapter implements ScannedPdfPort {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxScannedPdfAdapter.class);
    private static final float DPI = 200;

    @Override
    public int pageCount(byte[] pdf, int maxPages) {
        try (PDDocument document = load(pdf)) {
            int pages = document.getNumberOfPages();
            if (pages == 0) {
                throw new InvalidRequestException("INVALID_PDF", "The PDF has no pages");
            }
            if (pages > maxPages) {
                throw new InvalidRequestException("TOO_MANY_PAGES",
                        "The PDF has " + pages + " pages; the maximum per upload is " + maxPages);
            }
            return pages;
        } catch (IOException e) {
            throw invalid();
        }
    }

    @Override
    public void forEachPage(byte[] pdf, int firstPage, PageHandler handler) {
        try (PDDocument document = load(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = Math.max(firstPage, 1) - 1; i < document.getNumberOfPages(); i++) {
                handler.handle(i + 1, renderPage(renderer, i));
            }
        } catch (IOException e) {
            throw invalid();
        }
    }

    private static PDDocument load(byte[] pdf) {
        if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
            throw new InvalidRequestException("INVALID_PDF", "Only non-empty PDF files are supported");
        }
        try {
            return Loader.loadPDF(pdf);
        } catch (IOException e) { // incluye PDFs cifrados con contraseña
            throw invalid();
        }
    }

    private static byte[] renderPage(PDFRenderer renderer, int index) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage image = renderer.renderImageWithDPI(index, DPI, ImageType.RGB);
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.warn("Could not rasterize page {} of a scanned PDF", index + 1, e);
            return null;
        }
    }

    private static InvalidRequestException invalid() {
        return new InvalidRequestException("INVALID_PDF", "The file could not be read as a PDF");
    }
}
