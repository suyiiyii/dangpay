package top.suyiiyii.su.IOC;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.suyiiyii.dto.UserRoles;
import top.suyiiyii.models.Event;
import top.suyiiyii.service.*;
import top.suyiiyii.su.UniversalUtils;
import top.suyiiyii.su.exception.Http_200_OK;
import top.suyiiyii.su.exception.Http_403_ForbiddenException;
import top.suyiiyii.su.orm.core.ModelManger;
import top.suyiiyii.su.orm.core.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Slf4j
@Proxy(isNeedAuthorization = false)
public class ProxyInvocationHandler implements InvocationHandler {
    private final UserRoles userRoles;
    private final RBACService rbacService;
    private final Session db;
    private final ApproveService approveService;
    private final ApproveServiceImpl.ApplicantReason applicantReason;
    private final EventService eventService;
    private final HttpServletRequest req;
    private final GroupService groupService;
    private final LockService lockService;
    private int eventId;
    @Setter
    private Object target;
    /**
     * 在代理对象被创建时，设置整个代理对象是否需要权限校验
     */
    private boolean isNeedAuthorization;

    public ProxyInvocationHandler(
            UserRoles userRoles,
            @Proxy(isNeedAuthorization = false, isNotProxy = true) RBACService rbacService,
            Session db,
            @Proxy(isNeedAuthorization = false, isNotProxy = true) ApproveService approveService,
            ApproveServiceImpl.ApplicantReason applicantReason,
            @Proxy(isNeedAuthorization = false, isNotProxy = true) EventService eventService,
            HttpServletRequest req,
            @Proxy(isNeedAuthorization = false, isNotProxy = true) GroupService groupService,
            LockService lockService) {
        this.userRoles = userRoles;
        this.rbacService = rbacService;
        this.db = db;
        this.approveService = approveService;
        this.applicantReason = applicantReason;
        this.eventService = eventService;
        this.req = req;
        this.groupService = groupService;
        this.lockService = lockService;
    }

    public void setNeedAuthorization(boolean needAuthorization) {
        isNeedAuthorization = needAuthorization;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 跳过toString方法
        if (method.getName().equals("toString")) {
            return "Proxy for " + target.getClass().getName();
        }
        // 跳过destroy方法
        if (method.getName().equals("destroy")) {
            return null;
        }

        boolean isNeedAuthorization = false;
        boolean isTransaction = false;
        // 检查对象的接口上是否有Proxy注解
        if (method.getDeclaringClass().isAnnotationPresent(Proxy.class)) {
            Proxy annotation = method.getDeclaringClass().getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
        }
        // 检查对象上是否有Proxy注解
        if (target.getClass().isAnnotationPresent(Proxy.class)) {
            Proxy annotation = target.getClass().getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
        }
        // 检查接口的方法上是否有Proxy注解
        if (method.isAnnotationPresent(Proxy.class)) {
            Proxy annotation = method.getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
            isTransaction = annotation.isTransaction();
        }
        // 检查对象的方法上是否有Proxy注解
        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        if (targetMethod.isAnnotationPresent(Proxy.class)) {
            Proxy annotation = targetMethod.getAnnotation(Proxy.class);
            isNeedAuthorization = annotation.isNeedAuthorization();
            isTransaction = annotation.isTransaction();
        }
        // 如果需要权限校验，则进行权限校验
        if (isNeedAuthorization && this.isNeedAuthorization) {
            checkAuthorization(method, args);
        }


        // 判断是否需要进行审批
        if (approveService.checkApprove(userRoles.getUid(), applicantReason.getReason(), method, List.of(args))) {
            log.info("方法" + method + "需要审批");
            ApproveServiceImpl.NeedApproveResponse response = new ApproveServiceImpl.NeedApproveResponse();
            response.setNeedApprove(true);
            response.setMsg("已提交审批");
            throw new Http_200_OK("已提交审批");
        }

        // 枚举参数，判断有没有加锁注解
        boolean isNeedLock = false;
        String lockKey = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                if (method.getParameters()[i].isAnnotationPresent(SubRegion.class)) {
                    SubRegion annotation = method.getParameters()[i].getAnnotation(SubRegion.class);
                    if (annotation.lockKey().equals(""))
                        continue;
                    isNeedLock = true;
                    lockKey = annotation.lockKey() + "_" + args[i].toString();
                    break;
                }
            }
        }

        boolean errorFlag = false;
        try {
            // 如果需要加锁，则加锁
            if (isNeedLock) {
                lockService.tryLock(lockKey, 100, 100, java.util.concurrent.TimeUnit.SECONDS);
            }
            // 如果需要事务，则开启事务
            if (isTransaction && !db.isTransaction()) {
                try {
                    db.beginTransaction();
                    log.debug("使用session" + db + "开启事务");
                    Object result = method.invoke(target, args);
                    log.debug("使用session" + db + "提交事务");
                    db.commitTransaction();
                    log.debug("使用session" + db + "事务提交成功");
                    return result;
                } catch (Exception e) {
                    log.error("事务中发生异常，执行方法 " + method.getName() + " 参数 " + args, e);
                    db.rollbackTransaction();
                    throw e;
                }
            } else {
                return method.invoke(target, args);
            }
        } catch (InvocationTargetException e) {
            // invoke方法抛出的是一个包装过的异常，需要通过getTargetException获取原始异常
            Session db1 = IOCManager.getGlobalBean(ModelManger.class).getSession();
            assert db1 != null;
            db1.update(Event.class).set("status", "errInExec").eq("id", eventId).execute();
            db1.close();
            errorFlag = true;
            throw e.getTargetException();
        } finally {
            if (!errorFlag) {
                Session db1 = IOCManager.getGlobalBean(ModelManger.class).getSession();
                assert db1 != null;
                db1.update(Event.class).set("status", "success").eq("id", eventId).execute();
                db1.close();
                // 如果加了锁，则释放锁
                if (isNeedLock) {
                    lockService.unlock(lockKey);
                }
            }
        }
    }

    public void destroy() {
    }

    private void checkAuthorization(Method method, Object[] args) {
        String permission = method.getDeclaringClass().getSimpleName() + UniversalUtils.capitalizeFirstLetter(method.getName());

        int subRegionId = 0;
        // 检查有没有参数有子区域注解
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(SubRegion.class)) {
                // 如果参数有子区域注解，则获取子区域id，并拼接权限字符串
                SubRegion annotation = parameter.getAnnotation(SubRegion.class);
                String areaPrefix = annotation.areaPrefix();
                subRegionId = (int) args[i];
                permission += "/" + areaPrefix + subRegionId;
                break;
            }
        }
        boolean result = rbacService.checkUserPermission(userRoles, permission);
        String methodStr = method.getDeclaringClass().getName() + "/" + method.getName();
        String ip = req.getRemoteAddr();
        String UA = req.getHeader("User-Agent");

        // 记录事件
        Event event = new Event();
        event.setUid(userRoles.getUid());
        event.setMethod(methodStr);
        event.setIp(ip);
        if (req.getHeader("X-REAL-IP") != null) {
            event.setIp(req.getHeader("X-REAL-IP"));
        }
        if (req.getHeader("X-FORWARDED-FOR") != null) {
            event.setIp(req.getHeader("X-FORWARDED-FOR"));
        }
        if (req.getHeader("x-caddy-real-ip") != null) {
            event.setIp(req.getHeader("x-caddy-real-ip"));
        }
        event.setUa(UA);
        event.setPermission(permission);
        event.setCreateTime(UniversalUtils.getNow());
        event.setStatus(result ? "running" : "authFail");
        this.eventId = db.insert(event, true);

        if (!result) {
//            String message = "权限校验失败，请求用户: " + userRoles.uid + " 用户角色: " + userRoles.roles + " 请求权限: " + permission;
            String message = "您无权执行此操作";
            log.info("权限校验失败，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
//            throw new Http_403_ForbiddenException("权限不足");
            throw new Http_403_ForbiddenException(message);
        }
        log.info("权限校验通过，请求用户: {} 用户角色: {} 请求权限: {}", userRoles.uid, userRoles.roles, permission);
    }
}