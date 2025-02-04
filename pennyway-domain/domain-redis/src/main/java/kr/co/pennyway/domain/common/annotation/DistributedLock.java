package kr.co.pennyway.domain.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * Lock 이름
     */
    String key();

    /**
     * Lock 유지 시간 (초)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * Lock 유지 시간 (DEFAULT: 10초)
     * LOCK 획득을 위해 waitTime만큼 대기한다.
     */
    long waitTime() default 10L;

    /**
     * Lock 임대 시간 (DEFAULT: 5초)
     * LOCK 획득 이후 leaseTime이 지나면 LOCK을 해제한다.
     */
    long leaseTime() default 5L;

    /**
     * 동일한 트랜잭션에서 Lock을 획득할지 여부 (DEFAULT: true) <br/>
     * - true : Propagation.REQUIRES_NEW 전파 방식을 사용하여 새로운 트랜잭션에서 Lock을 획득한다. <br/>
     * - false : Propagation.MANDATORY 전파 방식을 사용하여 동일한 트랜잭션에서 Lock을 획득한다.
     */
    boolean needNewTransaction() default true;
}
