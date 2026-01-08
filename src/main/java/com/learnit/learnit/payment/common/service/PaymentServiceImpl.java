package com.learnit.learnit.payment.common.service;

import com.learnit.learnit.cart.CartMapper;
import com.learnit.learnit.course.CourseMapper;
import com.learnit.learnit.enroll.EnrollmentMapper;
import com.learnit.learnit.payment.common.PaymentException;
import com.learnit.learnit.payment.common.dto.*;
import com.learnit.learnit.payment.common.enums.PaymentStatus;
import com.learnit.learnit.payment.common.repository.CouponMapper;
import com.learnit.learnit.payment.common.repository.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService{

    private final PaymentMapper paymentMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CouponMapper couponMapper;
    private final CourseMapper courseMapper;
    private final CartMapper cartMapper;

    @Override
    public void processPayment(PaymentRequestDTO request) {

        try{
            log.info("결제 처리 시작: 주문번호 {}", request.getOrderNo());

            //1. 사전 검증 : 결제 데이터가 들어가기 전에 예외 상황 체크
            //1-1. 쿠폰 검증
            UserCouponDTO userCoupon = null;
            if(request.getCouponId() != null){
                userCoupon = couponMapper.findValidUserCoupon(request.getUserId(), request.getCouponId());

                if(userCoupon == null) throw new PaymentException("사용 가능한 쿠폰이 없습니다.");
                if(userCoupon.getMinPrice() != null && request.getTotalPrice() < userCoupon.getMinPrice()){
                    throw new PaymentException("쿠폰 적용 최소 금액을 만족하지 않습니다.");
                }
            }

            //1-2. 강의 및 수강 여부 미리 검증
            for(Long courseId : request.getCourseIds()){
                if(enrollmentMapper.existsEnrollment(request.getUserId(), courseId)){
                    throw new PaymentException("이미 수강 중인 강의가 포함되어 있습니다.");
                }
                if(courseMapper.findCoursePrice(courseId) == null){
                    throw new PaymentException("존재하지 않는 강의 정보가 있습니다.");
                }
            }


            //2. 데이터 저장
            //2-1. 주문 생성
            OrderDTO order = new OrderDTO();
            order.setOrderNo(request.getOrderNo());
            order.setUserId(request.getUserId());
            order.setTotalPrice(request.getTotalPrice());
            order.setStatus(PaymentStatus.READY.name());

            paymentMapper.insertOrder(order);

            Long orderId = order.getOrderId();

            //2-2. 결제 상세 정보 저장
            int finalPrice = request.getTotalPrice();

            PaymentDTO payment = new PaymentDTO();
            payment.setOrderId(orderId);
            payment.setFinalPrice(finalPrice);
            payment.setMethod(request.getMethod());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCouponId(request.getCouponId());

            paymentMapper.insertPayment(payment);

            Long paymentId = payment.getPaymentId();

            //2-3. 상세 내역 및 수강 권한
            for(Long courseId : request.getCourseIds()){
                PaymentDetailDTO detail = new PaymentDetailDTO();
                detail.setPaymentId(paymentId);
                detail.setCourseId(courseId);
                detail.setPrice(courseMapper.findCoursePrice(courseId));

                paymentMapper.insertPaymentDetail(detail);

                enrollmentMapper.insertEnrollment(request.getUserId(), courseId); //수강 권한 부여
            }

            //3. 상태 변경
            paymentMapper.updateOrderStatus(orderId, PaymentStatus.PAID.name()); // 주문 상태 변경
            if(userCoupon != null) couponMapper.useCoupon(userCoupon.getUserCouponId()); // 쿠폰 사용
            cartMapper.deleteByUserIdAndCourseIds(request.getUserId(), request.getCourseIds()); // 장바구니 비우기

        } catch (PaymentException e) {
            log.warn("결제 중단(비즈니스 로직): {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // 여기서 나는 에러는 DB 작업 중 발생하는 런타임 에러
            log.error("결제 중 심각한 시스템 에러 발생", e);
            throw new PaymentException("서버 내부 오류가 발생했습니다.");
        }

    }
}
