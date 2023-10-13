package com.kakao.sunsuwedding._core.errors.exception;


import com.kakao.sunsuwedding._core.errors.BaseException;
import com.kakao.sunsuwedding._core.utils.ApiUtils;
import lombok.Getter;
import org.springframework.http.HttpStatus;


// 인증 안됨
@Getter
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(BaseException exception){
        super(exception.getMessage());
    }

    public Object body(){
        return ApiUtils.error(getMessage(), HttpStatus.UNAUTHORIZED);
    }

    public HttpStatus status(){
        return HttpStatus.UNAUTHORIZED;
    }
}