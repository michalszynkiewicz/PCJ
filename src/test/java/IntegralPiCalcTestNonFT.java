import org.pcj.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * Date: 4/29/15
 * Time: 6:08 PM
 */
public class IntegralPiCalcTestNonFT extends Storage implements StartPoint {

   public static final Work FINISH_WORK = new Work(true);

   private static final long n = 100_000_000_000L;
   private static final double weight = 1.0 / (double) n;

   // all nodes data:
   @Shared
   private double sum;
   @Shared
   private Work work;

   private void work() {
      while (true) {
//         PCJ.log("Will wait for " + new Date());
         if (!doIgnoringFailure(() -> PCJ.waitFor("work"))) {
            continue;
         }
//         PCJ.log("after wait for " + new Date());
         Work work = PCJ.getLocal("work");
         if (work.finished) {
//            PCJ.log("finished" + new Date());
            break;
         } else {
//            PCJ.log("Will work: " + work.task);
            work(work.task);
         }
      }
   }

   private double f(final double x) {
      return (4.0 / (1.0 + x * x));
   }

   private void work(Task task) {
//      PCJ.log("will work " + new Date());

      double sum = 0.0;

      for (double i = task.start; i < task.end; i++) {
         sum += f((i + 0.5) * weight);
      }
      PCJ.putLocal("sum", sum * weight);
//      PCJ.log("will barrier " + new Date());
      PCJ.barrier();
//      PCJ.log("after" + new Date());
   }


   // node 0-specific data:
   private double pi;
   private final Map<Integer, Task> tasksByThreadId = new HashMap<>();
   private final ArrayList<Task> workToAssign = new ArrayList<>();

   private void coordinate() {
      workToAssign.add(new Task(0, n));
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
         doIgnoringFailure(() -> {
            PCJ.put(threadId, "work", work);
         });
      }
   }

   private Work createWork(int nodesNum, int nodeOrdinal, Task t) {
      long size = (t.end - t.start) / nodesNum;
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
                 } else {
                    System.out.printf("### got pi from %d, current result: %f10\n", t, pi);
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
         System.out.printf("PI: %f10 time: %f5\n", pi, time2 * 1.0E-9);
      }
   }


   boolean doIgnoringFailure(Runnable r) {
      r.run();
      return true;
   }

   public static void main(String[] args) {
      PCJ.deploy(IntegralPiCalcTestNonFT.class, IntegralPiCalcTestNonFT.class, System.getProperty("nodes").split(","));
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
