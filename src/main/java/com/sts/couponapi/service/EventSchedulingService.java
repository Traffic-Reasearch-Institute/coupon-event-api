package com.sts.couponapi.service;

import com.sts.couponapi.dto.CouponResponseDto;
import com.sts.couponapi.entity.FinishEvent;
import com.sts.couponapi.repository.FinishEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventSchedulingService {

    private final RedisTemplate<String, Double> registerCouponTemplate;
    private final FinishEventRepository finishEventRepository;

    @Scheduled(fixedDelay = 1000) //1초마다 실행
    public void moveToMySQL() {
        ZSetOperations<String, Double> zSetOps = registerCouponTemplate.opsForZSet();
        Set<Double> coupons = zSetOps.range("A1001:1", 0, -1);
        LocalDateTime now = LocalDateTime.now();

        // 년/월/일/시/분을 2자리 문자열로 표시
        String year = String.format("%02d", now.getYear() % 100);
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());
        String hour = String.format("%02d", now.getHour());
        String minute = String.format("%02d", now.getMinute());

        String result = year + month + day + hour + minute;

        for (Double coupon : coupons) {
            Double score = zSetOps.score("A1001:1", coupon);
            long tmpLong = coupon.longValue();
            String strScore = String.valueOf(tmpLong);
            String subStr = strScore.substring(strScore.length() - 2);
            double subStr2 = Double.parseDouble(subStr);
            long tmpScore = tmpLong;
            if (subStr2 + 15 >= 60)
                tmpScore += 55;
            else
                tmpScore += 15;
            if (Double.parseDouble(result) >= tmpScore || score <= 0) {
                FinishEvent event = new FinishEvent();
                event.setCouponType("A1001");
                event.setDate(coupon);
                event.setCount(score);
                finishEventRepository.save(event);
                zSetOps.remove("A1001:1", coupon);
            }
        }
    }
}
