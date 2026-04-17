package aliang.flowcopilot.exception;

import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 表示预期内的业务失败，例如资源不存在、参数非法或状态不满足要求。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    /**
     * 构造业务异常。
     *
     * @param message 异常提示信息
     */
    public BizException(String message) {
        super(message);
        this.code = 400;
    }
}
