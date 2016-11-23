package test.gol.model;

import org.junit.Test;
import test.gol.Backup;
import test.gol.utils.BackupUtils;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 8/7/16
 * Time: 12:17 AM
 */
public class CutTest {

    Path backupPath;

    public CutTest() throws URISyntaxException {
        backupPath = Paths.get(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void shouldCutSourceInsideTArget() {
        CutAssert cutAssert = having(0, 0, 100, 100,
                10, 10, 50, 50);
        cutAssert.assertSource(0, 0)
                .assertTarget(10, 10)
                .assertDims(50, 50);
        cutAssert.checkApply();
    }

    @Test
    public void shouldCutTargetInsideSource() {
        CutAssert cutAssert = having(10, 10, 50, 50,
                0, 0, 100, 100);
        cutAssert.assertSource(10, 10)
                .assertTarget(0, 0)
                .assertDims(50, 50);
        cutAssert.checkApply();
    }

    @Test
    public void shouldCutNonEmptyIntersectionSourceLeft() {
        CutAssert cutAssert = having(0, 10, 50, 50, 10, 0, 100, 100);
        cutAssert.assertSource(0, 10)
                .assertTarget(10, 0)
                .assertDims(40, 50);
        cutAssert.checkApply();
    }

    @Test
    public void shouldCutNonEmptyIntersectionSourceRight() {
        CutAssert cutAssert = having(40, 10, 50, 50, 0, 0, 100, 100);
        cutAssert.assertSource(40, 10)
                .assertTarget(0, 0)
                .assertDims(50, 50);
        cutAssert.checkApply();
    }

    @Test
    public void shouldCutNonEmptyIntersectionSourceDown() throws IOException {
        CutAssert cutAssert = having(0, 50, 180, 180, 0, 0, 100, 100);
        cutAssert.assertSource(0, 50)
                .assertTarget(0, 0)
                .assertDims(100, 50);

        cutAssert.checkApply();
    }

    @Test
    public void shouldCutEqual() throws IOException {
        CutAssert cutAssert = having(0, 0, 100, 100, 0, 0, 100, 100);
        cutAssert.assertSource(0, 0)
                .assertTarget(0, 0)
                .assertDims(100, 100);
        cutAssert.checkApply();
    }

    @Test
    public void shouldReturnEmptyIfNoXIntersection() {
        CutAssert cutAssert = having(
                0, 0, 100, 100,
                110, 10, 50, 50
        );
        cutAssert.assertNoCut();
    }

    @Test
    public void shouldReturnEmptyIfNoYIntersection() {
        CutAssert cutAssert = having(
                0, 0, 100, 100,
                10, 110, 50, 50);
        cutAssert.assertNoCut();
    }

    @Test
    public void shouldApply() throws IOException {
        CutAssert cutAssert = having(
                0, 0, 10, 20,
                0, 1, 10, 20);
        cutAssert.assertSource(0, 0);
        cutAssert.assertTarget(0, 1);
        cutAssert.assertDims(10, 19);
        cutAssert.checkApply();
    }


    private CutAssert having(int myAbsoluteX0, int myAbsoluteY0, int myW, int myH, int sourceAbsoluteX0, int sourceAbsoluteY0, int sourceW, int sourceH) {
        Optional<Cut> maybeCut = Cut.getCut(new Checkpoint("12_10_1"), 1,
                myAbsoluteX0, myAbsoluteY0, myW, myH,
                sourceAbsoluteX0, sourceAbsoluteY0, sourceW, sourceH);
        return new CutAssert(maybeCut,
                myAbsoluteX0, myAbsoluteY0, myW, myH,
                sourceAbsoluteX0, sourceAbsoluteY0, sourceW, sourceH);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public class CutAssert {

        final Optional<Cut> maybeCut;
        final int myAbsoluteX0, myAbsoluteY0, myW, myH, sourceAbsoluteX0, sourceAbsoluteY0, sourceW, sourceH;

        public CutAssert(Optional<Cut> maybeCut,
                         int myAbsoluteX0, int myAbsoluteY0, int myW, int myH,
                         int sourceAbsoluteX0, int sourceAbsoluteY0, int sourceW, int sourceH) {
            this.maybeCut = maybeCut;
            this.myAbsoluteX0 = myAbsoluteX0;
            this.myAbsoluteY0 = myAbsoluteY0;
            this.myW = myW;
            this.myH = myH;
            this.sourceAbsoluteX0 = sourceAbsoluteX0;
            this.sourceAbsoluteY0 = sourceAbsoluteY0;
            this.sourceW = sourceW;
            this.sourceH = sourceH;
        }

        CutAssert assertNoCut() {
            assertThat(maybeCut).isEmpty();
            return this;
        }

        CutAssert assertTarget(int expectedTargetX, int expectedTargetY) {
            Cut cut = getCut();
            assertThat(cut.targetX).isEqualTo(expectedTargetX);
            assertThat(cut.targetY).isEqualTo(expectedTargetY);
            return this;
        }

        CutAssert assertSource(int expectedSrcX, int expectedSrcY) {
            Cut cut = getCut();
            assertThat(cut.srcX).isEqualTo(expectedSrcX);
            assertThat(cut.srcY).isEqualTo(expectedSrcY);
            return this;
        }

        CutAssert assertDims(int expectedW, int expectedH) {
            Cut cut = getCut();
            assertThat(cut.w).isEqualTo(expectedW);
            assertThat(cut.h).isEqualTo(expectedH);
            return this;
        }

        private Cut getCut() {
            assertThat(maybeCut).isPresent();
            return maybeCut.get();
        }

        public void checkApply() {
            Cut cut = getCut();
            prepareSourceFile();

            int target[][] = new int[myH][myW];
            cut.apply(backupPath.toString(), target);

            assertBoard(target);
        }

        private void assertBoard(int[][] target) {
            printBoard(target);
            Cut cut = getCut();
            for (int i = 0; i < cut.targetY; i++) {
                for (int j = 0; j < myW; j++) {
                    assertThat(target[i][j]).isEqualTo(0);
                }
            }
            for (int i = cut.targetY + cut.h; i < myH; i++) {
                for (int j = 0; j < myW; j++) {
                    assertThat(target[i][j]).isEqualTo(0);
                }
            }
            for (int i = 0; i < myH; i++) {
                for (int j = 0; j < cut.targetX; j++) {
                    assertThat(target[i][j]).isEqualTo(0);
                }
            }
            for (int i = 0; i < myH; i++) {
                for (int j = cut.targetX + cut.w; j < myW; j++) {
                    assertThat(target[i][j]).isEqualTo(0);
                }
            }

            for (int i = cut.targetY; i < cut.h; i++) {
                for (int j = cut.targetX; j < cut.w; j++) {
                    assertThat(target[i][j]).isEqualTo((i + cut.srcY - cut.targetY) % 2);
                }

            }
        }

        private void printBoard(int[][] target) {
            System.out.println("===================================");
            for (int j = 0; j < target[0].length; j++) {
                for (int i = 0; i < target.length; i++) {
                    System.out.print(target[i][j] + " ");
                }
                System.out.println();
            }
            System.out.println("===================================");
        }

        private void prepareSourceFile() {
            try {
                int source[][] = new int[sourceH][sourceW];

                for (int i = 0; i < sourceH; i++) {
                    for (int j = 0; j < sourceW; j++) {
                        source[i][j] = i % 2; // 0's stripe, 1's stripe, and so on
                    }
                }

                System.out.println("source:");
                printBoard(source);

                Backup backup = new Backup(source, 10, sourceH, sourceW, 0, 0); // mstodo row/col???

                String backupFilePath = BackupUtils.backupFilePath(12, 10, 1);
                Path path = backupPath.resolve(backupFilePath);
                if (path.toFile().exists()) {
                    path.toFile().delete();
                }
                Path file = Files.createFile(path);
                try (OutputStream stream = Files.newOutputStream(file)) {
                    ObjectOutputStream objectStream = new ObjectOutputStream(stream);
                    objectStream.writeObject(backup);
                }
                System.out.println("Prepared backup file: " + backupFilePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}