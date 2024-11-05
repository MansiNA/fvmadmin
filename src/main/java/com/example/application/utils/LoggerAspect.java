package com.example.application.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggerAspect.class);

    @Value("${verbose:false}")
    private boolean verbose;

    @Around("execution(* com.example.application..*(..))") // Adjust the pointcut expression as needed
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if (verbose) {
            logger.info("Executing: " + joinPoint.getSignature());
        } else {
            logger.debug("Executing: " + joinPoint.getSignature());
        }

//        long start = System.currentTimeMillis();
        Object proceed = joinPoint.proceed();
//        long executionTime = System.currentTimeMillis() - start;

//        if (verbose) {
//            logger.info("Execution time of {}: {} ms", joinPoint.getSignature(), executionTime);
//        } else {
//            logger.debug("Execution time of {}: {} ms", joinPoint.getSignature(), executionTime);
//        }

        return proceed;
    }
}
