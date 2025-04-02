package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，用于实现自动填充公共字段的处理逻辑
 *
 * @author : LXRkk
 * @date : 2025/4/2 15:38
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点
     * excution 后的第一个 * 表示方法返回类型为所有类型
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    /**
     * 前置通知，在通知中进行公共字段赋值
     * @param joinPoint
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段填充……");

        // 获取当前方法的操作类型 insert or update
        MethodSignature signature = (MethodSignature)joinPoint.getSignature(); // 获取方法签名对象
        Method method = signature.getMethod(); // 获取方法对象
        AutoFill annotation = method.getAnnotation(AutoFill.class); // 获取注解
        OperationType operation = annotation.operation(); // 获取注解方法的操作类型
        // 获取当前被拦截方法的参数--实体对象
        Object[] args = joinPoint.getArgs(); // 获取被拦截方法的所有参数，这里规定第一个参数为实体对象
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0]; // 获取实体对象
        // 获取赋值数据
        LocalDateTime now = LocalDateTime.now(); // 创建 or 更新时的时间
        Long currentId = BaseContext.getCurrentId(); // 创建人id
        // 根据不同的操作类型，通过反射为对应的属性赋值
        Class<?> entityClass = entity.getClass();
        if (operation == OperationType.INSERT) {
            // 为 4 个公告字段赋值
            try {
                Method setCreateTime = entityClass.getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = entityClass.getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = entityClass.getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = entityClass.getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setCreateTime.invoke(entity,now);
                setUpdateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateUser.invoke(entity,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (operation == OperationType.UPDATE) {
            // 为 2 个公告字段赋值
            try {
                Method setUpdateTime = entityClass.getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entityClass.getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
