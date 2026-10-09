package ypfunding.server.support;

import ypfunding.common.util.DateTimeUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 테스트에서 "현재 시각"을 마음대로 정하고 옮길 수 있는 시계.
 *
 * <p>Service는 생성자로 받은 Clock에서 현재 시각을 읽는다. (설계 9.5)
 * 테스트용 AppContext에 이 시계를 넣으면, 테스트가 현재 시각을 바꾸는 즉시 모든 Service에 반영된다.
 * <pre>
 *   clock.set(LocalDateTime.of(2026, 12, 7, 10, 0));   // 이 시각에 후원하고
 *   clock.plus(Duration.ofDays(31));                   // 31일 뒤(펀딩 종료 후)로 옮겨서
 *   reviewService.write(...);                          // 리뷰 작성을 검사한다
 * </pre>
 *
 * <p>Clock.fixed()는 한 번 만들면 시각을 바꿀 수 없어서, 시간이 흐르는 시나리오(마감 전 → 마감 후)를
 * 한 테스트 안에서 검사할 수 없다. 그래서 바꿀 수 있는 시계를 따로 만들었다.
 * 시간대는 항상 한국 시간(KST)이다.
 */
public final class TestClock extends Clock {

    /** 여러 스레드(동시성 테스트)가 읽으므로 volatile */
    private volatile Instant instant;

    /**
     * @param start 처음 현재 시각 (한국 시각)
     */
    public TestClock(LocalDateTime start) {
        set(start);
    }

    /**
     * 현재 시각을 바꾼다.
     *
     * @param now 새 현재 시각 (한국 시각)
     */
    public void set(LocalDateTime now) {
        this.instant = now.atZone(DateTimeUtil.KST).toInstant();
    }

    /**
     * 현재 시각을 앞으로(또는 음수면 뒤로) 옮긴다.
     *
     * @param amount 옮길 양
     */
    public void plus(Duration amount) {
        this.instant = instant.plus(amount);
    }

    /** @return 지금 이 시계가 가리키는 한국 시각 */
    public LocalDateTime now() {
        return LocalDateTime.now(this);
    }

    @Override
    public ZoneId getZone() {
        return DateTimeUtil.KST;
    }

    /** 다른 시간대가 필요할 때(거의 없음): 현재 시각에서 멈춘 시계를 돌려준다. */
    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}