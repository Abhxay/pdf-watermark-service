package com.geminid.watermark.scalability;

import com.geminid.watermark.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class ConcurrentWatermarkTest {

    private final PdfService pdfService = new PdfService();

    private byte[] buildPdf(int pages) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("20 concurrent threads: all succeed, zero failures")
    void concurrent_20Threads_allSucceed() throws Exception {
        int threadCount = 20;
        byte[] pdf = buildPdf(3);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String name = "User-" + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    byte[] result = pdfService.addWatermark(pdf, name);
                    if (result != null && result.length > 0) successes.incrementAndGet();
                    else failures.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
                return null;
            }));
        }

        ready.await();
        start.countDown();
        for (Future<Void> f : futures) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(threadCount);
        assertThat(failures.get()).isZero();
    }

    @Test
    @DisplayName("100 sequential watermarks complete under 10 seconds")
    void sequential_100Requests_completesInTime() throws Exception {
        byte[] pdf = buildPdf(1);
        long start = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            assertThat(pdfService.addWatermark(pdf, "User" + i)).isNotEmpty();
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("100 sequential: %d ms (%.1f req/sec)%n", elapsed, 100_000.0 / elapsed);
        assertThat(elapsed).isLessThan(10_000);
    }

    @Test
    @DisplayName("50-page PDF repeated 10 times: no OutOfMemoryError")
    void largePdf_50Pages_noOOM() throws Exception {
        byte[] bigPdf = buildPdf(50);
        for (int i = 0; i < 10; i++) {
            assertThat(pdfService.addWatermark(bigPdf, "Stress-" + i)).isNotEmpty();
        }
    }

    @Test
    @DisplayName("10 concurrent results are all independent valid PDFs")
    void concurrent_resultsAreIndependent() throws Exception {
        byte[] pdf = buildPdf(1);
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<byte[]>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String name = "Name" + i;
            futures.add(pool.submit(() -> pdfService.addWatermark(pdf, name)));
        }

        List<byte[]> results = new ArrayList<>();
        for (Future<byte[]> f : futures) results.add(f.get(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(results).hasSize(threadCount);
        for (byte[] result : results) {
            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) 0x25);
        }
    }

    @Test
    @DisplayName("Single 10-page PDF watermark completes under 500ms")
    void singleRequest_10Pages_under500ms() throws Exception {
        byte[] pdf = buildPdf(10);
        long start = System.currentTimeMillis();
        pdfService.addWatermark(pdf, "Performance Test");
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("10-page watermark: %d ms%n", elapsed);
        assertThat(elapsed).isLessThan(500);
    }
}