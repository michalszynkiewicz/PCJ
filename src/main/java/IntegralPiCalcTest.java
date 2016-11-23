import org.pcj.*;
import org.pcj.internal.faulttolerance.NodeFailedException;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/29/15
 * Time: 6:08 PM
 */
public class IntegralPiCalcTest extends Storage implements StartPoint {

    public static final Work FINISH_WORK = new Work(true);

    private static final int fails = Integer.valueOf(System.getProperty("fails", "0"));
    private static final long pointCount = Long.valueOf(System.getProperty("pointCount", "100_000_000L"));
    private static final double weight = 1.0 / (double) pointCount;

    // all nodes data:
    @Shared
    private double sum;
    @Shared
    private Work work;

    private void work() {
        while (true) {
            PCJ.log("Will wait for " + new Date());
            if (!doIgnoringFailure(() -> PCJ.waitFor("work"))) {
                continue;
            }
            PCJ.log("after wait for " + new Date());
            Work work = PCJ.getLocal("work");
            if (work.finished) {
                PCJ.log("finished" + new Date());
                break;
            } else {
                PCJ.log("Will work: " + work.task);
                work(work.task);
            }
        }
    }

    private double f(final double x) {
        return (4.0 / (1.0 + x * x));
    }

    private void work(Task task) {
        double sum = 0.0;

       for (double i = task.start; i < task.end; i++) {
                sum += f((i + 0.5) * weight);
                if (fails > 0
                        && PCJ.getPhysicalNodeId() == 2         /*mstodo smarter way before tests to minimize the impact!*/
                        && i - task.start < (task.end - task.start)/3) {
                    System.exit(123);
                }
                if (fails > 1
                        && PCJ.getPhysicalNodeId() == 17
                        && i - task.start < (task.end - task.start)/2) {
                    System.exit(124);
                }
        }
        PCJ.putLocal("sum", sum * weight);
        PCJ.barrier();
    }


    // node 0-specific data:
    private double pi;
    private final Map<Integer, Task> tasksByThreadId = new HashMap<>();
    private final ArrayList<Task> workToAssign = new ArrayList<>();

    private void coordinate() {
        workToAssign.add(new Task(0, pointCount));
        List<Integer> activeThreads = range(0, PCJ.threadCount()).boxed().collect(Collectors.toList());
        while (true) {
            splitWork(activeThreads);
            work(tasksByThreadId.get(0));
            if (gatherResults(activeThreads)) {
                System.out.println("#### results gathered successfully, should break and finish");
                break;
            }
        }
        setFinish(activeThreads);
    }

    private void setFinish(List<Integer> activeThreads) {
        activeThreads.forEach(t ->
                        doIgnoringFailure(() -> PCJ.put(t, "work", FINISH_WORK))
        );
    }

    private void splitWork(List<Integer> activeThreads) {
        Task task = workToAssign.remove(workToAssign.size() - 1);
        int threadCount = activeThreads.size();
        for (int i = 0; i < activeThreads.size(); i++) {
            int threadId = activeThreads.get(i);
            Work work = createWork(threadCount, i, task);
            tasksByThreadId.put(threadId, work.task);
            doIgnoringFailure(() -> PCJ.put(threadId, "work", work));
        }
    }

    private Work createWork(int nodesNum, int nodeOrdinal, Task t) {
        long size = (t.end - t.start)/nodesNum;
        long start = t.start + nodeOrdinal * size;
        long end = nodeOrdinal == nodesNum - 1 ? t.end : start + size;
        return new Work(new Task(start, end));
    }

    private boolean gatherResults(List<Integer> threads) {
        Map<Integer, FutureObject<Double>> cL = new HashMap<>();
        Set<Integer> unfinishedNodes = new HashSet<>();

        threads.forEach(t -> {
                    if (!doIgnoringFailure(() -> cL.put(t, PCJ.getFutureObject(t, "sum")))) {
                        unfinishedNodes.add(t);
                    }
                }
        );
        threads.removeAll(unfinishedNodes);

        threads.forEach(t -> {
                    if (!doIgnoringFailure(() -> pi += cL.get(t).get())) {
                        unfinishedNodes.add(t);
                    }
                }
        );
        threads.removeAll(unfinishedNodes);
        if (unfinishedNodes.isEmpty() && workToAssign.isEmpty()) {
            return true;
        } else {
            unfinishedNodes.forEach(t -> workToAssign.add(tasksByThreadId.get(t)));
            return false;
        }
    }

    @Override
    public void main() {
        PCJ.barrier();
        double time = System.nanoTime();

        if (PCJ.myId() == 0) {
            coordinate();
        } else {
            work();
        }

        double time2 = System.nanoTime();
        time2 -= time;

        if (PCJ.myId() == 0) {
            System.out.printf("######PI: %f10 time: %f5\n", pi, time2 * 1.0E-9);
        }
    }


    boolean doIgnoringFailure(Runnable r) {
        try {
            r.run();
            return true;
        } catch (NodeFailedException ignored) {
            return false;
        }
    }

    public static void main(String[] args) {
        PCJ.deploy(IntegralPiCalcTest.class, IntegralPiCalcTest.class, System.getProperty("nodes").split(","));
    }

    public static class Work implements Serializable {
        boolean finished;
        Task task;

        public Work(boolean finished) {
            this.finished = finished;
        }

        public Work(Task task) {
            this.task = task;
        }
    }

    public static class Task implements Serializable {
        long start;
        long end;

        public Task(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }


}
