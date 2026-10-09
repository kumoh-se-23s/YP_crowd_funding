package ypfunding.server.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * 동시성 테스트 도구: 같은 작업을 여러 스레드에서 "최대한 같은 순간에" 실행한다.
 *
 * <p>평가표 19 "제공 가능 수량 동시성 제어"와 요구사항 "4.1 남은 리워드 수량보다 많은 후원자가 동시에 해당 리워드를
 * 신청하는 상황을 발생시켜 테스트한다"를 검증할 때 쓴다.
 *
 * <p>[동작] 모든 스레드를 출발선(latch)에 세워 두었다가 한 번에 출발시킨다.
 * 그냥 스레드를 차례로 시작하면 앞 스레드가 이미 끝나 버려서 실제로는 겹치지 않을 수 있다.
 *
 * <p>[쓰는 법]
 * <pre>
 *   List&lt;Throwable&gt; results = Concurrent.runAtOnce(20, i -&gt;
 *           fundService.applyFund(supporters.get(i), rewardId, 1));   // i = 0 ~ 19, 스레드 번호
 *
 *   assertEquals(1, Concurrent.successCount(results));                // 성공 1명
 *   assertEquals(19, Concurrent.countOf(results, SoldOutException.class));   // 나머지는 품절
 * </pre>
 */
public final class Concurrent {

    /** 전체 작업이 이 시간 안에 끝나지 않으면 실패로 본다 (데드락 등으로 멈추는 경우 테스트가 영원히 안 끝나는 것 방지) */
    private static final long TIMEOUT_SECONDS = 30;

    private Concurrent() {
    }

    /**
     * 작업을 threadCount개 스레드에서 동시에 실행하고, 각 스레드의 결과를 돌려준다.
     *
     * @param threadCount 스레드 수
     * @param task        실행할 작업. 인자로 스레드 번호(0부터)를 받는다
     * @return 스레드 번호 순서의 결과 목록. 성공이면 null, 실패면 그 스레드에서 난 예외
     * @throws AssertionError 제한 시간 안에 끝나지 않은 경우
     */
    public static List<Throwable> runAtOnce(int threadCount, IntConsumer task) {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);   // 모든 스레드가 출발선에 섰는지
        CountDownLatch start = new CountDownLatch(1);             // 출발 신호
        CountDownLatch done = new CountDownLatch(threadCount);    // 모든 스레드가 끝났는지
        List<Throwable> results = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(threadCount, null)));

        try {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                pool.execute(() -> {
                    try {
                        ready.countDown();
                        start.await();
                        task.accept(index);
                    } catch (Throwable t) {
                        results.set(index, t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            await(ready, "스레드 준비");
            start.countDown();                                      // 동시에 출발
            await(done, "작업 완료");
            return new ArrayList<>(results);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * @param results runAtOnce의 결과
     * @return 성공(예외 없음)한 스레드 수
     */
    public static long successCount(List<Throwable> results) {
        return results.stream().filter(t -> t == null).count();
    }

    /**
     * @param results runAtOnce의 결과
     * @param type    셀 예외 타입 (하위 타입 포함)
     * @return 해당 예외로 실패한 스레드 수
     */
    public static long countOf(List<Throwable> results, Class<? extends Throwable> type) {
        return results.stream().filter(type::isInstance).count();
    }

    private static void await(CountDownLatch latch, String what) {
        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(what + "이(가) " + TIMEOUT_SECONDS + "초 안에 끝나지 않았습니다. (데드락 의심)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(what + " 대기 중 인터럽트", e);
        }
    }
}